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
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.helpers.DescendantSynchronizationCache;
import org.eclipse.team.core.subscribers.helpers.SynchronizationCache;
import org.eclipse.team.internal.ccvs.core.CVSException;

/**
 * CVS sycnrhonization cache that ignores stale remote bytes
 */
public class CVSDescendantSynchronizationCache extends DescendantSynchronizationCache {

	public CVSDescendantSynchronizationCache(SynchronizationCache baseCache, SynchronizationCache remoteCache) {
		super(baseCache, remoteCache);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.DescendantSynchronizationCache#isDescendant(org.eclipse.core.resources.IResource, byte[], byte[])
	 */
	protected boolean isDescendant(IResource resource, byte[] baseBytes, byte[] remoteBytes) throws TeamException {
		if (resource.getType() != IResource.FILE) return true;
		try {
			return ResourceSyncInfo.isLaterRevisionOnSameBranch(remoteBytes, baseBytes);
		} catch (CVSException e) {
			throw TeamException.asTeamException(e);
		}
	}

}
