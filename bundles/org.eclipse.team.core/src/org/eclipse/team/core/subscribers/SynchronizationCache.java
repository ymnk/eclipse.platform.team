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

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.TeamException;

/**
 * This class defines the API for a sync bytes cache. Synchronization
 * bytes are associated with a local resource handle.
 * It also has API that differentiates the case of no existing remote for
 * a local resource from that of the remote state never having been queried
 * for that local resource.
 */
public abstract class SynchronizationCache {

	/**
	 * Dispose of any cached sync bytes when this cache is no longer needed.
	 */
	public abstract void dispose();
	
	/**
	 * Return the remote sync bytes cached for the given local resource.
	 * A return value of <code>null</code> can mean either that the
	 * remote has never been fetched or that it doesn't exist. The method
	 * <code>isRemoteKnown(IResource)</code> should be used to differentiate
	 * these two cases.
	 * @param resource the local resource
	 * @return the synchronization bytes for a corresponding remote resource
	 * @throws TeamException
	 */
	public abstract byte[] getSyncBytes(IResource resource) throws TeamException;
	
	/**
	 * Set the remote sync bytes for the given resource. The bytes should never be
	 * <code>null</code>. If it is known that the remote does not exist, 
	 * <code>setRemoteDoesNotExist(IResource)</code> should be invoked. If the sync
	 * bytes for the remote are stale and should be removed, <code>removeSyncBytes()</code>
	 * should be called.
	 * @param resource the local resource
	 * @param bytes the synchronization bytes for a corresponding remote resource
	 * @return <code>true</code> if the sync bytes changed
	 * @throws TeamException
	 */
	public abstract boolean setSyncBytes(IResource resource, byte[] bytes) throws TeamException;
	
	/**
	 * Remove the remote bytes cached for the given local resource. After this
	 * operation <code>isRemoteKnown(resource)</code> will return <code>false</code> 
	 * and <code>getSyncBytes(resource)</code> will return <code>null</code> for the
	 * resource (and potentially it's children depending on the value of the depth parameter.
	 * @param resource the local resource
	 * @parem depth the depth of the operation (@see IResource)
	 * @return <code>true</code> if there were bytes present which were removed
	 * @throws TeamException
	 */
	public abstract boolean removeSyncBytes(IResource resource, int depth) throws TeamException;
	
	/**
	 * Return true if the remote resources associated with the given local 
	 * resource has been fetched. This method is useful for those cases when
	 * there are no sync bytes for a remote resource and the client wants to
	 * know if this means that the remote does exist (i.e. this method returns
	 * <code>true</code>) or the remote has not been fetched (i.e. this method returns
	 * <code>false</code>).
	 * @param resource the local resource
	 * @throws TeamException
	 */
	public abstract boolean isRemoteKnown(IResource resource) throws TeamException;
	
	/**
	 * This method should be invoked by a client to indicate that it is known that 
	 * there is no remote resource associated with the local resource. After this method
	 * is invoked, <code>isRemoteKnown(resource)</code> will return <code>true</code> and
	 * <code>getSyncBytes(resource)</code> will return <code>null</code>.
	 * @param resource the local resource
	 * @return <code>true</code> if this changes the remote sync bytes
	 */
	public abstract boolean setRemoteDoesNotExist(IResource resource) throws TeamException;
	
	/**
	 * Helper method to compare two byte arrays for equality
	 * @param syncBytes1 the first byte array
	 * @param syncBytes2 the second byte array
	 * @return whetehr the two arrays are equal (i.e. same content)
	 */
	protected boolean equals(byte[] syncBytes1, byte[] syncBytes2) {
		if (syncBytes1 == null) {
			return syncBytes2 == null;
		} else if (syncBytes2 == null) {
			return false;
		}
		if (syncBytes1.length != syncBytes2.length) return false;
		for (int i = 0; i < syncBytes1.length; i++) {
			if (syncBytes1[i] != syncBytes2[i]) return false;
		}
		return true;
	}
}
