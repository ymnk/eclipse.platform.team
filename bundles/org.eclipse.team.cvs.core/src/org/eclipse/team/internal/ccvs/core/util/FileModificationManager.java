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
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.IResourceStateChangeListener;

/**
 * This class listens for file modifications and broadcasts them to all
 * listeners
 */
public class FileModificationManager implements IResourceChangeListener, ISaveParticipant {
	/**
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

}

