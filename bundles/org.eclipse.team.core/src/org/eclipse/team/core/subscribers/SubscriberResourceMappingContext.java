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
package org.eclipse.team.core.subscribers;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.core.TeamPlugin;

/**
 * A traversal context that uses the remote state of a subscriber.
 * It does not refresh it's state.
 * @since 3.1
 */
public class SubscriberResourceMappingContext extends ResourceMappingContext {
    
    Subscriber subscriber;

    /**
     * Create a resource mappin context for the given subscriber
     * @param subscriber the subscriber
     */
    public SubscriberResourceMappingContext(Subscriber subscriber) {
        this.subscriber = subscriber;
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.resources.mapping.ITraversalContext#contentDiffers(org.eclipse.core.resources.IFile, org.eclipse.core.runtime.IProgressMonitor)
     */
    public boolean contentDiffers(IFile file, IProgressMonitor monitor) throws CoreException {
        SyncInfo syncInfo = subscriber.getSyncInfo(file);
        return syncInfo != null && syncInfo.getKind() != SyncInfo.IN_SYNC;
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.resources.mapping.ITraversalContext#fetchContents(org.eclipse.core.resources.IFile, org.eclipse.core.runtime.IProgressMonitor)
     */
    public IStorage fetchContents(IFile file, IProgressMonitor monitor) throws CoreException {
        SyncInfo syncInfo = subscriber.getSyncInfo(file);
        if (syncInfo == null) {
            throw new CoreException(new Status(IStatus.ERROR, TeamPlugin.ID, IResourceStatus.RESOURCE_NOT_FOUND, "File {0} does not have a corresponding remote" + file.getFullPath().toString(), null));
        }
        return syncInfo.getRemote().getStorage(monitor);
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.resources.mapping.ITraversalContext#fetchMembers(org.eclipse.core.resources.IContainer, org.eclipse.core.runtime.IProgressMonitor)
     */
    public IResource[] fetchMembers(IContainer container, IProgressMonitor monitor) throws CoreException {
        return subscriber.members(container);
    }

    public void refresh(ResourceTraversal[] traversals, IProgressMonitor monitor) throws CoreException {
        // TODO For now, refresh for each traversal.
        for (int i = 0; i < traversals.length; i++) {
            ResourceTraversal traversal = traversals[i];
            subscriber.refresh(traversal.getResources(), traversal.getDepth(), monitor);
        }
    }

}
