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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.helpers.RefreshOperation;
import org.eclipse.team.core.subscribers.helpers.SynchronizationCache;
import org.eclipse.team.core.sync.IRemoteResource;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.RemoteResource;

/**
 * CVS Specific refresh operation
 */
public class CVSRefreshOperation extends RefreshOperation {

	private SynchronizationCache cache, baseCache;
	private CVSTag tag;

	public CVSRefreshOperation(SynchronizationCache cache, SynchronizationCache baseCache, CVSTag tag) {
		this.tag = tag;
		this.cache = cache;
		this.baseCache = cache;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.RefreshOperation#getSynchronizationCache()
	 */
	protected SynchronizationCache getSynchronizationCache() {
		return cache;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.RefreshOperation#getRemoteSyncBytes(org.eclipse.core.resources.IResource, org.eclipse.team.core.subscribers.ISubscriberResource)
	 */
	protected byte[] getRemoteSyncBytes(IResource local, IRemoteResource remote) throws TeamException {
		if (remote != null) {
			return ((RemoteResource)remote).getSyncBytes();
		} else {
			if (local.getType() == IResource.FOLDER && baseCache != null) {
				// If there is no remote, use the local sync for the folder
				return baseCache.getSyncBytes(local);
			}
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.RefreshOperation#getRemoteChildren(org.eclipse.team.core.subscribers.ISubscriberResource, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IRemoteResource[] getRemoteChildren(IRemoteResource remote, IProgressMonitor progress) throws TeamException {
		ICVSRemoteResource[] children = remote != null ? (ICVSRemoteResource[])((RemoteResource)remote).members(progress) : new ICVSRemoteResource[0];
		IRemoteResource[] result = new IRemoteResource[children.length];
		for (int i = 0; i < children.length; i++) {
			result[i] = (IRemoteResource)children[i];
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.RefreshOperation#getLocalChildren(org.eclipse.core.resources.IResource)
	 */
	protected IResource[] getLocalChildren(IResource local) throws TeamException {
		IResource[] localChildren = null;			
		if( local.getType() != IResource.FILE && (local.exists() || local.isPhantom())) {
			// Include all non-ignored resources including outgoing deletions
			ICVSFolder cvsFolder = CVSWorkspaceRoot.getCVSFolderFor((IContainer)local);
			// Look inside existing folders and phantoms that are CVS folders
			if (local.exists() || cvsFolder.isCVSFolder()) {
				ICVSResource[] cvsChildren = cvsFolder.members(ICVSFolder.MANAGED_MEMBERS | ICVSFolder.UNMANAGED_MEMBERS);
				List resourceChildren = new ArrayList();
				for (int i = 0; i < cvsChildren.length; i++) {
					ICVSResource cvsResource = cvsChildren[i];
					resourceChildren.add(cvsResource.getIResource());
				}
				localChildren = (IResource[]) resourceChildren.toArray(new IResource[resourceChildren.size()]);
			}
		}
		if (localChildren == null) {
			localChildren = new IResource[0];
		}
		return localChildren;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.RefreshOperation#buildRemoteTree(org.eclipse.core.resources.IResource, int, boolean, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IRemoteResource buildRemoteTree(IResource resource, int depth, boolean cacheFileContentsHint, IProgressMonitor monitor) throws TeamException {
		// TODO: we are currently ignoring the depth parameter because the build remote tree is
		// by default deep!
		return CVSWorkspaceRoot.getRemoteTree(resource, tag, cacheFileContentsHint, monitor);
	}

}
