/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 * IBM - Initial implementation
 ******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFile;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.ILogEntry;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.RepositoriesViewContentHandler;
import org.eclipse.team.internal.ccvs.ui.XMLWriter;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class RepositoryRoot extends CVSModelElement implements IAdaptable {

	public static final String[] DEFAULT_AUTO_REFRESH_FILES = { ".project", ".vcm_meta" };
	
	ICVSRepositoryLocation root;
	String name;
	// Map of String (remote folder path) -> Set (CVS tags)
	Map knownTags = new HashMap();
	// Map of String (remote folder path) -> Set (file paths that are project relative)
	Map autoRefreshFiles = new HashMap();
	
	public RepositoryRoot(ICVSRepositoryLocation root) {
		this.root = root;
	}
	
	/**
	 * Returns an object which is an instance of the given class
	 * associated with this object. Returns <code>null</code> if
	 * no such object can be found.
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == IWorkbenchAdapter.class) return this;
		return null;
	}
	
	public ImageDescriptor getImageDescriptor(Object object) {
		return CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_REPOSITORY);
	}
	
	public String getLabel(Object o) {
		if (name == null)
			return root.getLocation();
		else
			return name;
	}
	
	public Object getParent(Object o) {
		return null;
	}
	
	public Object[] getChildren(Object o) {
		return new Object[] {
			new CVSTagElement(CVSTag.DEFAULT, root),
			new BranchCategory(root),
			new VersionCategory(root)
		};
	}
	
	/**
	 * Returns the name.
	 * @return String
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the root.
	 * @return ICVSRepositoryLocation
	 */
	public ICVSRepositoryLocation getRoot() {
		return root;
	}

	/**
	 * Sets the name.
	 * @param name The name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Accept the tags for any remote path that represents a folder. However, for the time being,
	 * the given version tags are added to the list of known tags for the 
	 * remote ancestor of the resource that is a direct child of the remote root.
	 * 
	 * It is the reponsibility of the caller to ensure that the given remote path is valid.
	 */
	public void addTags(String remotePath, CVSTag[] tags) {	
		// Get the name to cache the version tags with
		String name = getCachePathFor(remotePath);
		
		// Make sure there is a table for the ancestor that holds the tags
		Set set = (Set)knownTags.get(name);
		if (set == null) {
			set = new HashSet();
			knownTags.put(name, set);
		}
		
		// Store the tag with the appropriate ancestor
		for (int i = 0; i < tags.length; i++) {
			set.add(tags[i]);
		}
	}
	
	/**
	 * Remove the given tags from the receiver	 * @param remotePath	 * @param tags	 */
	public void removeTags(String remotePath, CVSTag[] tags) {	
		// Get the name to cache the version tags with
		String name = getCachePathFor(remotePath);
		
		// Make sure there is a table for the ancestor that holds the tags
		Set set = (Set)knownTags.get(name);
		if (set == null) {
			return;
		}
		
		// Store the tag with the appropriate ancestor
		for (int i = 0; i < tags.length; i++) {
			set.remove(tags[i]);
		}
	}
	
	/**
	 * Returns the autoRefreshFiles.
	 * @return String[]
	 */
	public String[] getAutoRefreshFiles(String remotePath) {
		String name = getCachePathFor(remotePath);
		Set files = (Set)autoRefreshFiles.get(name);
		if (files == null) {
			return DEFAULT_AUTO_REFRESH_FILES;
		} else {
			return (String[]) files.toArray(new String[files.size()]);
		}
	}

	/**
	 * Sets the autoRefreshFiles.
	 * @param autoRefreshFiles The autoRefreshFiles to set
	 */
	public void setAutoRefreshFiles(String remotePath, String[] autoRefreshFiles) {
		List newFiles = Arrays.asList(autoRefreshFiles);
		// Check to see if the auto-refresh files are the default files
		if (autoRefreshFiles.length == DEFAULT_AUTO_REFRESH_FILES.length) {
			boolean isDefault = true;
			for (int i = 0; i < DEFAULT_AUTO_REFRESH_FILES.length; i++) {
				String filePath = DEFAULT_AUTO_REFRESH_FILES[i];
				if (!newFiles.contains(filePath)) {
					isDefault = false;
					break;
				}
			}
			if (isDefault) {
				this.autoRefreshFiles.remove(getCachePathFor(remotePath));
				return;
			}
		}
		Set files = new HashSet(newFiles);
		files.addAll(newFiles);
		this.autoRefreshFiles.put(getCachePathFor(remotePath), files);
	}

	/**
	 * Fetches tags from auto-refresh files.
	 */
	public void refreshDefinedTags(String remotePath, IProgressMonitor monitor) throws TeamException {
		String[] filesToRefresh = getAutoRefreshFiles(remotePath);
		monitor.beginTask(null, filesToRefresh.length * 10); //$NON-NLS-1$
		try {
			List tags = new ArrayList();
			for (int i = 0; i < filesToRefresh.length; i++) {
				String relativePath = new Path(remotePath).append(filesToRefresh[i]).toString();
				ICVSRemoteFile file = root.getRemoteFile(relativePath, CVSTag.DEFAULT);
				tags.addAll(Arrays.asList(fetchTags(file, Policy.subMonitorFor(monitor, 5))));
			}
			addTags(remotePath, (CVSTag[]) tags.toArray(new CVSTag[tags.size()]));
		} finally {
			monitor.done();
		}
	}
	
	/**
	 * Returns Branch and Version tags for the given files
	 */	
	private CVSTag[] fetchTags(ICVSRemoteFile file, IProgressMonitor monitor) throws TeamException {
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
	
	private String getCachePathFor(String remotePath) {
		return new Path(remotePath).segment(0);
	}
	
	/**
	 * Write out the state of the receiver as XML on the given XMLWriter.
	 * 	 * @param writer	 * @throws IOException	 */
	public void writeState(XMLWriter writer) throws IOException {

		HashMap attributes = new HashMap();

		attributes.clear();
		attributes.put(RepositoriesViewContentHandler.ID_ATTRIBUTE, root.getLocation());
		String programName = ((CVSRepositoryLocation)root).getRemoteCVSProgramName();
		if (!programName.equals(CVSRepositoryLocation.DEFAULT_REMOTE_CVS_PROGRAM_NAME)) {
			attributes.put(RepositoriesViewContentHandler.REPOSITORY_PROGRAM_NAME_ATTRIBUTE, programName);
		}
		if (name != null) {
			attributes.put(RepositoriesViewContentHandler.NAME_ATTRIBUTE, name);
		}
		writer.startTag(RepositoriesViewContentHandler.REPOSITORY_TAG, attributes, true);
		
		// Gather all the modules that have tags and/or auto-refresh files
		

		// for each module, write the moduel, tags and auto-refresh files.
		String[] paths = getKnownRemotePaths();
		for (int i = 0; i < paths.length; i++) {
			String path = paths[i];
			attributes.clear();
			attributes.put(RepositoriesViewContentHandler.PATH_ATTRIBUTE, path);
			writer.startTag(RepositoriesViewContentHandler.MODULE_TAG, attributes, true);
			Set tagSet = (Set)knownTags.get(path);
			Iterator tagIt = tagSet.iterator();
			while (tagIt.hasNext()) {
				CVSTag tag = (CVSTag)tagIt.next();
				attributes.clear();
				attributes.put(RepositoriesViewContentHandler.NAME_ATTRIBUTE, tag.getName());
				attributes.put(RepositoriesViewContentHandler.TYPE_ATTRIBUTE, RepositoriesViewContentHandler.TAG_TYPES[tag.getType()]);
				writer.startAndEndTag(RepositoriesViewContentHandler.TAG_TAG, attributes, true);
			}
			Set refreshSet = (Set)autoRefreshFiles.get(path);
			Iterator filenameIt = refreshSet.iterator();
			while (filenameIt.hasNext()) {
				String filename = (String)filenameIt.next();
				attributes.clear();
				attributes.put(RepositoriesViewContentHandler.PATH_ATTRIBUTE, filename);
				writer.startAndEndTag(RepositoriesViewContentHandler.AUTO_REFRESH_FILE_TAG, attributes, true);
			}
			writer.endTag(RepositoriesViewContentHandler.MODULE_TAG);
		}
		writer.endTag(RepositoriesViewContentHandler.REPOSITORY_TAG);

	}
	/**
	 * Method getKnownTags.
	 * @param remotePath
	 * @return CVSTag[]
	 */
	public CVSTag[] getKnownTags(String remotePath) {
		Set tagSet = (Set)knownTags.get(getCachePathFor(remotePath));
		if (tagSet == null) return new CVSTag[0];
		return (CVSTag[]) tagSet.toArray(new CVSTag[tagSet.size()]);
	}
	
	public String[] getKnownRemotePaths() {
		Set paths = new HashSet();
		paths.addAll(knownTags.keySet());
		paths.addAll(autoRefreshFiles.keySet());
		return (String[]) paths.toArray(new String[paths.size()]);
	}
}
