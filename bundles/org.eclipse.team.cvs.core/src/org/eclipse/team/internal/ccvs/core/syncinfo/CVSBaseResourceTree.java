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
import org.eclipse.team.core.subscribers.ISubscriberResource;
import org.eclipse.team.core.subscribers.SyncBytesSubscriberResourceTree;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;

/**
 * A base sychronizer provides access to the base sync bytes for the 
 * resources in the local workspace
 */
public class CVSBaseResourceTree extends SyncBytesSubscriberResourceTree {

	/**
	 * Create a synchronizer that uses the CVS local workspace synchronization information
	 */
	public CVSBaseResourceTree() {
		super(new CVSBaseSynchronizationCache());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSynchronizer#refresh(org.eclipse.core.resources.IResource[], int, boolean, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IResource[] refresh(
		IResource[] resources,
		int depth,
		boolean cacheFileContentsHint,
		IProgressMonitor monitor)
		throws TeamException {
			
		// TODO Ensure that file contents are cached for modified local files
		try {
			monitor.beginTask(null, 100);
			return new IResource[0];
		} finally {
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSynchronizer#getRemoteResource(org.eclipse.core.resources.IResource)
	 */
	public ISubscriberResource getRemoteResource(IResource resource) throws TeamException {
		return (ISubscriberResource)CVSWorkspaceRoot.getRemoteResourceFor(resource);
	}

}
