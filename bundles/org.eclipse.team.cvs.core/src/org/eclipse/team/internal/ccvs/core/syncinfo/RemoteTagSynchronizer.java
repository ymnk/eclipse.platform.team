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

import org.eclipse.core.internal.jobs.JobManager;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;

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
		IResource[] changedResources = null;
		monitor.beginTask(null, 100 * resources.length);
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			ISchedulingRule rule = resource.getProject();
			
			try {
				// Get a scheduling rule on the project since CVS may obtain a lock higher then
				// the resource itself.
				JobManager.getInstance().beginRule(rule, monitor);
				if (!resource.getProject().isAccessible()) {
					// The project is closed so silently skip it
					return new IResource[0];
				}
				
				monitor.setTaskName(Policy.bind("RemoteTagSynchronizer.0", resource.getFullPath().makeRelative().toString())); //$NON-NLS-1$
				
				// build the remote tree only if an initial tree hasn't been provided
				ICVSRemoteResource	tree = buildRemoteTree(resource, depth, cacheFileContentsHint, Policy.subMonitorFor(monitor, 70));
				
				// update the known remote handles 
				IProgressMonitor sub = Policy.infiniteSubMonitorFor(monitor, 30);
				try {
					sub.beginTask(null, 64);
					// TODO: API shoudl include refresh itself
					changedResources = new CVSRefreshOperation(getSynchronizationCache(), baseCache).collectChanges(resource, (ISubscriberResource)tree, depth, sub);
				} finally {
					sub.done();	 
				}
			} finally {
				JobManager.getInstance().endRule(rule);
			}
		}
		monitor.done();
		if (changedResources == null) return new IResource[0];
		return changedResources;
	}

	/**
	 * Build a remote tree for the given parameters.
	 */
	protected ICVSRemoteResource buildRemoteTree(IResource resource, int depth, boolean cacheFileContentsHint, IProgressMonitor monitor) throws TeamException {
		// TODO: we are currently ignoring the depth parameter because the build remote tree is
		// by default deep!
		return CVSWorkspaceRoot.getRemoteTree(resource, tag, cacheFileContentsHint, monitor);
	}

}
