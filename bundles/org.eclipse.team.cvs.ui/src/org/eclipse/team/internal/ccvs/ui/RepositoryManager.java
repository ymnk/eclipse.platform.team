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
import java.lang.reflect.InvocationTargetException;
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
import org.eclipse.jface.operation.IRunnableWithProgress;
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
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.ICVSRunnable;
import org.eclipse.team.internal.ccvs.core.ILogEntry;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.ui.model.RepositoryRoot;
import org.omg.CORBA.portable.InvokeHandler;
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

	Map repositoryRoots = new HashMap();
	
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
	public CVSTag[] getKnownTags(ICVSFolder project, int tagType) {
		try {
			CVSTag[] tags = getKnownTags(project);
			Set result = new HashSet();
			for (int i = 0; i < tags.length; i++) {
				CVSTag tag = tags[i];
				if (tag.getType() == tagType)
					result.add(tag);
			}

			return (CVSTag[])result.toArray(new CVSTag[result.size()]);
		} catch(CVSException e) {
			CVSUIPlugin.log(e.getStatus());
			return new CVSTag[0];
		}
	}
	
	/**
	 * Get the list of known version tags for a given project.
	 */
	public CVSTag[] getKnownTags(ICVSRepositoryLocation location, int tagType) {
		Set result = new HashSet();
		RepositoryRoot root = (RepositoryRoot)repositoryRoots.get(location.getLocation());
		if (root != null) {
			String[] paths = root.getKnownRemotePaths();
			for (int i = 0; i < paths.length; i++) {
				String path = paths[i];
				CVSTag[] tags = root.getKnownTags(path);
				for (int j = 0; i < tags.length; i++) {
					CVSTag tag = tags[j];
					if (tag.getType() == tagType)
						result.add(tag);
				}
			}
		}
		return (CVSTag[])result.toArray(new CVSTag[0]);
	}
	
	public CVSTag[] getKnownTags(ICVSFolder project) throws CVSException {
		RepositoryRoot root = getRepositoryRootFor(project);
		String remotePath = getRemotePathFor(project);
		return root.getKnownTags(remotePath);
	}
	
	/*
	 * XXX I hope this methos is not needed in this form	 */
	public Map getKnownProjectsAndVersions(ICVSRepositoryLocation location) {
		Map knownTags = new HashMap();
		try {
			RepositoryRoot root = getRepositoryRootFor(location);
			String[] paths = root.getKnownRemotePaths();
			for (int i = 0; i < paths.length; i++) {
				String path = paths[i];
				Set result = new HashSet();
				result.addAll(Arrays.asList(root.getKnownTags(path)));
				knownTags.put(path, result);
			}
		} catch (CVSException e) {
			CVSUIPlugin.log(e);
		}
		return knownTags;
	}
	
	public ICVSRemoteResource[] getFoldersForTag(ICVSRepositoryLocation location, CVSTag tag, IProgressMonitor monitor) throws CVSException {
		if (tag.getType() == tag.HEAD) {
			IPreferenceStore store = CVSUIPlugin.getPlugin().getPreferenceStore();
			return location.members(tag, store.getBoolean(ICVSUIConstants.PREF_SHOW_MODULES), monitor);
		}
		Set result = new HashSet();
		// Get the tags for the location
		RepositoryRoot root = getRepositoryRootFor(location);
		String[] paths = root.getKnownRemotePaths();
		for (int i = 0; i < paths.length; i++) {
			String path = paths[i];
			List tags = Arrays.asList(root.getKnownTags(path));
			if (tags.contains(tag)) {
				ICVSRemoteFolder remote = location.getRemoteFolder(path, tag);
				result.add(remote);
			}
		}
		return (ICVSRemoteResource[])result.toArray(new ICVSRemoteResource[result.size()]);
	}
		
	/*
	 * Fetches tags from .project and .vcm_meta if they exist. Then fetches tags from the user defined auto-refresh file
	 * list. The fetched tags are cached in the CVS ui plugin's tag cache.
	 */
	public void refreshDefinedTags(ICVSFolder project, boolean notify, IProgressMonitor monitor) throws TeamException {
		RepositoryRoot root = getRepositoryRootFor(project);
		String remotePath = getRemotePathFor(project);
		root.refreshDefinedTags(remotePath, monitor);
		broadcastChange(root);
	}
	
	public void addBranchTags(ICVSRepositoryLocation location, CVSTag[] tags) {
		// XXX Is this still needed
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
		RepositoryRoot repoRoot = (RepositoryRoot)repositoryRoots.remove(root.getLocation());
		if (root != null)
			broadcastChange(repoRoot);
	}
	
	/**
	 * Accept tags for any CVS resource. However, for the time being,
	 * the given version tags are added to the list of known tags for the 
	 * remote ancestor of the resource that is a direct child of the remote root
	 */
	public void addTags(ICVSResource resource, CVSTag[] tags) throws CVSException {
		RepositoryRoot root = getRepositoryRootFor(resource);
		// XXX could be a file or folder
		String remotePath = getRemotePathFor(resource);
		root.addTags(remotePath, tags);
	}
	
	public void addAutoRefreshFiles(ICVSFolder project, String[] relativeFilePaths) throws CVSException {
		RepositoryRoot root = getRepositoryRootFor(project);
		String remotePath = getRemotePathFor(project);
		Set set = new HashSet();
		set.addAll(Arrays.asList(root.getAutoRefreshFiles(remotePath)));
		set.addAll(Arrays.asList(relativeFilePaths));
		root.setAutoRefreshFiles(remotePath, (String[]) set.toArray(new String[set.size()]));
	}
	
	public void removeAutoRefreshFiles(ICVSFolder project, String[] relativeFilePaths) throws CVSException {
		RepositoryRoot root = getRepositoryRootFor(project);
		String remotePath = getRemotePathFor(project);
		Set set = new HashSet();
		set.addAll(Arrays.asList(root.getAutoRefreshFiles(remotePath)));
		set.removeAll(Arrays.asList(relativeFilePaths));
		root.setAutoRefreshFiles(remotePath, (String[]) set.toArray(new String[set.size()]));
	}
	
	public String[] getAutoRefreshFiles(ICVSFolder project) throws CVSException {
		RepositoryRoot root = getRepositoryRootFor(project);
		String remotePath = getRemotePathFor(project);
		return root.getAutoRefreshFiles(remotePath);
	}
	
	/**
	 * Remove the given branch tag from the list of known tags for the
	 * given remote root.
	 */
	public void removeBranchTag(ICVSRepositoryLocation location, CVSTag[] tags) {
		// XXX is this still needed?
	}
	
	/**
	 * Remove the given tags from the list of known tags for the
	 * given remote root.
	 */
	public void removeTags(ICVSFolder project, CVSTag[] tags) throws CVSException {
		RepositoryRoot root = getRepositoryRootFor(project);
		String remotePath = getRemotePathFor(project);
		root.removeTags(remotePath, tags);
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
		File tempFile = pluginStateLocation.append(REPOSITORIES_VIEW_FILE + ".tmp").toFile(); //$NON-NLS-1$
		File stateFile = pluginStateLocation.append(REPOSITORIES_VIEW_FILE).toFile();
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
	private void writeState(XMLWriter writer) throws IOException, CVSException {
		writer.startTag(RepositoriesViewContentHandler.REPOSITORIES_VIEW_TAG, null, true);
		// Write the repositories
		Collection repos = Arrays.asList(getKnownRoots());
		Iterator it = repos.iterator();
		HashMap attributes = new HashMap();
		while (it.hasNext()) {
			CVSRepositoryLocation location = (CVSRepositoryLocation)it.next();
			RepositoryRoot root = getRepositoryRootFor(location);
			root.writeState(writer);
		}
		writer.endTag(RepositoriesViewContentHandler.REPOSITORIES_VIEW_TAG);
	}
	
	private void readState(InputStream stream) throws IOException, TeamException {
		SAXParser parser = new SAXParser();
		parser.setContentHandler(new RepositoriesViewContentHandler(this));
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
			RepositoryRoot repoRoot = getRepositoryRootFor(root);
			
			// read branch tags associated with this root
			int tagsSize = dis.readInt();
			CVSTag[] branchTags = new CVSTag[tagsSize];
			for (int j = 0; j < tagsSize; j++) {
				String tagName = dis.readUTF();
				int tagType = dis.readInt();
				branchTags[j] = new CVSTag(tagName, tagType);
			}
			// Ignore the branch tags since they are handled differently now
			// addBranchTags(root, branchTags);
			
			// read the number of projects for this root that have version tags
			int projSize = dis.readInt();
			if (projSize > 0) {
				for (int j = 0; j < projSize; j++) {
					String name = dis.readUTF();
					Set tagSet = new HashSet();
					int numTags = dis.readInt();
					for (int k = 0; k < numTags; k++) {
						tagSet.add(new CVSTag(dis.readUTF(), CVSTag.VERSION));
					}
					repoRoot.addTags(name, (CVSTag[]) tagSet.toArray(new CVSTag[tagSet.size()]));
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
						for (int j = 0; j < projSize; j++) {
							String name = dis.readUTF();
							Set filenames = new HashSet();
							int numFilenames = dis.readInt();
							for (int k = 0; k < numFilenames; k++) {
								filenames.add(dis.readUTF());
							}
							repoRoot.setAutoRefreshFiles(name, (String[]) filenames.toArray(new String[filenames.size()]));
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
			return internalGetRepositoryLocationFor(resource);
		} catch (CVSException e) {
			CVSUIPlugin.log(e.getStatus());
			return null;
		}
	}

	private ICVSRepositoryLocation internalGetRepositoryLocationFor(ICVSResource resource) throws CVSException {
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
		// XXX This is asking for trouble
		return null;
	}
		
	private RepositoryRoot getRepositoryRootFor(ICVSResource resource) throws CVSException {
		ICVSRepositoryLocation location = internalGetRepositoryLocationFor(resource);
		if (location == null) return null;
		return getRepositoryRootFor(location);
	}
	
	private RepositoryRoot getRepositoryRootFor(ICVSRepositoryLocation location) throws CVSException {
		RepositoryRoot root = (RepositoryRoot)repositoryRoots.get(location.getLocation());
		if (root == null) {
			root = new RepositoryRoot(location);
			add(root);
		}
		return root;
	}
	
	private String getRemotePathFor(ICVSResource resource) throws CVSException {
		if (resource.isFolder()) {
			FolderSyncInfo info = ((ICVSFolder)resource).getFolderSyncInfo();
			if (info == null)
				throw new CVSException(Policy.bind("RepositoryManager.folderInfoMissing", resource.getName()));
			return info.getRepository();
		} else {
			FolderSyncInfo info = resource.getParent().getFolderSyncInfo();
			if (info == null)
				throw new CVSException(Policy.bind("RepositoryManager.folderInfoMissing", resource.getParent().getName()));
			String path = new Path(info.getRepository()).append(resource.getName()).toString();
			return path;
		}
	}
	
	/**
	 * Add the given repository root to the receiver. The provided instance of RepositoryRoot
	 * is used to provide extra information about the repository location
	 * 
	 * @param currentRepositoryRoot
	 */
	public void add(RepositoryRoot currentRepositoryRoot) {
		repositoryRoots.put(currentRepositoryRoot.getRoot().getLocation(), currentRepositoryRoot);
	}
	
	/*
	 * Broadcast the change to the given repository configuration to all listeners
	 * @param root
	 */
	private void broadcastChange(RepositoryRoot root) {
		// XXX todo
	}
	
	/**
	 * Run the given runnable, waiting until the end to perform a refresh
	 * 	 * @param runnable	 * @param monitor	 */
	public void run(IRunnableWithProgress runnable, IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		runnable.run(monitor);
	}
}
