/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core.syncinfo;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.helpers.TeamSubscriberRefreshOperation;
import org.eclipse.team.core.subscribers.helpers.SynchronizationCache;

/**
 * A remote bytes sychronizer is a remote synchronizer that caches the 
 * remote sync bytes using a subclass of <code>AbstractSynchronizationCache</code>.
 * It also has API that differentiates the case of no existing remote for
 * a local resource from that of the remote state never having been queried
 * for that local resource.
 */
public abstract class SyncBytesSubscriberResourceTree extends SubscriberResourceTree {
	
	private SynchronizationCache synchronizationCache;
	
	public SyncBytesSubscriberResourceTree(SynchronizationCache synchronizationCache) {
		this.synchronizationCache = synchronizationCache;
	}
	
	/**
	 * Provide access to the synchronization cache for any subclasses.
	 * @return Returns the synchronization cache.
	 */
	public SynchronizationCache getSynchronizationCache() {
		return synchronizationCache;
	}
	/**
	 * Dispose of any cached sync bytes.
	 */
	public void dispose() {
		synchronizationCache.dispose();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.RemoteSynchronizer#hasRemote(org.eclipse.core.resources.IResource)
	 */
	public boolean hasRemote(IResource resource) throws TeamException {
		return getSynchronizationCache().getSyncBytes(resource) != null;
	}

	public byte[] getSyncBytes(IResource resource) throws TeamException {
		return getSynchronizationCache().getSyncBytes(resource);
	}
	
	public boolean setSyncBytes(IResource resource, byte[] bytes) throws TeamException {
		return getSynchronizationCache().setSyncBytes(resource, bytes);
	}

	public boolean removeSyncBytes(IResource resource, int depth) throws TeamException {
		return getSynchronizationCache().removeSyncBytes(resource, depth);
	}
	
	public boolean isRemoteKnown(IResource resource) throws TeamException {
		return getSynchronizationCache().isRemoteKnown(resource);
	}
	
	public IResource[] refresh(IResource[] resources, int depth, boolean cacheFileContentsHint, IProgressMonitor monitor) throws TeamException {
		return getRefreshOperation().refresh(resources, depth, cacheFileContentsHint, monitor);
	}

	/**
	 * @return
	 */
	protected abstract TeamSubscriberRefreshOperation getRefreshOperation();
}