/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.repo;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.operations.RemoteLogOperation;
import org.eclipse.team.internal.ccvs.ui.operations.RemoteLogOperation.LogEntryCache;
import org.eclipse.team.internal.core.caches.CacheableReference;
import org.eclipse.team.internal.core.caches.ICacheableReference;

public class RepositoryRoot extends PlatformObject {

	public static final String[] DEFAULT_AUTO_REFRESH_FILES = { ".project" }; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String DEFINED_MODULE_PREFIX = "module:"; //$NON-NLS-1$
	
	ICVSRepositoryLocation root;
	String name;
	// Map of String (remote folder path) -> TagCacheEntry
	Map versionAndBranchTags = new HashMap();
	// Map of String (remote folder path) -> Set (file paths that are project relative)
	Map autoRefreshFiles = new HashMap();
	// Map of String (module name) -> ICVSRemoteFolder (that is a defined module)
	Map modulesCache;
	Object modulesCacheLock = new Object();
	// List of date tags
	List dateTags = new ArrayList();
	
	// A unique name used to identify the directory where tags and the such can be cached for this root
	private String cacheDirectory;
	
	public static class TagCacheEntry {
	    private final ICacheableReference cache;
	    long lastAccessTime;
        private static final int CACHE_LIFESPAN_IN_DAYS = 7;
        private final RepositoryRoot repo;
        String fileName;
        public TagCacheEntry(RepositoryRoot repo, String remotePath) {
            this.repo = repo;
            this.cache = new CacheableReference(createTagCacheSource(remotePath));
            accessed();
        }
        public boolean isExpired() {
            long currentTime = System.currentTimeMillis();
            long ms = currentTime - lastAccessTime;
            int seconds = (int)ms / 1000;
            int hours = seconds / 60 / 60;
            int days = hours / 24;
            return days > CACHE_LIFESPAN_IN_DAYS;
        }
        public void accessed() {
            lastAccessTime = System.currentTimeMillis();
        }
        private LocalTagCache createTagCacheSource(String remotePath) {
            return new LocalTagCache(repo, remotePath, getLocalFilePath());
        }
        private String getLocalFilePath() {
            if (fileName == null) {
                fileName = generateFilename();
            }
            return repo.getCachePath().append(fileName).toOSString();
        }
        private String generateFilename() {
            return Long.toString(System.currentTimeMillis());
        }
        public ICacheableReference getReference() {
            accessed();
            return cache;
        }
        public void setTags(CVSTag[] tags) {
            // Modify the replica first so the new tags will be available when requested
            LocalTagCache replica = (LocalTagCache)cache.getSource();
            try {
                replica.setTags(tags);
            } catch (CoreException e) {
                // The replica couldn't be saved.
                // Log the error and continue since the tags will still be available in memory for a while
                CVSUIPlugin.log(e);
            }
            // Set the cache directly to avoid reloading from disk
            ((CacheableReference)cache).setCachedObject(tags);
        }
        public void removeTags(CVSTag[] tags) {
            // Store the tag with the appropriate ancestor
            accessed();
    		CVSTag[] existingTags = getTags();
            Set tagSet = new HashSet();
    		if (existingTags == null) {
    		    // No tags to remove
    		    return;
    		}
    		tagSet.addAll(Arrays.asList(existingTags));
    		for (int i = 0; i < tags.length; i++) {
    			tagSet.remove(tags[i]);
    		}
    		setTags((CVSTag[]) tagSet.toArray(new CVSTag[tagSet.size()]));
        }
        public void addTags(CVSTag[] tags) {
            // Store the tag with the appropriate ancestor
    		CVSTag[] existingTags = getTags();
            Set tagSet = new HashSet();
    		if (existingTags != null)
    		    tagSet.addAll(Arrays.asList(existingTags));
    		for (int i = 0; i < tags.length; i++) {
    			if(tags[i].getType() != CVSTag.DATE){
    			    tagSet.add(tags[i]);
    			}
    		}
    		setTags((CVSTag[]) tagSet.toArray(new CVSTag[tagSet.size()]));
        }
        private CVSTag[] getTags() {
            try {
                return (CVSTag[])getReference().getObject(ICacheableReference.ONLY_LOAD_REPLICA_IF_ABSENT, null);
            } catch (CoreException e) {
                CVSUIPlugin.log(e);
            } catch (ClassCastException e) {
                CVSUIPlugin.log(IStatus.WARNING, "Invalid object in tags cache. It will be ignored", e); //$NON-NLS-1$
            }
            return new CVSTag[0];
        }
        public void dispose() {
            LocalTagCache replica = (LocalTagCache)cache.getSource();
            replica.dispose();
        }
        public void write(XMLWriter writer) {
            HashMap attributes = new HashMap();
            attributes.put(RepositoriesViewContentHandler.PATH_ATTRIBUTE, fileName);
			attributes.put(RepositoriesViewContentHandler.LAST_ACCESS_TIME_ATTRIBUTE, Long.toString(lastAccessTime));
			writer.startAndEndTag(RepositoriesViewContentHandler.TAG_REPLICA_TAG, attributes, true);
        }
	}
	
	public RepositoryRoot(ICVSRepositoryLocation root) {
		this.root = root;
	}
	
	/**
	 * Returns the name.
	 * @return String
	 */
	public String getName() {
		return name;
	}

	/**
	 * Method getRemoteFolder.
	 * @param path
	 * @param tag
	 * @return ICVSRemoteFolder
	 */
	public ICVSRemoteFolder getRemoteFolder(String path, CVSTag tag, IProgressMonitor monitor) {
		if (isDefinedModuleName(path)) {
			return getDefinedModule(getDefinedModuleName(path), tag, monitor);
		} else {
			return root.getRemoteFolder(path, tag);
		}
	}

	static boolean isDefinedModuleName(String path) {
		return path.startsWith(DEFINED_MODULE_PREFIX);
	}

	static String getDefinedModuleName(String path) {
		return path.substring(DEFINED_MODULE_PREFIX.length());
	}
	
	static String asDefinedModulePath(String path) {
		return DEFINED_MODULE_PREFIX + path;
	}
	
	/**
	 * Method getDefinedModule.
	 * @param path
	 * @param tag
	 * @param monitor
	 * @return ICVSRemoteFolder
	 */
	private ICVSRemoteFolder getDefinedModule(String path, CVSTag tag, IProgressMonitor monitor) {
		Map cache = getDefinedModulesCache(tag, monitor);
		ICVSRemoteFolder folder = (ICVSRemoteFolder)cache.get(path);
		if (folder != null) {
			folder = (ICVSRemoteFolder)folder.forTag(tag);
		}
		return folder;
	}
	
	private Map getDefinedModulesCache(CVSTag tag, IProgressMonitor monitor) {
		if (modulesCache == null) {
			try {
				// Fetch the modules before locking the cache (to avoid deadlock)
				ICVSRemoteResource[] folders = root.members(CVSTag.DEFAULT, true, monitor);
				synchronized(modulesCacheLock) {
					modulesCache = new HashMap();
					for (int i = 0; i < folders.length; i++) {
						ICVSRemoteResource resource = folders[i];
						modulesCache.put(resource.getName(), resource);
					}
				}
			} catch (CVSException e) {
				// we could't fetch the modules. Log the problem and continue
				CVSUIPlugin.log(e);
				// Return an empty map but don't save it so the fetching of 
				// the modules will occur again
				return new HashMap();
			}
		}
		return modulesCache;
	}
	
	public ICVSRemoteResource[] getDefinedModules(CVSTag tag, IProgressMonitor monitor) {
		Map cache = getDefinedModulesCache(tag, monitor);
		return (ICVSRemoteResource[]) cache.values().toArray(new ICVSRemoteResource[cache.size()]);
	}
	
	public static String getRemotePathFor(ICVSResource resource) throws CVSException {
		if (resource.isFolder()) {
			if (resource instanceof ICVSRemoteFolder) {
				ICVSRemoteFolder remoteFolder = (ICVSRemoteFolder) resource;
				if (remoteFolder.isDefinedModule()) {
					return asDefinedModulePath(remoteFolder.getName());
				}
			}
			FolderSyncInfo info = ((ICVSFolder)resource).getFolderSyncInfo();
			if (info == null)
				throw new CVSException(Policy.bind("RepositoryRoot.folderInfoMissing", resource.getName())); //$NON-NLS-1$
			return info.getRepository();
		} else {
			FolderSyncInfo info = resource.getParent().getFolderSyncInfo();
			if (info == null)
				throw new CVSException(Policy.bind("RepositoryRoot.folderInfoMissing", resource.getParent().getName())); //$NON-NLS-1$
			String path = new Path(null, info.getRepository()).append(resource.getName()).toString();
			return path;
		}
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
		addDateTags(tags);
		addVersionAndBranchTags(remotePath, tags);
	}

	private void addDateTags(CVSTag[] tags){
		for(int i = 0; i < tags.length; i++){
			if(tags[i].getType() == CVSTag.DATE){
				dateTags.add(tags[i]);
			}
		}
	}
	private void addVersionAndBranchTags(String remotePath, CVSTag[] tags) {
		// Get the name to cache the version tags with
		String name = getCachePathFor(remotePath);
		TagCacheEntry cache = getTagCache(name);
		cache.addTags(tags);
	}

    /**
	 * Add the given date tag to the list of date tags associated with the repository.
	 * @param tag a date tag
	 */
	public void addDateTag(CVSTag tag) {
		if (!dateTags.contains(tag)) {
			dateTags.add(tag);
		}
	}
	public void removeDateTag(CVSTag tag) {
		if (dateTags.contains(tag)) {
			dateTags.remove(tag);
		}
	}
	/**
	 * Return the list of date tags assocaiated with the repository.
	 * @return the list of date tags
	 */
	public CVSTag[] getDateTags() {
		return (CVSTag[]) dateTags.toArray(new CVSTag[dateTags.size()]);
	}
	
	/**
	 * Remove the given tags from the receiver
	 * @param remotePath
	 * @param tags
	 */
	public void removeTags(String remotePath, CVSTag[] tags) {
		removeDateTags(tags);
		removeVersionAndBranchTags(remotePath, tags);
	}
	
	private void removeDateTags(CVSTag[] tags) {		
		if(dateTags.isEmpty())return;
		// Store the tag with the appropriate ancestor
		for (int i = 0; i < tags.length; i++) {
			dateTags.remove(tags[i]);
		}
	}

	private void removeVersionAndBranchTags(String remotePath, CVSTag[] tags) {
		// Get the name to cache the version tags with
		String name = getCachePathFor(remotePath);
		
		// Make sure there is a table for the ancestor that holds the tags
		TagCacheEntry entry = (TagCacheEntry)versionAndBranchTags.get(name);
		if (entry == null) {
			return;
		}
		
		// Store the tag with the appropriate ancestor
		entry.removeTags(tags);
	}

	/**
	 * Returns the absolute paths of the auto refresh files relative to the
	 * repository.
	 * 
	 * @return String[]
	 */
	public String[] getAutoRefreshFiles(String remotePath) {
		String name = getCachePathFor(remotePath);
		Set files = (Set)autoRefreshFiles.get(name);
		if (files == null || files.isEmpty()) {
			// convert the default relative file paths to full paths
			if (isDefinedModuleName(remotePath)) {
				return new String[0];
			}
			List result = new ArrayList();
			for (int i = 0; i < DEFAULT_AUTO_REFRESH_FILES.length; i++) {
				String relativePath = DEFAULT_AUTO_REFRESH_FILES[i];
				result.add(new Path(null, remotePath).append(relativePath).toString());
			}
			return (String[]) result.toArray(new String[result.size()]);
		} else {
			return (String[]) files.toArray(new String[files.size()]);
		}
	}
	
	/**
	 * Sets the auto refresh files for the given remote path to the given
	 * string values which are absolute file paths (relative to the receiver).
	 * 
	 * @param autoRefreshFiles The autoRefreshFiles to set
	 */
	public void setAutoRefreshFiles(String remotePath, String[] autoRefreshFiles) {
		Set newFiles = new HashSet(Arrays.asList(autoRefreshFiles));
		// Check to see if the auto-refresh files are the default files
		if (autoRefreshFiles.length == DEFAULT_AUTO_REFRESH_FILES.length) {
			boolean isDefault = true;
			for (int i = 0; i < DEFAULT_AUTO_REFRESH_FILES.length; i++) {
				String filePath = DEFAULT_AUTO_REFRESH_FILES[i];
				if (!newFiles.contains(new Path(null, remotePath).append(filePath).toString())) {
					isDefault = false;
					break;
				}
			}
			if (isDefault) {
				this.autoRefreshFiles.remove(getCachePathFor(remotePath));
				return;
			}
		}
		this.autoRefreshFiles.put(getCachePathFor(remotePath), newFiles);
	}

	/**
	 * Fetches tags from auto-refresh files.
	 */
	public CVSTag[] refreshDefinedTags(ICVSFolder folder, boolean recurse, IProgressMonitor monitor) throws TeamException {
	    ICacheableReference reference = getTagCache(getRemotePathFor(folder)).getReference();
	    int flags = ICacheableReference.ALWAYS_FETCH;
	    if (recurse) {
	        flags |= TagCacheSource.RECURSE;
	    }
        try {
            return (CVSTag[])reference.getObject(flags, monitor);
        } catch (CoreException e) {
            throw TeamException.asTeamException(e);
        }
	}
	
    private TagCacheEntry getTagCache(String remotePath) {
        TagCacheEntry cache = (TagCacheEntry)versionAndBranchTags.get(remotePath);
        if (cache == null) {
            cache = new TagCacheEntry(this, remotePath);
            versionAndBranchTags.put(remotePath, cache);
        }
        
        return cache;
    }

    /*
	 * Method invoked from TagCacheSource to fetch the tags from the repository
	 */
    /* package */ CVSTag[] fetchTags(ICVSFolder folder, boolean recurse, IProgressMonitor monitor) throws TeamException, CVSException {
        monitor.beginTask(null, 100);
        try {
		    CVSTag[] tags = null;
		    if (!recurse) {
		        // Only try the auto-refresh file(s) if we are not recursing into sub-folders
		        tags = fetchTagsUsingAutoRefreshFiles(folder, Policy.subMonitorFor(monitor, 50));
		    }
	        if (tags == null || tags.length == 0) {
	            // There we're no tags found on the auto-refresh files or we we're aksed to go deep
	            // Try using the log command
	            tags = fetchTagsUsingLog(folder, recurse, Policy.subMonitorFor(monitor, 50));
	        }
	        return tags;
        } finally {
            monitor.done();
        }
    }

    private CVSTag[] fetchTagsUsingLog(ICVSFolder folder, final boolean recurse, IProgressMonitor monitor) throws CVSException {
        LogEntryCache logEntries = new LogEntryCache();
        RemoteLogOperation operation = new RemoteLogOperation(null, new ICVSRemoteResource[] { asRemoteResource(folder) }, null, null, logEntries) {
            protected Command.LocalOption[] getLocalOptions(CVSTag tag1,CVSTag tag2) {
                Command.LocalOption[] options = super.getLocalOptions(tag1, tag2);
                if (recurse) 
                    return options;
                Command.LocalOption[] newOptions = new Command.LocalOption[options.length + 1];
                System.arraycopy(options, 0, newOptions, 0, options.length);
                newOptions[options.length] = Command.DO_NOT_RECURSE;
                return newOptions;
            }
        };
        try {
            operation.run(monitor);
        } catch (InvocationTargetException e) {
            throw CVSException.wrapException(e);
        } catch (InterruptedException e) {
            // Ignore;
        }
        String[] keys = logEntries.getCachedFilePaths();
        Set tags = new HashSet();
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            ILogEntry[] entries = logEntries.getLogEntries(key);
            for (int j = 0; j < entries.length; j++) {
                ILogEntry entry = entries[j];
                tags.addAll(Arrays.asList(entry.getTags()));
            }
        }
        return (CVSTag[]) tags.toArray(new CVSTag[tags.size()]);
    }

    private ICVSRemoteResource asRemoteResource(ICVSFolder folder) throws CVSException {
        if (folder instanceof ICVSRemoteResource) {
            return (ICVSRemoteResource)folder;
        }
        return CVSWorkspaceRoot.getRemoteResourceFor(folder);
    }

    /**
	 * Fetches tags from auto-refresh files.
	 */
	private CVSTag[] fetchTagsUsingAutoRefreshFiles(ICVSFolder folder, IProgressMonitor monitor) throws TeamException {
	    String remotePath = getRemotePathFor(folder);
		String[] filesToRefresh = getAutoRefreshFiles(remotePath);
		try {
			monitor.beginTask(null, filesToRefresh.length * 10); //$NON-NLS-1$
			List tags = new ArrayList();
			for (int i = 0; i < filesToRefresh.length; i++) {
				ICVSRemoteFile file = root.getRemoteFile(filesToRefresh[i], CVSTag.DEFAULT);
				try {
                    tags.addAll(Arrays.asList(fetchTags(file, Policy.subMonitorFor(monitor, 5))));
        		} catch (TeamException e) {
        			IStatus status = e.getStatus();
        			boolean doesNotExist = false;
        			if (status.getCode() == CVSStatus.SERVER_ERROR && status.isMultiStatus()) {
        				IStatus[] children = status.getChildren();
        				if (children.length == 1 && children[0].getCode() == CVSStatus.DOES_NOT_EXIST) {
        				    // Don't throw an exception if the file does no exist
        					doesNotExist = true;
        				}
        			}
        			if (!doesNotExist) {
        			    throw e;
        			}
                }
			}
			return (CVSTag[]) tags.toArray(new CVSTag[tags.size()]);
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
	
	/*
	 * Return the cache key (path) for the given folder path.
	 * This has been changed to cache the tags directly 
	 * with the folder to better support non-root projects.
	 * However, resources in the local workspace use the folder
	 * the project is mapped to as the tag source (see TagSource)
	 */
	private String getCachePathFor(String remotePath) {
		return remotePath;
	}
	
	/**
	 * Write out the state of the receiver as XML on the given XMLWriter.
	 * 
	 * @param writer
	 * @throws IOException
	 */
	public void writeState(XMLWriter writer) {

		HashMap attributes = new HashMap();

		attributes.clear();
		attributes.put(RepositoriesViewContentHandler.ID_ATTRIBUTE, root.getLocation());
		if (name != null) {
			attributes.put(RepositoriesViewContentHandler.NAME_ATTRIBUTE, name);
		}
		if (cacheDirectory != null) {
		    attributes.put(RepositoriesViewContentHandler.CACHE_ATTRIBUTE, cacheDirectory);
		}
		
		writer.startTag(RepositoriesViewContentHandler.REPOSITORY_TAG, attributes, true);

		//put date tag under repository
		if(!dateTags.isEmpty()){
			writer.startTag(RepositoriesViewContentHandler.DATE_TAGS_TAG, attributes, true);
			Iterator iter = dateTags.iterator();
			while(iter.hasNext()){
				CVSTag tag = (CVSTag)iter.next();
				writeATag(writer, attributes, tag, RepositoriesViewContentHandler.DATE_TAG_TAG);
			}
			writer.endTag(RepositoriesViewContentHandler.DATE_TAGS_TAG);
		}
		
		// Gather all the modules that have tags and/or auto-refresh files
		// for each module, write the moduel, tags and auto-refresh files.
		String[] paths = getKnownRemotePaths();
		for (int i = 0; i < paths.length; i++) {
			String path = paths[i];
			attributes.clear();
			String name = path;
			if (isDefinedModuleName(path)) {
				name = getDefinedModuleName(path);
				attributes.put(RepositoriesViewContentHandler.TYPE_ATTRIBUTE, RepositoriesViewContentHandler.DEFINED_MODULE_TYPE);
			}
			attributes.put(RepositoriesViewContentHandler.PATH_ATTRIBUTE, name);
			writer.startTag(RepositoriesViewContentHandler.MODULE_TAG, attributes, true);
			
			TagCacheEntry entry = (TagCacheEntry)versionAndBranchTags.get(path);
			if (entry != null) {
			    if (entry.isExpired()) {
			        entry.dispose();
			    } else {
			        entry.write(writer);	        
			    }
			}
			Set refreshSet = (Set)autoRefreshFiles.get(path);
			if (refreshSet != null) {
				Iterator filenameIt = refreshSet.iterator();
				while (filenameIt.hasNext()) {
					String filename = (String)filenameIt.next();
					attributes.clear();
					attributes.put(RepositoriesViewContentHandler.FULL_PATH_ATTRIBUTE, filename);
					writer.startAndEndTag(RepositoriesViewContentHandler.AUTO_REFRESH_FILE_TAG, attributes, true);
				}
			}
			writer.endTag(RepositoriesViewContentHandler.MODULE_TAG);
		}
		writer.endTag(RepositoriesViewContentHandler.REPOSITORY_TAG);
	}

    private void writeATag(XMLWriter writer, HashMap attributes, CVSTag tag, String s) {
		attributes.clear();
		attributes.put(RepositoriesViewContentHandler.NAME_ATTRIBUTE, tag.getName());
		attributes.put(RepositoriesViewContentHandler.TYPE_ATTRIBUTE, RepositoriesViewContentHandler.TAG_TYPES[tag.getType()]);
		writer.startAndEndTag(s, attributes, true);
	}

	/**
	 * Method getKnownTags.
	 * @param remotePath
	 * @return CVSTag[]
	 */
	public CVSTag[] getAllKnownTags(String remotePath) {
	    TagCacheEntry entry = (TagCacheEntry)versionAndBranchTags.get(getCachePathFor(remotePath));
		if(entry != null){
		    entry.accessed();
			CVSTag[] tags1;
            try {
                tags1 = (CVSTag[]) entry.getReference().getObject(ICacheableReference.ONLY_LOAD_REPLICA_IF_ABSENT, null);
            } catch (CoreException e) {
                // Log and continue
                CVSUIPlugin.log(e);
                tags1= new CVSTag[0];
            }
            CVSTag[] tags2 = getDateTags();
			int len = tags1.length + tags2.length;
			CVSTag[] tags = new CVSTag[len];
			for(int i = 0; i < len; i++){
				if(i < tags1.length){
					tags[i] = tags1[i];
				}else{
					tags[i] = tags2[i-tags1.length];
				}
			}
			return tags;
		}
		return getDateTags();
	}

	public String[] getKnownRemotePaths() {
		Set paths = new HashSet();
		paths.addAll(versionAndBranchTags.keySet());
		paths.addAll(autoRefreshFiles.keySet());
		return (String[]) paths.toArray(new String[paths.size()]);
	}
	/**
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (ICVSRepositoryLocation.class.equals(adapter)) return getRoot();
		return super.getAdapter(adapter);
	}

	public ICVSRemoteResource[] filterResources(ICVSRemoteResource[] resource) {
		List result = new ArrayList();
		for (int i = 0; i < resource.length; i++) {
			ICVSRemoteResource remoteResource = resource[i];
			if (remoteResource instanceof ICVSRemoteFolder) {
				ICVSRemoteFolder folder = (ICVSRemoteFolder) remoteResource;
				if (tagIsKnown(remoteResource)) {
					result.add(folder);
				}
			}
		}
		return (ICVSRemoteResource[]) result.toArray(new ICVSRemoteResource[result.size()]);
	}
	
	/**
	 * Method tagIsKnown.
	 * @param remoteResource
	 * @return boolean
	 */
	public boolean tagIsKnown(ICVSRemoteResource remoteResource) {
		if (remoteResource instanceof ICVSRemoteFolder) {
			ICVSRemoteFolder folder = (ICVSRemoteFolder) remoteResource;
			String path = getCachePathFor(folder.getRepositoryRelativePath());
			CVSTag[] tags = getAllKnownTags(path);
			CVSTag tag = folder.getTag();
			for (int i = 0; i < tags.length; i++) {
				CVSTag knownTag = tags[i];
				if (knownTag.equals(tag)) return true;
			}
		}
		return false;
	}
	
	/**
	 * This method is invoked whenever the refresh button in the
	 * RepositoriesView is pressed.
	 */
	void clearCache() {
		synchronized(modulesCacheLock) {
			if (modulesCache != null)
				modulesCache = null;
		}
	}

	/**
	 * Sets the root.
	 * @param root The root to set
	 */
	void setRepositoryLocation(ICVSRepositoryLocation root) {
		this.root = root;
	}

    /*
     * Set the last access time of the cache entry for the given path
     * as it was read from the persitent store.
     */
    /* package */ void setLastAccessedTime(String remotePath, long lastAccessTime) {
	    TagCacheEntry entry = (TagCacheEntry)versionAndBranchTags.get(getCachePathFor(remotePath));
		if(entry != null){
		    entry.lastAccessTime = lastAccessTime;
		}
    }

    /* package */ String getCacheDirectoryName() {
        if (cacheDirectory == null) {
            cacheDirectory = "cache." + System.currentTimeMillis(); //$NON-NLS-1$
        }
        return cacheDirectory;
    }
    
    /* package */ void setCacheDirectory(String cache) {
        cacheDirectory = cache;
    }
    
    /*
     * Recreate the tag cache entry from disk on startup
     */
    /* package */ void addLocalReplica(String currentRemotePath, String replicaPath, long cachedTime) {
        TagCacheEntry entry = getTagCache(currentRemotePath);
        if (cachedTime != 0) {
            entry.lastAccessTime = cachedTime;
        }
        entry.fileName = replicaPath;
    }

    /**
     * Clear up any cached state for the repository
     */
    public void dispose() {
        File file = getCachePath().toFile();
        deepDelete(file);
    }
    
	private static void deepDelete(File resource) {
		if (resource.isDirectory()) {
			File[] fileList = resource.listFiles();
			for (int i = 0; i < fileList.length; i++) {
				deepDelete(fileList[i]);
			}
		}
		resource.delete();
	}
	
    /* package */ IPath getCachePath() {
        String dirName = getCacheDirectoryName();
        return CVSUIPlugin.getPlugin().getStateLocation().append(dirName);
    }

}
