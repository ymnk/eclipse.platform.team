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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;

/**
 * This class listens for file modifications and broadcasts them to all
 * listeners
 */
public class FileModificationListener implements IResourceChangeListener {
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

					if (delta.getKind() == IResourceDelta.CHANGED) {
						changedResources.add(resource);
					}

					return true;
				}
			});
			CVSProviderPlugin.broadcastModificationStateChanges(
				(IResource[])changedResources.toArray(new IResource[changedResources.size()]));
		} catch (CoreException e) {
			CVSProviderPlugin.log(e.getStatus());
		}

	}

}
