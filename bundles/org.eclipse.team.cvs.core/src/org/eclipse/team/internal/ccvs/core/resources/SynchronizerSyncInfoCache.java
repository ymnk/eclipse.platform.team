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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ISynchronizer;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;

/**
 * This cache uses session properties to hold the bytes representing the sync
 * info
 */
/*package*/ class SynchronizerSyncInfoCache extends LowLevelSyncInfoCache {

	/**
	 * Return the Eclipse Workspace Synchronizer (from org.eclipse.core.resources)
	 */
	private ISynchronizer getWorkspaceSynchronizer() {
		return ResourcesPlugin.getWorkspace().getSynchronizer();
	}

	/**
	 * Convert a FolderSyncInfo into a byte array that can be stored
	 * in the workspace synchronizer
	 */
	private static byte[] getBytes(FolderSyncInfo info) throws CVSException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(out);
		try {
			dos.writeUTF(info.getRoot());
			dos.writeUTF(info.getRepository());
			CVSEntryLineTag tag = info.getTag();
			if (tag == null) {
				dos.writeUTF(""); //$NON-NLS-1$
			} else {
				dos.writeUTF(tag.toString());
			}
			dos.writeBoolean(info.getIsStatic());
			dos.close();
		} catch (IOException e) {
			throw CVSException.wrapException(e);
		}
		return out.toByteArray();
	}

	/**
	 * Convert a byte array that was created using getBytes(FolderSyncInfo)
	 * into a FolderSyncInfo
	 */
	private static FolderSyncInfo getFolderSyncInfo(byte[] bytes) throws CVSException {
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		DataInputStream dis = new DataInputStream(in);
		String root;
		String repository;
		CVSEntryLineTag tag;
		boolean isStatic;
		try {
			root = dis.readUTF();
			repository = dis.readUTF();
			String tagName = dis.readUTF();
			if (tagName.length() == 0) {
				tag = null;
			} else {
				tag = new CVSEntryLineTag(tagName);
			}
			isStatic = dis.readBoolean();
		} catch (IOException e) {
			throw CVSException.wrapException(e);
		}
		return new FolderSyncInfo(repository, root, tag, isStatic);
	}
		
	/**
	 * Method getBytes converts an array of bytes into a single byte array
	 * @param infos
	 * @return byte[]
	 */
	private byte[] getBytes(byte[][] infos) {
		// todo
		return null;
	}
	
	/**
	 * Convert a byte array that was created using getBytes(Map)
	 * into a Map of ResourceSyncInfo
	 */
	private static byte[][] getResourceSyncInfo(byte[] bytes) throws CVSException {
		// todo
		return null;
	}

	/**
	 * Flush any info cahced for the folder
	 */
	private void flushPhantomInfo(IContainer container) throws CVSException {
		try {
			if (container.exists() || container.isPhantom()) {
				getWorkspaceSynchronizer().flushSyncInfo(FOLDER_SYNC_KEY, container, IResource.DEPTH_ZERO);
			}
			if (container.exists() || container.isPhantom()) {
				getWorkspaceSynchronizer().flushSyncInfo(RESOURCE_SYNC_KEY, container, IResource.DEPTH_ZERO);
			}
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}
	
	/*package*/ void flush(IProject project) throws CVSException {
		try {
			getWorkspaceSynchronizer().flushSyncInfo(FOLDER_SYNC_KEY, project, IResource.DEPTH_INFINITE);
			getWorkspaceSynchronizer().flushSyncInfo(RESOURCE_SYNC_KEY, project, IResource.DEPTH_INFINITE);
			getWorkspaceSynchronizer().flushSyncInfo(DIRTY_COUNT, project, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}
	
	/**
	 * Method flush.
	 * @param folder
	 */
	/*package*/ void flush(IFolder folder) throws CVSException {
		flushPhantomInfo(folder);
	}
	
	/**
	 * If not already cached, loads and caches the folder sync for the container.
	 * Folder must exist and must not be the workspace root.
	 *
	 * @param container the container
	 * @return the folder sync info for the folder, or null if none.
	 */
	/*package*/ FolderSyncInfo cacheFolderSync(IContainer container) throws CVSException {
		// nothing needs to be done since the synchronizer is persisted
		return getCachedFolderSync(container);
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
			byte[] bytes = getWorkspaceSynchronizer().getSyncInfo(FOLDER_SYNC_KEY, container);
			if (bytes == null) return null;
			return getFolderSyncInfo(bytes);
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}
	
	/**
	 * Sets the folder sync info for the container; if null, deletes it.
	 * Folder must exist and must not be the workspace root.
	 * The folder sync info for the container need not have previously been
	 * cached.
	 *
	 * @param container the container
	 * @param info the new folder sync info
	 */
	/*package*/ void setCachedFolderSync(IContainer container, FolderSyncInfo info) throws CVSException {
		try {
			if (info == null) {
				if (container.exists() || container.isPhantom()) {
					getWorkspaceSynchronizer().flushSyncInfo(FOLDER_SYNC_KEY, container, IResource.DEPTH_ZERO);
				}
			} else {
				getWorkspaceSynchronizer().setSyncInfo(FOLDER_SYNC_KEY, container, getBytes(info));
			}
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
		// nothing needs to be done since the synchronizer is persisted
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
			byte[] bytes = getWorkspaceSynchronizer().getSyncInfo(RESOURCE_SYNC_KEY, container);
			if (bytes == null) return null;
			return getResourceSyncInfo(bytes);
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
			if (infos == null) {
				if (container.exists() || container.isPhantom()) {
					getWorkspaceSynchronizer().flushSyncInfo(RESOURCE_SYNC_KEY, container, IResource.DEPTH_ZERO);
				}
			} else {
				getWorkspaceSynchronizer().setSyncInfo(RESOURCE_SYNC_KEY, container, getBytes(infos));
			}
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}
	
	/**
	 * @see org.eclipse.team.internal.ccvs.core.resources.LowLevelSyncInfoCache#commitCache(org.eclipse.core.runtime.IProgressMonitor)
	 */
	IStatus commitCache(IProgressMonitor monitor) {
		// Nothing needs to be done since the synchronizer is persisted
		return STATUS_OK;
	}
}
