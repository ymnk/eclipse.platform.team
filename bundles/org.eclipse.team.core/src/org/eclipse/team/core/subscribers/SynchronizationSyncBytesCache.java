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
package org.eclipse.team.core.subscribers;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.core.Assert;

/**
 * This class provides a concrete synchronization cache that uses the
 * <code>org.eclipse.core.resources.ISynchronizer</code>.
 */
public class SynchronizationSyncBytesCache extends SynchronizationCache {

	private static final byte[] NO_REMOTE = new byte[0];
	
	protected QualifiedName syncName;
	
	public SynchronizationSyncBytesCache(QualifiedName name) {
		syncName = name;
		getSynchronizer().add(syncName);
	}
	
	public void dispose() {
		getSynchronizer().remove(getSyncName());
	}

	protected ISynchronizer getSynchronizer() {
		return ResourcesPlugin.getWorkspace().getSynchronizer();
	}

	public QualifiedName getSyncName() {
		return syncName;
	}

	public byte[] getSyncBytes(IResource resource) throws TeamException {
		byte[] syncBytes = internalGetSyncBytes(resource);
		if (syncBytes != null && equals(syncBytes, NO_REMOTE)) {
			// If it is known that there is no remote, return null
			return null;
		}
		return syncBytes;
	}

	private byte[] internalGetSyncBytes(IResource resource) throws TeamException {
		try {
			return getSynchronizer().getSyncInfo(getSyncName(), resource);
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}
	}
	
	public boolean setSyncBytes(IResource resource, byte[] bytes) throws TeamException {
		Assert.isNotNull(bytes);
		byte[] oldBytes = internalGetSyncBytes(resource);
		if (oldBytes != null && equals(oldBytes, bytes)) return false;
		try {
			getSynchronizer().setSyncInfo(getSyncName(), resource, bytes);
			return true;
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}
	}

	public boolean removeSyncBytes(IResource resource, int depth) throws TeamException {
		if (resource.exists() || resource.isPhantom()) {
			try {
				if (depth != IResource.DEPTH_ZERO || internalGetSyncBytes(resource) != null) {
					getSynchronizer().flushSyncInfo(getSyncName(), resource, depth);
					return true;
				}
			} catch (CoreException e) {
				throw TeamException.asTeamException(e);
			}
		}
		return false;
	}
	
	public boolean isRemoteKnown(IResource resource) throws TeamException {
		return internalGetSyncBytes(resource) != null;
	}
	
	/**
	 * This method should be invoked by a client to indicate that it is known that 
	 * there is no remote resource associated with the local resource. After this method
	 * is invoked, <code>isRemoteKnown(resource)</code> will return <code>true</code> and
	 * <code>getSyncBytes(resource)</code> will return <code>null</code>.
	 * @return <code>true</code> if this changes the remote sync bytes
	 */
	public boolean setRemoteDoesNotExist(IResource resource) throws TeamException {
		return setSyncBytes(resource, NO_REMOTE);
	}

}
