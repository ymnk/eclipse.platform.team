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
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.internal.ccvs.core.CVSTag;

/**
 * This RemoteSynchronizr uses a CVS Tag to fetch the remote tree
 */
public class RemoteTagSynchronizer extends CVSRemoteSynchronizer {

	private CVSTag tag;
	private SynchronizationCache baseCache;
	
	public RemoteTagSynchronizer(SynchronizationCache cache, CVSTag tag) {
		super(cache);
		this.tag = tag;
	}

	public RemoteTagSynchronizer(String id, CVSTag tag) {
		this(
			new SynchronizationSyncBytesCache(new QualifiedName(CVSRemoteSynchronizer.SYNC_KEY_QUALIFIER, id)),
			tag);
	}

	public RemoteTagSynchronizer(SynchronizationCache baseCache, SynchronizationCache cache, CVSTag tag) {
		this(new DescendantSynchronizationCache(baseCache, cache), tag);
		this.baseCache = baseCache;
	}

	public IResource[] refresh(IResource[] resources, int depth, boolean cacheFileContentsHint, IProgressMonitor monitor) throws TeamException {
		return new CVSRefreshOperation(getSynchronizationCache(), baseCache, tag).refresh(resources, depth, cacheFileContentsHint, monitor);
	}

}
