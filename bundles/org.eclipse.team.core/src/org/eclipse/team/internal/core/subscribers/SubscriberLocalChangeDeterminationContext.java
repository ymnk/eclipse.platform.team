/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core.subscribers;

import org.eclipse.core.internal.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.internal.resources.mapping.ResourceTraversal;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoFilter;
import org.eclipse.team.core.variants.CachedResourceVariant;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.core.Policy;
import org.eclipse.team.internal.core.TeamPlugin;

/**
 * A change determination context for comparing the local resource state against the base state.
 */
public class SubscriberLocalChangeDeterminationContext extends RemoteResourceMappingContext {
    
    private final SyncInfoFilter contentDiffFilter = new SyncInfoFilter() {
        public boolean select(SyncInfo info, IProgressMonitor monitor) {
            if (info != null) {
                int direction = info.getKind() & SyncInfo.DIRECTION_MASK;
                // When committing, only outgoing and conflicting changes are needed
                return direction == SyncInfo.OUTGOING || direction == SyncInfo.CONFLICTING ;
            }
            return false;
        }
    };
    private final Subscriber subscriber;
    private final boolean canContactServer;
    
    /**
     * Create a change determination context that delegates to the given subscriber context
     * @param filter 
     */
    public SubscriberLocalChangeDeterminationContext(Subscriber subscriber, boolean canContactServer) {
        this.subscriber = subscriber;
        this.canContactServer = canContactServer;
    }
    
    public boolean canContactServer() {
        return canContactServer;
    }

    public boolean hasCachedContentFor(IFile file) {
        try {
            IResourceVariant base = getBase(file);
            if (base == null) return true;
            if (base instanceof CachedResourceVariant) {
                CachedResourceVariant cached = (CachedResourceVariant) base;
                return cached.isContentsCached();
            }
        } catch (TeamException e) {
            TeamPlugin.log(e);
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.internal.resources.mapping.RemoteResourceMappingContext#contentDiffers(org.eclipse.core.resources.IFile, org.eclipse.core.runtime.IProgressMonitor)
     */
    public boolean contentDiffers(IFile file, IProgressMonitor monitor) throws CoreException {
        SyncInfo syncInfo = subscriber.getSyncInfo(file);
        return syncInfo != null && contentDiffFilter.select(syncInfo, Policy.subMonitorFor(monitor, 90));
    }

    public IStorage fetchContents(IFile file, IProgressMonitor monitor) throws CoreException {
        if (canContactServer() || hasCachedContentFor(file)) {
            IResourceVariant base = getBase(file);
            if (base != null)
                return base.getStorage(monitor);
            return null;
        }
        throw new CoreException(new Status(IStatus.ERROR, TeamPlugin.ID, SERVER_CONTACT_PROHIBITED, NLS.bind("The contents for file {0} are not available.", new String[] { file.getFullPath().toString() }), null));
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.internal.resources.mapping.RemoteResourceMappingContext#fetchMembers(org.eclipse.core.resources.IContainer, org.eclipse.core.runtime.IProgressMonitor)
     */
    public IResource[] fetchMembers(IContainer container, IProgressMonitor monitor) throws TeamException {
        return subscriber.members(container);
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.internal.resources.mapping.RemoteResourceMappingContext#refresh(org.eclipse.core.internal.resources.mapping.ResourceTraversal[], int, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void refresh(ResourceTraversal[] traversals, int flags, IProgressMonitor monitor) throws CoreException {
        // No need to refresh as the base is known but we should bulk fetch any requested file contents
        // TODO:
    }
    
    private IResourceVariant getBase(IFile file) throws TeamException {
        SyncInfo syncInfo = subscriber.getSyncInfo(file);
        if (syncInfo != null) {
            return syncInfo.getBase(); 
        }
        return null;
    }

}
