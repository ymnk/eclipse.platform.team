package org.eclipse.team.internal.ccvs.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.xerces.parsers.SAXParser;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.sync.IRemoteSyncElement;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.CVSTeamProvider;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSListener;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFile;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.ILogEntry;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class is repsible for maintaining the UI's list of known repositories,
 * and a list of known tags within each of those repositories.
 * 
 * It also provides a number of useful methods for assisting in repository operations.
 */
public class RepositoryManager {
	// old state file
	private static final String STATE_FILE = ".repositoryManagerState"; //$NON-NLS-1$
	private static final int STATE_FILE_VERSION_1 = -1;
	// new state file
	private static final String REPOSITORIES_VIEW_FILE = "repositoriesView.xml"; //$NON-NLS-1$

	// Map ICVSRepositoryLocation -> List of CVSTag
	Map branchTags = new HashMap();
	// Map ICVSRepositoryLocation -> Map of (Project name -> Set of CVSTag)
	Map versionTags = new HashMap();
	// Map ICVSRepositoryLocation -> Map of (Project name -> Set of file paths that are project relative)
	Map autoRefreshFiles = new HashMap();
	
	List listeners = new ArrayList();

	// The previously remembered comment
	private static String previousComment = ""; //$NON-NLS-1$
	
	public static boolean notifyRepoView = true;
	
	/**
	 * Answer an array of all known remote roots.
	 */
	public ICVSRepositoryLocation[] getKnownRoots() {
		return CVSProviderPlugin.getPlugin().getKnownRepositories();
	}
	
	/**
	 * Get the list of known branch tags for a given remote root.
	 */
	public CVSTag[] getKnownBranchTags(ICVSFolder project) {
		ICVSRepositoryLocation location = getRepositoryLocationFor(project);
		return getKnownBranchTags(location);
	}

	public CVSTag[] getKnownBranchTags(ICVSRepositoryLocation location) {
		Set set = (Set)branchTags.get(location);
		if (set == null) return new CVSTag[0];
		return (CVSTag[])set.toArray(new CVSTag[0]);
	}

	/**
	 * Get the list of known version tags for a given project.
	 */
	public CVSTag[] getKnownVersionTags(ICVSFolder project) {
		try {
			ICVSRepositoryLocation location = getRepositoryLocationFor(project);
			String name = new Path(project.getFolderSyncInfo().getRepository()).segment(0);
			Set result = new HashSet();
			Map table = (Map)versionTags.get(location);
			if (table == null) {
				return (CVSTag[])result.toArray(new CVSTag[result.size()]);
			}
			Set set = (Set)table.get(name);
			if (set == null) {
				return (CVSTag[])result.toArray(new CVSTag[result.size()]);
			}
			result.addAll(set);
			return (CVSTag[])result.toArray(new CVSTag[0]);
		} catch(CVSException e) {
			CVSUIPlugin.log(e.getStatus());
			return new CVSTag[0];
		}
	}
	
	/**
	 * Get the list of known version tags for a given project.
	 */
	public CVSTag[] getKnownVersionTags(ICVSRepositoryLocation location) {
		Set result = new HashSet();
		// Get the table of tags for the location
		Map table = (Map)versionTags.get(location);
		if (table != null) {
			// The table is keyed by folder
			for (Iterator iter = table.values().iterator(); iter.hasNext();) {
				Set tags = (Set)iter.next();
				result.addAll(tags);
			}
		}
		return (CVSTag[])result.toArray(new CVSTag[0]);
	}
	
	public Map getKnownProjectsAndVersions(ICVSRepositoryLocation location) {
		return (Map)versionTags.get(location);
	}
	
	public ICVSRemoteResource[] getFoldersForTag(ICVSRepositoryLocation location, CVSTag tag, IProgressMonitor monitor) throws CVSException {
		if (tag.getType() == tag.BRANCH || tag.getType() == tag.HEAD) {
			IPreferenceStore store = CVSUIPlugin.getPlugin().getPreferenceStore();
			return location.members(tag, store.getBoolean(ICVSUIConstants.PREF_SHOW_MODULES), monitor);
		}
		Set result = new HashSet();
		// Get the table of tags for the location
		Map table = (Map)versionTags.get(location);
		if (table != null) {
			// The table is keyed by folder
			for (Iterator iter = table.keySet().iterator(); iter.hasNext();) {
				String folderPath = (String) iter.next();
				Set tags = (Set)table.get(folderPath);
				if (tags.contains(tag)) {
					ICVSRemoteFolder remote = location.getRemoteFolder(folderPath, tag);
					result.add(remote);
				}
			}
			
		}
		return (ICVSRemoteResource[])result.toArray(new ICVSRemoteResource[result.size()]);
	}
		
	/*
	 * Fetches tags from .project and .vcm_meta if they exist. Then fetches tags from the user defined auto-refresh file
	 * list. The fetched tags are cached in the CVS ui plugin's tag cache.
	 */
	public void refreshDefinedTags(ICVSFolder project, boolean notify, IProgressMonitor monitor) throws TeamException {
		List filesToRefresh = new ArrayList(Arrays.asList(getAutoRefreshFiles(project)));
		monitor.beginTask(Policy.bind("RepositoryManager.refreshDefinedTags"), filesToRefresh.size() * 10); //$NON-NLS-1$
		try {
			ICVSRepositoryLocation location = CVSProviderPlugin.getPlugin().getRepository(project.getFolderSyncInfo().getRoot());
			List tags = new ArrayList();
			for (Iterator it = filesToRefresh.iterator(); it.hasNext();) {
				String relativePath = (String)it.next();
				ICVSFile file = null;
				if (project instanceof ICVSRemoteFolder) {
					// There should be a better way of doing this.
					ICVSRemoteFolder projectFolder = (ICVSRemoteFolder)project;
					ICVSRemoteFolder parentFolder = location.getRemoteFolder(new Path(projectFolder.getRepositoryRelativePath()).append(relativePath).removeLastSegments(1).toString(), CVSTag.DEFAULT);
					ICVSResource[] resources = parentFolder.fetchChildren(Policy.subMonitorFor(monitor, 5));
					for (int i = 0; i < resources.length; i++) {
						if (resources[i] instanceof ICVSRemoteFile && resources[i].getName().equals(new Path(relativePath).lastSegment())) {
							file = (ICVSFile)resources[i];
						}
					}
				} else {
					file = project.getFile(relativePath);
				}
				if (file != null) {
					tags.addAll(Arrays.asList(fetchDefinedTagsFor(file, project, location,
						Policy.subMonitorFor(monitor, 5))));
				} else {
					monitor.worked(5);
				}
			}
			// add all tags in one pass so that the listeners only get one notification for
			// versions and another for branches
			List branches = new ArrayList();
			List versions = new ArrayList();
			for (Iterator it = tags.iterator(); it.hasNext();) {
				CVSTag element = (CVSTag) it.next();
				if (element.getType() == CVSTag.BRANCH) {
					branches.add(element);
				} else {
					versions.add(element);
				}
			}
			
			// Hack for optimizing refreshing of repo view
			notifyRepoView = false;
			addBranchTags(project, (CVSTag[]) branches.toArray(new CVSTag[branches.size()]));
			if (notify) {
				notifyRepoView = true;
			}
			addVersionTags(project, (CVSTag[]) versions.toArray(new CVSTag[versions.size()]));
			notifyRepoView = true;
		} catch (CVSException e) {
			throw new TeamException(e.getStatus());
		} finally {
			monitor.done();
		}
	}
	
	/**
	 * Accept branch tags for any CVS resource. However, for the time being,
	 * the given branch tags are added to the list of known tags for the resource's
	 * remote root.
	 */
	public void addBranchTags(ICVSResource resource, CVSTag[] tags) {
		ICVSRepositoryLocation location = getRepositoryLocationFor(resource);
		addBranchTags(location, tags);
	}
	
	public void addBranchTags(ICVSRepositoryLocation location, CVSTag[] tags) {
		Set set = (Set)branchTags.get(location);
		if (set == null) {
			set = new HashSet();
			branchTags.put(location, set);
		}
		for (int i = 0; i < tags.length; i++) {
			set.add(tags[i]);
		}
		Iterator it = listeners.iterator();
		while (it.hasNext() && notifyRepoView) {
			IRepositoryListener listener = (IRepositoryListener)it.next();
			listener.branchTagsAdded(tags, location);
		}
	}
	
	/**
	 * A repository root has been added. Notify any listeners.
	 */
	public void rootAdded(ICVSRepositoryLocation root) {
		Iterator it = listeners.iterator();
		while (it.hasNext()) {
			IRepositoryListener listener = (IRepositoryListener)it.next();
			listener.repositoryAdded(root);
		}
	}
	
	/**
	 * A repository root has been removed.
	 * Remove the tags defined for this root and notify any listeners
	 */
	public void rootRemoved(ICVSRepositoryLocation root) {
		CVSTag[] branchTags = getKnownBranchTags(root);
		Map vTags = (Map)this.versionTags.get(root);
		this.branchTags.remove(root);
		this.versionTags.remove(root);
		this.autoRefreshFiles.remove(root);
		Iterator it = listeners.iterator();
		while (it.hasNext()) {
			IRepositoryListener listener = (IRepositoryListener)it.next();
			listener.branchTagsRemoved(branchTags, root);
			if (vTags != null) {
				Iterator keyIt = vTags.keySet().iterator();
				while (keyIt.hasNext()) {
					String projectName = (String)keyIt.next();
					Set tagSet = (Set)vTags.get(projectName);
					CVSTag[] versionTags = (CVSTag[])tagSet.toArray(new CVSTag[0]);
					listener.versionTagsRemoved(versionTags, root);
				}
			}
			listener.repositoryRemoved(root);
		}
	}
	
	/**
	 * Accept version tags for any CVS resource. However, for the time being,
	 * the given version tags are added to the list of known tags for the 
	 * remote ancestor of the resource that is a direct child of the remote root
	 */
	public void addVersionTags(ICVSResource resource, CVSTag[] tags) {
		try {		
			// Make sure there is a version tag table for the location
			ICVSRepositoryLocation location = getRepositoryLocationFor(resource);
			Map table = (Map)versionTags.get(location);
			if (table == null) {
				table = new HashMap();
				versionTags.put(location, table);
			}
			
			// Get the name to cache the version tags with
			ICVSFolder parent;
			if (resource.isFolder()) {
				parent = (ICVSFolder)resource;
			} else {
				parent = resource.getParent();
			}
			if (!parent.isCVSFolder()) return;
			String name = new Path(parent.getFolderSyncInfo().getRepository()).segment(0);
			
			// Make sure there is a table for the ancestor that holds the tags
			Set set = (Set)table.get(name);
			if (set == null) {
				set = new HashSet();
				table.put(name, set);
			}
			
			// Store the tag with the appropriate ancestor
			for (int i = 0; i < tags.length; i++) {
				set.add(tags[i]);
			}
			
			// Notify any listeners
			Iterator it = listeners.iterator();
			while (it.hasNext() && notifyRepoView) {
				IRepositoryListener listener = (IRepositoryListener)it.next();
				listener.versionTagsAdded(tags, location);
			}
		} catch (CVSException e) {
			CVSUIPlugin.log(e.getStatus());
		}
	}
	
	public void addAutoRefreshFiles(ICVSFolder project, String[] relativeFilePaths) {
		ICVSRepositoryLocation location = getRepositoryLocationFor(project);
		initDefaultAutoRefreshFiles(project);
		Map table = (Map)autoRefreshFiles.get(location);
		Set set = (Set)table.get(project.getName());
		for (int i = 0; i < relativeFilePaths.length; i++) {
			set.add(relativeFilePaths[i]);
		}
	}
	
	public void removeAutoRefreshFiles(ICVSFolder project, String[] relativeFilePaths) {
		ICVSRepositoryLocation location = getRepositoryLocationFor(project);
		initDefaultAutoRefreshFiles(project);
		Map table = (Map)autoRefreshFiles.get(location);
		if (table == null) return;
		Set set = (Set)table.get(project.getName());
		if (set == null) return;
		for (int i = 0; i < relativeFilePaths.length; i++) {
			set.remove(relativeFilePaths[i]);
		}
	}
	
	public String[] getAutoRefreshFiles(ICVSFolder project) {
		ICVSRepositoryLocation location = getRepositoryLocationFor(project);
		initDefaultAutoRefreshFiles(project);
		Map table = (Map)autoRefreshFiles.get(location);
		Set set = (Set)table.get(project.getName());
		return (String[])set.toArray(new String[0]);
	}
	
	private void initDefaultAutoRefreshFiles(ICVSFolder project) {
		ICVSRepositoryLocation location = getRepositoryLocationFor(project);
		Map table = (Map)autoRefreshFiles.get(location);
		if (table == null) {
			table = new HashMap();
			autoRefreshFiles.put(location, table);
		}
		Set set = (Set)table.get(project.getName());
		if (set == null) {
			String projectName = project.getName();
			set = new HashSet();
			table.put(projectName, set);
			set.add(".project"); //$NON-NLS-1$
			set.add(".vcm_meta"); //$NON-NLS-1$
		}
	}
	
	/**
	 * Remove the given branch tag from the list of known tags for the
	 * given remote root.
	 */
	public void removeBranchTag(ICVSFolder project, CVSTag[] tags) {
		ICVSRepositoryLocation location = getRepositoryLocationFor(project);
		removeBranchTag(location, tags);
	}
	
	public void removeBranchTag(ICVSRepositoryLocation location, CVSTag[] tags) {
		Set set = (Set)branchTags.get(location);
		if (set == null) return;
		for (int i = 0; i < tags.length; i++) {
			set.remove(tags[i]);
		}
		Iterator it = listeners.iterator();
		while (it.hasNext() && notifyRepoView) {
			IRepositoryListener listener = (IRepositoryListener)it.next();
			listener.branchTagsRemoved(tags, location);
		}
	}
	
	/**
	 * Remove the given tags from the list of known tags for the
	 * given remote root.
	 */
	public void removeVersionTags(ICVSFolder project, CVSTag[] tags) {
		ICVSRepositoryLocation location = getRepositoryLocationFor(project);
		Map table = (Map)versionTags.get(location);
		if (table == null) return;
		Set set = (Set)table.get(project.getName());
		if (set == null) return;
		for (int i = 0; i < tags.length; i++) {
			set.remove(tags[i]);
		}
		Iterator it = listeners.iterator();
		while (it.hasNext() && notifyRepoView) {
			IRepositoryListener listener = (IRepositoryListener)it.next();
			listener.versionTagsRemoved(tags, location);
		}
	}
	
	public void startup() throws TeamException {
		loadState();
		CVSProviderPlugin.getPlugin().addRepositoryListener(new ICVSListener() {
			public void repositoryAdded(ICVSRepositoryLocation root) {
				rootAdded(root);
			}
			public void repositoryRemoved(ICVSRepositoryLocation root) {
				rootRemoved(root);
			}
		});
	}
	
	public void shutdown() throws TeamException {
		saveState();
	}
	
	private void loadState() throws TeamException {
		IPath pluginStateLocation = CVSUIPlugin.getPlugin().getStateLocation().append(REPOSITORIES_VIEW_FILE);
		File file = pluginStateLocation.toFile();
		if (file.exists()) {
			try {
				BufferedInputStream is = new BufferedInputStream(new FileInputStream(file));
				try {
					readState(is);
				} finally {
					is.close();
				}
			} catch (IOException e) {
				CVSUIPlugin.log(new Status(Status.ERROR, CVSUIPlugin.ID, TeamException.UNABLE, Policy.bind("RepositoryManager.ioException"), e)); //$NON-NLS-1$
			} catch (TeamException e) {
				CVSUIPlugin.log(e.getStatus());
			}
		} else {
			IPath oldPluginStateLocation = CVSUIPlugin.getPlugin().getStateLocation().append(STATE_FILE);
			if (file.exists()) {
				try {
					DataInputStream dis = new DataInputStream(new FileInputStream(file));
					try {
						readOldState(dis);
					} finally {
						dis.close();
					}
					// saveState();
					// file.delete();
				} catch (IOException e) {
					CVSUIPlugin.log(new Status(Status.ERROR, CVSUIPlugin.ID, TeamException.UNABLE, Policy.bind("RepositoryManager.ioException"), e)); //$NON-NLS-1$
				} catch (TeamException e) {
					CVSUIPlugin.log(e.getStatus());
				}
			} 
		}
	}
	
	private void saveState() throws TeamException {
		IPath pluginStateLocation = CVSUIPlugin.getPlugin().getStateLocation();
		File tempFile = pluginStateLocation.append(STATE_FILE + ".tmp").toFile(); //$NON-NLS-1$
		File stateFile = pluginStateLocation.append(STATE_FILE).toFile();
		try {
			DataOutputStream dos = new DataOutputStream(new FileOutputStream(tempFile));
			try {
				writeState(dos);
			} finally {
				dos.close();
			}
			if (stateFile.exists()) {
				stateFile.delete();
			}
			boolean renamed = tempFile.renameTo(stateFile);
			if (!renamed) {
				throw new TeamException(new Status(Status.ERROR, CVSUIPlugin.ID, TeamException.UNABLE, Policy.bind("RepositoryManager.rename", tempFile.getAbsolutePath()), null)); //$NON-NLS-1$
			}
		} catch (IOException e) {
			throw new TeamException(new Status(Status.ERROR, CVSUIPlugin.ID, TeamException.UNABLE, Policy.bind("RepositoryManager.save",stateFile.getAbsolutePath()), e)); //$NON-NLS-1$
		}
		tempFile = pluginStateLocation.append(REPOSITORIES_VIEW_FILE + ".tmp").toFile(); //$NON-NLS-1$
		stateFile = pluginStateLocation.append(REPOSITORIES_VIEW_FILE).toFile();
		try {
			XMLWriter writer = new XMLWriter(new BufferedOutputStream(new FileOutputStream(tempFile)));
			try {
				writeState(writer);
			} finally {
				writer.close();
			}
			if (stateFile.exists()) {
				stateFile.delete();
			}
			boolean renamed = tempFile.renameTo(stateFile);
			if (!renamed) {
				throw new TeamException(new Status(Status.ERROR, CVSUIPlugin.ID, TeamException.UNABLE, Policy.bind("RepositoryManager.rename", tempFile.getAbsolutePath()), null)); //$NON-NLS-1$
			}
		} catch (IOException e) {
			throw new TeamException(new Status(Status.ERROR, CVSUIPlugin.ID, TeamException.UNABLE, Policy.bind("RepositoryManager.save",stateFile.getAbsolutePath()), e)); //$NON-NLS-1$
		}
	}
	private void writeState(XMLWriter writer) throws IOException {
		writer.startTag(RepositoriesViewContentHandler.REPOSITORIES_VIEW_TAG, null, true);
		// Write the repositories
		Collection repos = Arrays.asList(getKnownRoots());
		Iterator it = repos.iterator();
		HashMap attributes = new HashMap();
		while (it.hasNext()) {
			
			// write the repository start tag with attributes
			CVSRepositoryLocation root = (CVSRepositoryLocation)it.next();
			attributes.clear();
			attributes.put(RepositoriesViewContentHandler.ID_ATTRIBUTE, root.getLocation());
			String programName = root.getRemoteCVSProgramName();
			if (!programName.equals(CVSRepositoryLocation.DEFAULT_REMOTE_CVS_PROGRAM_NAME)) {
				attributes.put(RepositoriesViewContentHandler.REPOSITORY_PROGRAM_NAME_ATTRIBUTE, programName);
			}
			writer.startTag(RepositoriesViewContentHandler.REPOSITORY_TAG, attributes, true);
			
			// Gather all the modules that have tags and/or auto-refresh files
			Set paths = new HashSet();
			Map versionTable = (HashMap)versionTags.get(root);
			if (versionTable != null) paths.addAll(versionTable.keySet());
			Map autoRefreshTable = (HashMap)autoRefreshFiles.get(root);
			if (autoRefreshTable != null) paths.addAll(autoRefreshTable.keySet());

			// for each module, write the moduel, tags and auto-refresh files.
			Iterator projIt = paths.iterator();
			while (projIt.hasNext()) {
				String path = (String)projIt.next();
				attributes.clear();
				attributes.put(RepositoriesViewContentHandler.PATH_ATTRIBUTE, path);
				writer.startTag(RepositoriesViewContentHandler.MODULE_TAG, attributes, true);
				Set tagSet = (Set)versionTable.get(path);
				Iterator tagIt = tagSet.iterator();
				while (tagIt.hasNext()) {
					CVSTag tag = (CVSTag)tagIt.next();
					attributes.clear();
					attributes.put(RepositoriesViewContentHandler.NAME_ATTRIBUTE, tag.getName());
					attributes.put(RepositoriesViewContentHandler.TYPE_ATTRIBUTE, RepositoriesViewContentHandler.TAG_TYPES[tag.getType()]);
					writer.printTag(RepositoriesViewContentHandler.TAG_TAG, attributes, true, true);
				}
				Set refreshSet = (Set)autoRefreshTable.get(path);
				Iterator filenameIt = tagSet.iterator();
				while (filenameIt.hasNext()) {
					String filename = (String)filenameIt.next();
					attributes.clear();
					attributes.put(RepositoriesViewContentHandler.NAME_ATTRIBUTE, filename);
					writer.printTag(RepositoriesViewContentHandler.AUTO_REFRESH_FILE_TAG, attributes, true, true);
				}
				writer.endTag(RepositoriesViewContentHandler.MODULE_TAG);
			}
			// write the auto-refresh files for each remote folder
			
			writer.endTag(RepositoriesViewContentHandler.REPOSITORY_TAG);
		}
		writer.endTag(RepositoriesViewContentHandler.REPOSITORIES_VIEW_TAG);
	}
		
	private void writeState(DataOutputStream dos) throws IOException {
		// Write the repositories
		Collection repos = Arrays.asList(getKnownRoots());
		// Write out version number for file.
		// We write it as an int so we can read either the repoSize or the version in the readState
		// XXX We should come up with a more long term solution.
		dos.writeInt(STATE_FILE_VERSION_1);
		dos.writeInt(repos.size());
		Iterator it = repos.iterator();
		while (it.hasNext()) {
			ICVSRepositoryLocation root = (ICVSRepositoryLocation)it.next();
			dos.writeUTF(root.getLocation());
			CVSTag[] branchTags = getKnownBranchTags(root);
			dos.writeInt(branchTags.length);
			for (int i = 0; i < branchTags.length; i++) {
				dos.writeUTF(branchTags[i].getName());
				dos.writeInt(branchTags[i].getType());
			}
			// write number of projects for which there are tags in this root
			Map table = (Map)versionTags.get(root);
			if (table == null) {
				dos.writeInt(0);
			} else {
				dos.writeInt(table.size());
				// for each project, write the name of the project, number of tags, and each tag.
				Iterator projIt = table.keySet().iterator();
				while (projIt.hasNext()) {
					String name = (String)projIt.next();
					dos.writeUTF(name);
					Set tagSet = (Set)table.get(name);
					dos.writeInt(tagSet.size());
					Iterator tagIt = tagSet.iterator();
					while (tagIt.hasNext()) {
						CVSTag tag = (CVSTag)tagIt.next();
						dos.writeUTF(tag.getName());
					}
				}
			}
			// write number of projects for which there were customized auto refresh files
			table = (Map)autoRefreshFiles.get(root);
			if (table == null) {
				dos.writeInt(0);
			} else {
				dos.writeInt(table.size());
				// for each project, write the name of the project, number of filename then each file name
				Iterator projIt = table.keySet().iterator();
				while (projIt.hasNext()) {
					String name = (String)projIt.next();
					dos.writeUTF(name);
					Set tagSet = (Set)table.get(name);
					dos.writeInt(tagSet.size());
					Iterator filenameIt = tagSet.iterator();
					while (filenameIt.hasNext()) {
						String filename = (String)filenameIt.next();
						dos.writeUTF(filename);
					}
				}
			}
		}
	}
	
	private void readState(InputStream stream) throws IOException, TeamException {
		SAXParser parser = new SAXParser();
		parser.setContentHandler(new RepositoriesViewContentHandler());
		try {
			parser.parse(new InputSource(stream));
		} catch (SAXException ex) {
			throw new CVSException(Policy.bind("RepositoryManager.parsingProblem"), ex); //$NON-NLS-1$
		}
	}
	
	private void readOldState(DataInputStream dis) throws IOException, TeamException {
		int repoSize = dis.readInt();
		boolean version1 = false;
		if (repoSize == STATE_FILE_VERSION_1) {
			version1 = true;
			repoSize = dis.readInt();
		}
		for (int i = 0; i < repoSize; i++) {
			ICVSRepositoryLocation root = CVSProviderPlugin.getPlugin().getRepository(dis.readUTF());
			
			// read branch tags associated with this root
			int tagsSize = dis.readInt();
			CVSTag[] branchTags = new CVSTag[tagsSize];
			for (int j = 0; j < tagsSize; j++) {
				String tagName = dis.readUTF();
				int tagType = dis.readInt();
				branchTags[j] = new CVSTag(tagName, tagType);
			}
			addBranchTags(root, branchTags);
			
			// read the number of projects for this root that have version tags
			int projSize = dis.readInt();
			if (projSize > 0) {
				Map projTable = new HashMap();
				versionTags.put(root, projTable);
				for (int j = 0; j < projSize; j++) {
					String name = dis.readUTF();
					Set tagSet = new HashSet();
					projTable.put(name, tagSet);
					int numTags = dis.readInt();
					for (int k = 0; k < numTags; k++) {
						tagSet.add(new CVSTag(dis.readUTF(), CVSTag.VERSION));
					}
					Iterator it = listeners.iterator();
					while (it.hasNext()) {
						IRepositoryListener listener = (IRepositoryListener)it.next();
						listener.versionTagsAdded((CVSTag[])tagSet.toArray(new CVSTag[0]), root);
					}
				}
			}
			// read the auto refresh filenames for this project
			if (version1) {
				try {
					projSize = dis.readInt();
					if (projSize > 0) {
						Map autoRefreshTable = new HashMap();
						autoRefreshFiles.put(root, autoRefreshTable);
						for (int j = 0; j < projSize; j++) {
							String name = dis.readUTF();
							Set filenames = new HashSet();
							autoRefreshTable.put(name, filenames);
							int numFilenames = dis.readInt();
							for (int k = 0; k < numFilenames; k++) {
								filenames.add(dis.readUTF());
							}
						}
					}
				} catch (EOFException e) {
					// auto refresh files are not persisted, continue and save them next time.
				}
			}
		}
	}
	
	public void addRepositoryListener(IRepositoryListener listener) {
		listeners.add(listener);
	}
	
	public void removeRepositoryListener(IRepositoryListener listener) {
		listeners.remove(listener);
	}
	
	/**
	 * Add the given resources to their associated providers.
	 * This schedules the resources for addition; they still need to be committed.
	 */
	public void add(IResource[] resources, IProgressMonitor monitor) throws TeamException {
		Map table = getProviderMapping(resources);
		Set keySet = table.keySet();
		monitor.beginTask("", keySet.size() * 1000); //$NON-NLS-1$
		monitor.setTaskName(Policy.bind("RepositoryManager.adding")); //$NON-NLS-1$
		Iterator iterator = keySet.iterator();
		while (iterator.hasNext()) {
			IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1000);
			CVSTeamProvider provider = (CVSTeamProvider)iterator.next();
			provider.setComment(previousComment);
			List list = (List)table.get(provider);
			IResource[] providerResources = (IResource[])list.toArray(new IResource[list.size()]);
			provider.add(providerResources, IResource.DEPTH_ZERO, subMonitor);
		}		
	}
	
	/**
	 * Delete the given resources from their associated providers.
	 * This schedules the resources for deletion; they still need to be committed.
	 */
	public void delete(IResource[] resources, IProgressMonitor monitor) throws TeamException {
		Map table = getProviderMapping(resources);
		Set keySet = table.keySet();
		monitor.beginTask("", keySet.size() * 1000); //$NON-NLS-1$
		monitor.setTaskName(Policy.bind("RepositoryManager.deleting")); //$NON-NLS-1$
		Iterator iterator = keySet.iterator();
		while (iterator.hasNext()) {
			IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1000);
			CVSTeamProvider provider = (CVSTeamProvider)iterator.next();
			provider.setComment(previousComment);
			List list = (List)table.get(provider);
			IResource[] providerResources = (IResource[])list.toArray(new IResource[list.size()]);
			provider.delete(providerResources, subMonitor);
		}		
	}
	
	public void update(IResource[] resources, Command.LocalOption[] options, boolean createBackups, IProgressMonitor monitor) throws TeamException {
		Map table = getProviderMapping(resources);
		Set keySet = table.keySet();
		monitor.beginTask("", keySet.size() * 1000); //$NON-NLS-1$
		monitor.setTaskName(Policy.bind("RepositoryManager.updating")); //$NON-NLS-1$
		Iterator iterator = keySet.iterator();
		while (iterator.hasNext()) {
			IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1000);
			CVSTeamProvider provider = (CVSTeamProvider)iterator.next();
			List list = (List)table.get(provider);
			IResource[] providerResources = (IResource[])list.toArray(new IResource[list.size()]);
			provider.update(providerResources, options, null, createBackups, subMonitor);
		}		
	}
	
	/**
	 * Mark the files as merged.
	 */
	public void merged(IRemoteSyncElement[] elements) throws TeamException {
		Map table = getProviderMapping(elements);
		Set keySet = table.keySet();
		Iterator iterator = keySet.iterator();
		while (iterator.hasNext()) {
			CVSTeamProvider provider = (CVSTeamProvider)iterator.next();
			provider.setComment(previousComment);
			List list = (List)table.get(provider);
			IRemoteSyncElement[] providerElements = (IRemoteSyncElement[])list.toArray(new IRemoteSyncElement[list.size()]);
			provider.merged(providerElements);
		}		
	}
	
	/**
	 * Return the ReleaseCommentDialog or null if canceled. 
	 * The comment and unadded resources to add  can be retrieved from the dialog.
	 * Persist the entered release comment for the next caller.
	 */
	public ReleaseCommentDialog promptForComment(final Shell shell, final IResource[] unadded) {
		final int[] result = new int[1];
		final ReleaseCommentDialog dialog = new ReleaseCommentDialog(shell, unadded); 
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				dialog.setComment(previousComment);
				result[0] = dialog.open();
				if (result[0] != ReleaseCommentDialog.OK) return;
				previousComment = dialog.getComment();
			}
		});
		if (result[0] != ReleaseCommentDialog.OK) return null;
		return dialog;
	}
	
	/**
	 * Commit the given resources to their associated providers.
	 * 
	 * @param resources  the resources to commit
	 * @param monitor  the progress monitor
	 */
	public void commit(IResource[] resources, String comment, IProgressMonitor monitor) throws TeamException {
		Map table = getProviderMapping(resources);
		Set keySet = table.keySet();
		monitor.beginTask("", keySet.size() * 1000); //$NON-NLS-1$
		monitor.setTaskName(Policy.bind("RepositoryManager.committing")); //$NON-NLS-1$
		Iterator iterator = keySet.iterator();
		while (iterator.hasNext()) {
			IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1000);
			CVSTeamProvider provider = (CVSTeamProvider)iterator.next();
			provider.setComment(comment);
			List list = (List)table.get(provider);
			IResource[] providerResources = (IResource[])list.toArray(new IResource[list.size()]);
			provider.checkin(providerResources, IResource.DEPTH_INFINITE, subMonitor);
		}
	}
	
	/**
	 * Helper method. Return a Map mapping provider to a list of resources
	 * shared with that provider.
	 */
	private Map getProviderMapping(IResource[] resources) {
		Map result = new HashMap();
		for (int i = 0; i < resources.length; i++) {
			RepositoryProvider provider = RepositoryProvider.getProvider(resources[i].getProject(), CVSProviderPlugin.getTypeId());
			List list = (List)result.get(provider);
			if (list == null) {
				list = new ArrayList();
				result.put(provider, list);
			}
			list.add(resources[i]);
		}
		return result;
	}
	/**
	 * Helper method. Return a Map mapping provider to a list of IRemoteSyncElements
	 * shared with that provider.
	 */
	private Map getProviderMapping(IRemoteSyncElement[] elements) {
		Map result = new HashMap();
		for (int i = 0; i < elements.length; i++) {
			RepositoryProvider provider = RepositoryProvider.getProvider(elements[i].getLocal().getProject(), CVSProviderPlugin.getTypeId());
			List list = (List)result.get(provider);
			if (list == null) {
				list = new ArrayList();
				result.put(provider, list);
			}
			list.add(elements[i]);
		}
		return result;
	}

	/**
	 * Returns Branch and Version tags for the given files
	 */	
	public CVSTag[] getTags(ICVSFile file, IProgressMonitor monitor) throws TeamException {
		Set tagSet = new HashSet();
		ILogEntry[] entries = file.getLogEntries(monitor);
		for (int j = 0; j < entries.length; j++) {
			CVSTag[] tags = entries[j].getTags();
			for (int k = 0; k < tags.length; k++) {
				tagSet.add(tags[k]);
			}
		}
		return (CVSTag[])tagSet.toArray(new CVSTag[0]);
	}
	
	public ICVSRepositoryLocation getRepositoryLocationFor(ICVSResource resource) {
		try {
			ICVSFolder folder;
			if (resource.isFolder()) {
				folder = (ICVSFolder)resource;
			} else {
				folder = resource.getParent();
			}
			if (folder.isCVSFolder()) {
				ICVSRepositoryLocation location = CVSProviderPlugin.getPlugin().getRepository(folder.getFolderSyncInfo().getRoot());
				return location;
			}
			return null;
		} catch (CVSException e) {
			CVSUIPlugin.log(e.getStatus());
			return null;
		}
	}
	
	/*
	 * Fetches and caches the tags found on the provided remote file.
	 */
	private CVSTag[] fetchDefinedTagsFor(ICVSFile file, ICVSFolder project,
		ICVSRepositoryLocation location, IProgressMonitor monitor) throws TeamException {
		if (file != null && file.exists()) {
			return getTags(file, monitor);
		}
		return new CVSTag[0];
	}
}
