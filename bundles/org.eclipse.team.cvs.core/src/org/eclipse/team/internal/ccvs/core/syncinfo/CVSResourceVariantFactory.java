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
import org.eclipse.team.core.synchronize.IResourceVariant;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.RemoteResource;
import org.eclipse.team.internal.core.subscribers.caches.IResourceVariantFactory;
import org.eclipse.team.internal.core.subscribers.caches.ResourceVariantTree;

public class CVSResourceVariantFactory implements IResourceVariantFactory {
	private CVSTag tag;
	private boolean cacheFileContentsHint;
	private ResourceVariantTree baseCache;
	
	public CVSResourceVariantFactory(ResourceVariantTree baseCache, CVSTag tag, boolean cacheFileContentsHint) {
		this.baseCache = baseCache;
		this.tag = tag;
		this.cacheFileContentsHint = cacheFileContentsHint;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.core.subscribers.caches.IResourceVariantFactory#fetchVariant(org.eclipse.core.resources.IResource, int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IResourceVariant fetchVariant(IResource resource, int depth, IProgressMonitor monitor) throws TeamException {
		// TODO: we are currently ignoring the depth parameter because the build remote tree is
		// by default deep!
		return (IResourceVariant)CVSWorkspaceRoot.getRemoteTree(resource, tag, cacheFileContentsHint, monitor);

	}
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.core.subscribers.caches.IResourceVariantFactory#fetchMembers(org.eclipse.team.core.synchronize.IResourceVariant, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IResourceVariant[] fetchMembers(IResourceVariant variant, IProgressMonitor progress) throws TeamException {
		ICVSRemoteResource[] children = variant != null ? (ICVSRemoteResource[])((RemoteResource)variant).members(progress) : new ICVSRemoteResource[0];
		IResourceVariant[] result = new IResourceVariant[children.length];
		for (int i = 0; i < children.length; i++) {
			result[i] = (IResourceVariant)children[i];
		}
		return result;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.core.subscribers.caches.IResourceVariantFactory#getBytes(org.eclipse.core.resources.IResource, org.eclipse.team.core.synchronize.IResourceVariant)
	 */
	public byte[] getBytes(IResource resource, IResourceVariant variant) throws TeamException {
		if (variant != null) {
			return ((RemoteResource)variant).getSyncBytes();
		} else {
			if (resource.getType() == IResource.FOLDER && baseCache != null) {
				// If there is no remote, use the local sync for the folder
				return baseCache.getBytes(resource);
			}
			return null;
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.core.subscribers.caches.IResourceVariantFactory#getResourceVariant(org.eclipse.core.resources.IResource, org.eclipse.team.internal.core.subscribers.caches.ResourceVariantTree)
	 */
	public IResourceVariant getResourceVariant(IResource resource, ResourceVariantTree tree) throws TeamException {
		// TODO Auto-generated method stub
		return null;
	}
}
