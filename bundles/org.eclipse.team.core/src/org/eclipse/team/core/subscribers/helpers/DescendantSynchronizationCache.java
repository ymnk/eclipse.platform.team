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
package org.eclipse.team.core.subscribers.helpers;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.TeamException;

/**
 * A descendant sychronization cache stores the remote synchronization bytes.
 * It does so in an optimal way by storing the bytes only for those resources
 * whose remote differ from the base bytes stored in the base synchronization cache.
 */
public abstract class DescendantSynchronizationCache extends SynchronizationCache {
	SynchronizationCache baseCache, remoteCache;

	public DescendantSynchronizationCache(SynchronizationCache baseCache, SynchronizationCache remoteCache) {
		this.baseCache = baseCache;
		this.remoteCache = remoteCache;
	}
	
	/**
	 * This method will dispose the remote cache but not the base cache.
	 * 
	 * @see org.eclipse.team.core.subscribers.helpers.SynchronizationCache#dispose()
	 */
	public void dispose() {
		remoteCache.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.AbstractSynchronizationCache#getSyncBytes(org.eclipse.core.resources.IResource)
	 */
	public byte[] getSyncBytes(IResource resource) throws TeamException {
		byte[] remoteBytes = remoteCache.getSyncBytes(resource);
		byte[] baseBytes = baseCache.getSyncBytes(resource);
		if (baseBytes == null) {
			// There is no base so use the remote bytes
			return remoteBytes;
		}
		if (remoteBytes == null) {
			if (isRemoteKnown(resource)) {
				// The remote is known to not exist
				// TODO: The check for NO_REMOTE does not take into consideration the line-of-descent
				return remoteBytes;
			} else {
				// The remote was either never queried or was the same as the base.
				// In either of these cases, the base bytes are used.
				return baseBytes;
			}
		}
		if (isDescendant(resource, baseBytes, remoteBytes)) {
			// Only use the remote bytes if they are later on the same line-of-descent as the base
			return remoteBytes;
		}
		// Use the base sbytes since the remote bytes must be stale (i.e. are
		// not on the same line-of-descent
		return baseBytes;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.AbstractSynchronizationCache#setSyncBytes(org.eclipse.core.resources.IResource, byte[])
	 */
	public boolean setSyncBytes(IResource resource, byte[] bytes) throws TeamException {
		byte[] baseBytes = baseCache.getSyncBytes(resource);
		if (baseBytes != null && equals(baseBytes, bytes)) {
			// Remove the existing bytes so the base will be used (thus saving space)
			return remoteCache.removeSyncBytes(resource, IResource.DEPTH_ZERO);
		} else {
			return remoteCache.setSyncBytes(resource, bytes);
		}	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.AbstractSynchronizationCache#removeSyncBytes(org.eclipse.core.resources.IResource, int)
	 */
	public boolean removeSyncBytes(IResource resource, int depth) throws TeamException {
		return remoteCache.removeSyncBytes(resource, depth);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.AbstractSynchronizationCache#isRemoteKnown(org.eclipse.core.resources.IResource)
	 */
	public boolean isRemoteKnown(IResource resource) throws TeamException {
		return remoteCache.isRemoteKnown(resource);
	}

	/**
	 * This method indicates whether the remote bytes are a later revision or version
	 * on the same line-of-descent as the base. A line of descent may be a branch or a fork
	 * (depending on the terminology used by the versioing server).
	 * @param resource the local resource
	 * @param baseBytes the base bytes for the local resoource
	 * @param remoteBytes the remote bytes for the local resoource
	 * @return whether the remote bytes are later on the same line-of-descent as the base bytes
	 */
	protected abstract boolean isDescendant(IResource resource, byte[] baseBytes, byte[] remoteBytes) throws TeamException;
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.AbstractSynchronizationCache#setRemoteDoesNotExist(org.eclipse.core.resources.IResource)
	 */
	public boolean setRemoteDoesNotExist(IResource resource) throws TeamException {
		return remoteCache.setRemoteDoesNotExist(resource);
	}

}
