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
package org.eclipse.team.internal.ccvs.core.resources;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSStatus;
import org.eclipse.team.internal.ccvs.core.Policy;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.SyncFileWriter;

/**
 * This cache uses session properties to hold the bytes representing the sync
 * info
 */
/*package*/ class SessionPropertySyncInfoCache extends LowLevelSyncInfoCache {
	
	private static final String[] NULL_IGNORES = new String[0];
	private static final FolderSyncInfo NULL_FOLDER_SYNC_INFO = new FolderSyncInfo("", "", null, false); //$NON-NLS-1$ //$NON-NLS-2$
	private static final byte[][] EMPTY_RESOURCE_SYNC_INFOS = new byte[0][0];
	
	private Set changedResourceSync = new HashSet();
	private Set changedFolderSync = new HashSet();
	
	/**
	 * If not already cached, loads and caches the folder ignores sync for the container.
	 * Folder must exist and must not be the workspace root.
	 *
	 * @param container the container
	 * @return the folder ignore patterns, or an empty array if none
	 */
	/*package*/ String[] cacheFolderIgnores(IContainer container) throws CVSException {
		try {
			// don't try to load if the information is already cached
			String[] ignores = (String[])container.getSessionProperty(IGNORE_SYNC_KEY);
			if (ignores == null) {
				// read folder ignores and remember it
				ignores = SyncFileWriter.readCVSIgnoreEntries(container);
				if (ignores == null) ignores = NULL_IGNORES;
				container.setSessionProperty(IGNORE_SYNC_KEY, ignores);
			}
			return ignores;
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}


	/**
	 * If not already cached, loads and caches the folder sync for the container.
	 * Folder must exist and must not be the workspace root.
	 *
	 * @param container the container
	 * @return the folder sync info for the folder, or null if none.
	 */
	/*package*/ FolderSyncInfo cacheFolderSync(IContainer container) throws CVSException {
		try {
			// don't try to load if the information is already cached
			FolderSyncInfo info = (FolderSyncInfo)container.getSessionProperty(FOLDER_SYNC_KEY);
			if (info == null) {
				// read folder sync info and remember it
				info = SyncFileWriter.readFolderSync(container);
				if (info == null) {
					container.setSessionProperty(FOLDER_SYNC_KEY, NULL_FOLDER_SYNC_INFO);
				} else {
					container.setSessionProperty(FOLDER_SYNC_KEY, info);
				}
			} else if (info == NULL_FOLDER_SYNC_INFO) {
				info = null;
			}
			return info;
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}


	/**
	 * If not already cached, loads and caches the resource sync for the children of the container.
	 * Folder must exist and must not be the workspace root.
	 *
	 * @param container the container
	 */
	/*package*/ void cacheResourceSyncForChildren(IContainer container) throws CVSException {
		try {
			// don't try to load if the information is already cached
			byte[][] infos = (byte[][])container.getSessionProperty(RESOURCE_SYNC_KEY);
			if (infos == null) {
				// load the sync info from disk
				infos = SyncFileWriter.readAllResourceSync(container);
				if (infos == null) {
					infos = EMPTY_RESOURCE_SYNC_INFOS;
				}
				container.setSessionProperty(RESOURCE_SYNC_KEY, infos);
			}
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}

	/**
	 * Returns the folder sync info for the container; null if none.
	 * Folder must exist and must not be the workspace root.
	 * The folder sync info for the container MUST ALREADY BE CACHED.
	 *
	 * @param container the container
	 * @return the folder sync info for the folder, or null if none.
	 * @see #cacheFolderSync
	 */
	/*package*/ FolderSyncInfo getCachedFolderSync(IContainer container) throws CVSException {
		try {
			FolderSyncInfo info = (FolderSyncInfo)container.getSessionProperty(FOLDER_SYNC_KEY);
			if (info == null) {
				// There should be sync info but it was missing. Report the error
				throw new CVSException(Policy.bind("EclipseSynchronizer.folderSyncInfoMissing", container.getFullPath().toString())); //$NON-NLS-1$
			}
			if (info == NULL_FOLDER_SYNC_INFO) return null;
			return info;
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}

	/**
	 * Returns the resource sync info for all children of the container.
	 * Container must exist and must not be the workspace root.
	 * The resource sync info for the children of the container MUST ALREADY BE CACHED.
	 *
	 * @param container the container
	 * @return a collection of the resource sync info's for all children
	 * @see #cacheResourceSyncForChildren
	 */
	/*package*/ byte[][] getCachedResourceSyncForChildren(IContainer container) throws CVSException {
		try {
			byte[][] infos = (byte[][])container.getSessionProperty(RESOURCE_SYNC_KEY);
			// todo: move check to caller
			if (infos == null) {
				// There should be sync info but it was missing. Report the error
				throw new CVSException(Policy.bind("EclipseSynchronizer.folderSyncInfoMissing", container.getFullPath().toString())); //$NON-NLS-1$
			}
			return infos;
		} catch(CoreException e) {
			throw CVSException.wrapException(e);
		}
	}

	/**
	 * Purges the cache recursively for all resources beneath the container.
	 * There must not be any pending uncommitted changes.
	 */
	/*package*/ void purgeCache(IContainer container, boolean deep) throws CVSException {
		if (! container.exists()) return;
		try {
			if (container.getType() != IResource.ROOT) {
				container.setSessionProperty(RESOURCE_SYNC_KEY, null);
				container.setSessionProperty(IGNORE_SYNC_KEY, null);
				container.setSessionProperty(FOLDER_SYNC_KEY, null);
			}
			if(deep) {
				IResource[] members = container.members();
				for (int i = 0; i < members.length; i++) {
					IResource resource = members[i];
					if (resource.getType() != IResource.FILE) {
						purgeCache((IContainer) resource, deep);
					}
				}
			}
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}

	/**
	 * Sets the array of folder ignore patterns for the container, must not be null.
	 * Folder must exist and must not be the workspace root.
	 *
	 * @param container the container
	 * @param ignores the array of ignore patterns
	 */
	/*package*/ void setCachedFolderIgnores(IContainer container, String[] ignores) throws CVSException {
		try {
			container.setSessionProperty(IGNORE_SYNC_KEY, ignores);
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}


	/**
	 * Sets the folder sync info for the container; if null, deletes it.
	 * Folder must exist and must not be the workspace root.
	 * The folder sync info for the container need not have previously been cached.
	 *
	 * @param container the container
	 * @param info the new folder sync info
	 */
	/*package*/ void setCachedFolderSync(IContainer container, FolderSyncInfo info) throws CVSException {
		try {
			if (info == null) info = NULL_FOLDER_SYNC_INFO;
			container.setSessionProperty(FOLDER_SYNC_KEY, info);
			changedFolderSync.add(container);
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}
	
	/**
	 * Sets the resource sync info for the resource; if null, deletes it. Parent
	 * must exist and must not be the workspace root. The resource sync info for
	 * the children of the parent container MUST ALREADY BE CACHED.
	 *
	 * @param resource the resource
	 * @param info the new resource sync info
	 * @see #cacheResourceSyncForChildren
	 */
	/*package*/ void setCachedResourceSyncForChildren(IContainer container, byte[][] infos) throws CVSException {
		try {
			if (infos == null)
				infos = EMPTY_RESOURCE_SYNC_INFOS;
			container.setSessionProperty(RESOURCE_SYNC_KEY, infos);
			changedResourceSync.add(container);
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}
	
	/**
	 * Commits the cache after a series of operations.
	 *
	 * Will return STATUS_OK unless there were problems writting sync
	 * information to disk. If an error occurs a multistatus is returned
	 * with the list of reasons for the failures. Failures are recovered,
	 * and all changed resources are given a chance to be written to disk.
	 *
	 * @param monitor the progress monitor, may be null
	 */
	/*package*/ IStatus commitCache(IProgressMonitor monitor) {
		List errors = new ArrayList();
		try {
			/*** prepare operation ***/
			// find parents of changed resources

			monitor = Policy.monitorFor(monitor);
			int numDirty = changedResourceSync.size();
			int numResources = changedFolderSync.size() + numDirty;
			monitor.beginTask(null, numResources);
			if(monitor.isCanceled()) {
				monitor.subTask(Policy.bind("EclipseSynchronizer.UpdatingSyncEndOperationCancelled")); //$NON-NLS-1$
			} else {
				monitor.subTask(Policy.bind("EclipseSynchronizer.UpdatingSyncEndOperation")); //$NON-NLS-1$
			}

			/*** write sync info to disk ***/
			// folder sync info changes
			for(Iterator it = changedFolderSync.iterator(); it.hasNext();) {
				IContainer folder = (IContainer) it.next();
				if (folder.exists() && folder.getType() != IResource.ROOT) {
					try {
						FolderSyncInfo info = getCachedFolderSync(folder);
						if (info == null) {
							// deleted folder sync info since we loaded it
							SyncFileWriter.deleteFolderSync(folder);
							changedResourceSync.remove(folder);
						} else {
							// modified or created new folder sync info since we loaded it
							SyncFileWriter.writeFolderSync(folder, info);
						}
					} catch(CVSException e) {
						try {
							purgeCache(folder, true /* deep */);
						} catch(CVSException pe) {
							errors.add(pe.getStatus());
						}
						errors.add(e.getStatus());
					}
				}
				monitor.worked(1);
			}

			// update progress for parents we will skip because they were deleted
			monitor.worked(numDirty - changedResourceSync.size());

			// resource sync info changes
			for (Iterator it = changedResourceSync.iterator(); it.hasNext();) {
				IContainer folder = (IContainer) it.next();
				if (folder.exists() && folder.getType() != IResource.ROOT) {
					// write sync info for all children in one go
					try {
						byte[][] infos = getCachedResourceSyncForChildren(folder);
						SyncFileWriter.writeAllResourceSync(folder, infos);
					} catch(CVSException e) {
						try {
							purgeCache(folder, false /* depth 1 */);
						} catch(CVSException pe) {
							errors.add(pe.getStatus());
						}
						errors.add(e.getStatus());
					}
				}
				monitor.worked(1);
			}

			/*** broadcast events ***/
			changedResourceSync.clear();
			changedFolderSync.clear();
			if ( ! errors.isEmpty()) {
				MultiStatus status = new MultiStatus(CVSProviderPlugin.ID,
											CVSStatus.COMMITTING_SYNC_INFO_FAILED,
											Policy.bind("EclipseSynchronizer.ErrorCommitting"), //$NON-NLS-1$
											null);
				for (int i = 0; i < errors.size(); i++) {
					status.merge((IStatus)errors.get(i));
				}
				return status;
			}
			return STATUS_OK;
		} finally {
			monitor.done();
		}
	}
}
