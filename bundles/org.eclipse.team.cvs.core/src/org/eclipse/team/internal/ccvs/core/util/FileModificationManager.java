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
package org.eclipse.team.internal.ccvs.core.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.ISavedState;
import org.eclipse.core.resources.ISynchronizer;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.IResourceStateChangeListener;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;

/**
 * This class performs several functions related to determining the modified
 * status of files under CVS control. First, it listens for change delta's for
 * files and brodcasts them to all listeners. It also registers as a save
 * participant so that deltas generated before the plugin are loaded are not
 * missed. Secondly, it listens for CVS resource state change events and uses
 * these to properly mark files and folders as modified.
 */
public class FileModificationManager implements IResourceChangeListener, ISaveParticipant {
	
	private static final String IS_DIRTY_INDICATOR = "d";
	private static final String NOT_DIRTY_INDICATOR = "c";
	private static final String UPDATED_INDICATOR = "u";
	private static final QualifiedName IS_DIRTY = new QualifiedName(CVSProviderPlugin.ID, "is-dirty");
	private static final QualifiedName DIRTY_COUNT = new QualifiedName(CVSProviderPlugin.ID, "dirty-count");
	private static final QualifiedName CLEAN_UPDATE = new QualifiedName(CVSProviderPlugin.ID, "clean-update");
	private static final QualifiedName DELETED_CHILDREN = new QualifiedName(CVSProviderPlugin.ID, "deleted");
	
	private Set modifiedResources = new HashSet();
	
	/**
	 * Constructor for FileModificationManager.
	 */
	public FileModificationManager() {
		// add the sync key which is used to cache the dirty count for phantom folders
		getWorkspaceSynchronizer().add(DIRTY_COUNT);
	}

	/**
	 * Listen for file modifications and fire modification state changes
	 * 
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			event.getDelta().accept(new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta delta) throws CoreException {
					IResource resource = delta.getResource();

					if (resource.getType()==IResource.FILE && delta.getKind() == IResourceDelta.CHANGED) {
						contentsChanged((IFile)resource);
					}

					return true;
				}
			});
			if (!modifiedResources.isEmpty()) {
				CVSProviderPlugin.broadcastModificationStateChanges(
					(IResource[])modifiedResources.toArray(new IResource[modifiedResources.size()]),
					IResourceStateChangeListener.CONTENTS_MODIFIED);
					modifiedResources.clear();
			}
		} catch (CoreException e) {
			CVSProviderPlugin.log(e.getStatus());
		}

	}
	
	/**
	 * We register a save participant so we can get the delta from workbench
	 * startup to plugin startup.
	 * @throws CoreException
	 */
	public void registerSaveParticipant() throws CoreException {
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		ISavedState ss = ws.addSaveParticipant(CVSProviderPlugin.getPlugin(), this);
		if (ss != null) {
			ss.processResourceChangeEvents(this);
		}
		ws.removeSaveParticipant(CVSProviderPlugin.getPlugin());
	}
	
	/**
	 * @see org.eclipse.core.resources.ISaveParticipant#doneSaving(org.eclipse.core.resources.ISaveContext)
	 */
	public void doneSaving(ISaveContext context) {
	}
	/**
	 * @see org.eclipse.core.resources.ISaveParticipant#prepareToSave(org.eclipse.core.resources.ISaveContext)
	 */
	public void prepareToSave(ISaveContext context) throws CoreException {
	}
	/**
	 * @see org.eclipse.core.resources.ISaveParticipant#rollback(org.eclipse.core.resources.ISaveContext)
	 */
	public void rollback(ISaveContext context) {
	}
	/**
	 * @see org.eclipse.core.resources.ISaveParticipant#saving(org.eclipse.core.resources.ISaveContext)
	 */
	public void saving(ISaveContext context) throws CoreException {
	}

	private ISynchronizer getWorkspaceSynchronizer() {
		return ResourcesPlugin.getWorkspace().getSynchronizer();
	}
	
	private boolean isModified(ICVSFile cvsFile, ResourceSyncInfo info) throws CVSException {
		if (info == null) return false;
		if(info.isMerged() || !cvsFile.exists()) return true;
		return !cvsFile.getTimeStamp().equals(info.getTimeStamp());
	}
	
	private void internalSetModified(IFile file, boolean modified) throws CVSException {
		try {
			if (file.exists()) {
				if (modified) {
					markFileDirty(file);
				} else {
					markFileClean(file);
				}
			} else {
				// the file doesn't exist so either the file is an outgoing deletion
				// or a deletion that has just been committed. Record the deletion 
				// if it is a resource modification, otherwise remove it from the
				// record.
				handleDeletion(file, modified /* record */);
			}
			if (modified)
				modifiedResources.add(file);
		} catch (CVSException e) {
		   // Should flush any cahced info so they are recalculated
			try {
			   flush(file);
			} catch (CoreException ex) {
				// This is bad because now we have no clue as to whether the properties are up to date
				// XXX Need a multi-status with original exception as well.
				throw new CVSException("Big trouble!", ex);
			}
			throw e;
	   }
	}
	
	/*
	 * Flush all cached info for the file and it's ancestors
	 */
	private void flush(IFile file) throws CoreException {
		try {
			if (file.exists())
				file.setSessionProperty(IS_DIRTY, null);
				file.setSessionProperty(CLEAN_UPDATE, null);
		} finally {
			flush(file.getParent());
		}
	}

	/*
	 * Flush all cached info for the container and it's ancestors
	 */
	private void flush(IContainer container) throws CoreException {
		if (container.getType() == IResource.ROOT) return;
		try {
			if (container.exists()) {
				container.setSessionProperty(DIRTY_COUNT, null);
				container.setSessionProperty(DELETED_CHILDREN, null);
				container.setPersistentProperty(IS_DIRTY, null);
			} else if (container.isPhantom()) {
				getWorkspaceSynchronizer().flushSyncInfo(DIRTY_COUNT, container, IResource.DEPTH_ZERO);
			}
		} finally {
			flush(container.getParent());
		}
	}
	
	/**
	 * Method adjustParentCount.
	 * @param file
	 * @param b
	 */
	private void adjustParent(IResource resource, boolean dirty) throws CVSException {
		IContainer parent = resource.getParent();
		if (parent.getType() == IResource.ROOT) return;
		Integer property = getDirtyCount(parent);
		if (property == null) {
			// The number of dirty children has not been tallied for this parent.
			// (i.e. no one has queried this folder yet)
			if (dirty) {
				// Make sure the parent and it's ansecestors 
				// are still marked as dirty (if they aren't already)
				String indicator = getFolderDirtyIndicator(parent);
				if (indicator.equals(NOT_DIRTY_INDICATOR)) {
					markFolderDirty(parent);
				} else if (indicator == null) {
					// The dirty state for the folder has never been cached
					// or the cache was flushed due to an error of some sort.
					// Let the next dirtyness query invoke the caching
				}
			} else {
				// Let the initial query of dirtyness determine if the persistent
				// property is still acurate.
			}
		} else {
			int count = property.intValue();
			if (dirty) {
				count++;
				if (count == 1) markFolderDirty(parent);
			} else {
				count--;
				if (count == 0) markFolderClean(parent);
			}
			setDirtyCount(parent, count);
		}
	}

	/*
	 * Mark the given existintg folder (that was previously dirty) as not dirty
	 * and adjust the parent folder accordingly.
	 */
	private void markFolderClean(IContainer parent) throws CVSException {
		try {
			parent.setPersistentProperty(IS_DIRTY, NOT_DIRTY_INDICATOR);
			adjustParent(parent, false);
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}

	/*
	 * Mark the given existing folder (that was not previously dirty) as dirty
	 * and adjust the parent folder accordingly.
	 */
	private void markFolderDirty(IContainer parent) throws CVSException {
		try {
			parent.setPersistentProperty(IS_DIRTY, IS_DIRTY_INDICATOR);
			adjustParent(parent, true);
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}

	/*
	 * Mark the given existing file as not dirty.
	 */
	private void markFileClean(IFile file) throws CVSException {
		try {
			if (file.getSessionProperty(IS_DIRTY) == NOT_DIRTY_INDICATOR) return;
			file.setSessionProperty(IS_DIRTY, NOT_DIRTY_INDICATOR);
			adjustParent(file, false /* dirty */);
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}

	/*
	 * Mark the given existing file as dirty.
	 */
	private void markFileDirty(IFile file) throws CVSException {
		try {
			if (file.getSessionProperty(IS_DIRTY) == IS_DIRTY_INDICATOR) return;
			file.setSessionProperty(IS_DIRTY, IS_DIRTY_INDICATOR);
			adjustParent(file, true /* dirty */);
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}
	
	/*
	 * Returns the folder dirty indicator for an existing folder
	 */
	private String getFolderDirtyIndicator(IContainer parent) throws CVSException {
		try {
			return parent.getPersistentProperty(IS_DIRTY);
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}

	/*
	 * Return the dirty count for the given folder. For existing folders, the
	 * dirty count may not have been calculated yet and this method will return
	 * null in that case. For phantom folders, the dirty count is calculated if
	 * it does not exist yet.
	 */
	private Integer getDirtyCount(IContainer parent) throws CVSException {
		try {
			if (parent.exists()) {
				// use the count session property
				return (Integer)parent.getSessionProperty(DIRTY_COUNT);
			} else if (parent.isPhantom()) {
				// get the count from the synchronizer
				byte[] bytes = getWorkspaceSynchronizer().getSyncInfo(DIRTY_COUNT, parent);
				if (bytes == null) {
					return calculateDirtyCountForPhantom(parent);
				}
				return new Integer(intFromBytes(bytes));
			} else {
				// XXX This is an error condition!
				return null;
			}
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}
	
	private void setDirtyCount(IContainer parent, int count) throws CVSException {
		try {
			if (parent.exists()) {
				// use the count session property
				parent.setSessionProperty(DIRTY_COUNT, new Integer(count));
			} else if (parent.isPhantom()) {
				// set the count for the phantom
				getWorkspaceSynchronizer().setSyncInfo(DIRTY_COUNT, parent, getBytes(count));
			}
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}
	
	/*
	 * Calculate the dirty cound for the given phatom folder, performing
	 * any necessary calculations on the childen as well
	 */
	private Integer calculateDirtyCountForPhantom(IContainer parent) throws CVSException {
		ICVSFolder cvsFolder = CVSWorkspaceRoot.getCVSFolderFor(parent);
		ICVSResource[] children = cvsFolder.members(ICVSFolder.MANAGED_MEMBERS | ICVSFolder.PHANTOM_MEMBERS);
		int count = 0;
		for (int i = 0; i < children.length; i++) {
			ICVSResource resource = children[i];
			if (resource.isFolder()) {
				Integer dc = getDirtyCount((IContainer)resource.getIResource());
				if (dc.intValue() > 0) count++;
			} else {
				// Any non-existant managed files are dirty (outgoing deletion)
				count++;
			}
		}
		setDirtyCount(parent, count);
		return new Integer(count);
	}
	
	/*
	 * Convert an int to a byte array
	 */
	private byte[] getBytes(int count) {
		byte[] result = new byte[4];
		result[0] = (byte)(count & 256);
		result[1] = (byte)(count<<8 & 256);
		result[1] = (byte)(count<<16 & 256);
		result[1] = (byte)(count<<24 & 256);
		return result;
	}

	/*
	 * Convert a byte array to an int
	 */
	private int intFromBytes(byte[] bytes) {
		return bytes[0] + (bytes[1]>>8) + (bytes[2]>>16) + (bytes[3]>>24);
	}
	
	/*
	 * Either record the deletion (if record is true) or remove the deletion if
	 * it is already recorded (if record is false).
	 */
	private void handleDeletion(IFile file, boolean record) throws CVSException {
		try {
			IContainer parent = file.getParent();
			if (parent.exists()) {
				// update the list of deleted files for the parent
				Set deletedFiles = (Set)parent.getSessionProperty(DELETED_CHILDREN);
				if (record = false && (deletedFiles == null || deletedFiles.isEmpty())) 
					return;
				if (deletedFiles == null)
					deletedFiles = new HashSet();
				String fileName = file.getName();
				if (record) {
					if (deletedFiles.contains(fileName))
						return;
					deletedFiles.add(fileName);
					parent.setSessionProperty(DELETED_CHILDREN, deletedFiles);
					adjustParent(file, true);
				} else {
					if (!deletedFiles.contains(fileName))
						return;
					deletedFiles.remove(fileName);
					if (deletedFiles.isEmpty()) deletedFiles = null;
					parent.setSessionProperty(DELETED_CHILDREN, deletedFiles);
					adjustParent(file, false);
				}
			} else {
				// calculate the proper counts for the phantom folders
				calculateDirtyCountForPhantom(parent);
			}
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}
		
	/**
	 * Method setModified.
	 * @param iFile
	 * @param b
	 */
	public void setModified(IFile file, boolean modified) throws CVSException {
		internalSetModified(file, modified);

	}
	
	/**
	 * Method syncInfoChanged.
	 * @param eclipseFile
	 * @param info
	 */
	public void syncInfoChanged(ICVSFile mFile, ResourceSyncInfo info) throws CVSException {
		IResource resource = mFile.getIResource();
		if (resource == null) return;
		setModified((IFile)resource, isModified(mFile, info));

	}
	
	/**
	 * Method syncInfoChanged.
	 * @param resources
	 */
	public void syncInfoChanged(IResource[] resources) throws CVSException {
		List files = new ArrayList();
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			if (resource.getType() == IResource.FILE) {
				ICVSFile file = CVSWorkspaceRoot.getCVSFileFor((IFile)resource);
				ResourceSyncInfo info = file.getSyncInfo();
				syncInfoChanged(file, info);
			}
		}
	}
	
	
	/**
	 * Method updated flags the objetc as having been modfied by the updated
	 * handler. This flag is read during the resource delta to determine whether
	 * the modification made the file dirty or not.
	 * 
	 * @param mFile
	 */
	public void updated(ICVSFile mFile) throws CVSException {
		IResource resource = mFile.getIResource();
		if (resource == null) return;
		try {
			resource.setSessionProperty(CLEAN_UPDATE, UPDATED_INDICATOR);
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}
	
	public void contentsChanged(IFile file) throws CoreException {
		Object indicator = file.getSessionProperty(CLEAN_UPDATE);
		boolean dirty = true;
		if (indicator == UPDATED_INDICATOR) {
			// the file was changed due to a clean update (i.e. no local mods) so skip it
			file.setSessionProperty(IS_DIRTY, NOT_DIRTY_INDICATOR);
			dirty = false;
		}
		try {
			// It is possible that the file was a deletion that was recreated so
			// make sure it is removed from the deletion list.
			handleDeletion(file, false /* add to list */);
			internalSetModified(file, dirty);
		} catch (CVSException e) {
			throw new CoreException(e.getStatus());
		}
	}
	
	public void flush(IProject project, int depth) throws CoreException {
		project.accept(new IResourceVisitor() {
			public boolean visit(IResource resource) throws CoreException {
				if (resource.getType() == IResource.FILE) {
					flush((IFile)resource);
				} else {
					flush((IContainer)resource);
				}
				return true;
			}
		}, depth, true);
	}
	
}

