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
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.ISavedState;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.IResourceStateChangeListener;
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
	
	private static final String IS_DIRTY_INDICATOR = new String();
	private static final Object NOT_DIRTY_INDICATOR = new Object();
	private static final QualifiedName IS_DIRTY = new QualifiedName(CVSProviderPlugin.ID, "is-dirty");
	
	/**
	 * Listen for file modifications and fire modification state changes
	 * 
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			final List changedResources = new ArrayList();
			event.getDelta().accept(new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta delta) throws CoreException {
					IResource resource = delta.getResource();

					if(resource.getType()==IResource.ROOT) {
						// continue with the delta
						return true;
					}

					if (resource.getType()==IResource.FILE && delta.getKind() == IResourceDelta.CHANGED) {
						changedResources.add(resource);
					}

					return true;
				}
			});
			if (!changedResources.isEmpty()) {
				CVSProviderPlugin.broadcastModificationStateChanges(
					(IResource[])changedResources.toArray(new IResource[changedResources.size()]),
					IResourceStateChangeListener.CONTENTS_MODIFIED);
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

	private boolean isModified(ICVSFile cvsFile, ResourceSyncInfo info) throws CVSException {
		if (info == null) return false;
		if(info.isMerged() || !cvsFile.exists()) return true;
		return !cvsFile.getTimeStamp().equals(info.getTimeStamp());
	}
	
	/**
	 * Method setModified.
	 * @param iFile
	 * @param b
	 */
	public void setModified(IFile file, boolean modified) {
		// ensureDirtyInfoCached(file);
		if (modified) {
			//makeDirty(file);
		} else {
			//resetDirty(file);
		}
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
			resource.setSessionProperty(IS_DIRTY, NOT_DIRTY_INDICATOR);
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}
	
	public void contentsChanged(IFile file) throws CoreException {
		Object indicator = file.getSessionProperty(IS_DIRTY);
		boolean dirty = true;
		if (indicator == NOT_DIRTY_INDICATOR) {
			// the file was changed die to an update so skip it
			file.setSessionProperty(IS_DIRTY, null);
			dirty = false;
		}
		setModified(file, dirty);
	}
	
}

