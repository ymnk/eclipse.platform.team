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
package org.eclipse.team.internal.core.subscribers.caches;

import java.util.*;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.ISubscriberResource;
import org.eclipse.team.internal.core.Assert;
import org.eclipse.team.internal.core.Policy;

/**
 * This abstract superclass provides the logic for refreshing the synchronization bytes for
 * remote resources that correspond to local resources. This class provides the logic to traverse
 * the local and remote resource trees in order to cache the remote synchronization bytes in
 * a <code>SynchronizationCache</code>. It also accumulates and returns all local resources 
 * for which the corresponding remote has changed.
 */
public abstract class SynchronizationCacheRefreshOperation {
	
	protected abstract SynchronizationCache getSynchronizationCache();
	
	public IResource[] refresh(IResource[] resources, int depth, boolean cacheFileContentsHint, IProgressMonitor monitor) throws TeamException {
		List changedResources = new ArrayList();
		monitor.beginTask(null, 100 * resources.length);
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			IResource[] changed = refresh(resource, depth, cacheFileContentsHint, Policy.subMonitorFor(monitor, 100));
			changedResources.addAll(Arrays.asList(changed));
		}
		monitor.done();
		if (changedResources == null) return new IResource[0];
		return (IResource[]) changedResources.toArray(new IResource[changedResources.size()]);
	}

	protected IResource[] refresh(IResource resource, int depth, boolean cacheFileContentsHint, IProgressMonitor monitor) throws TeamException {
		IResource[] changedResources = null;
		monitor.beginTask(null, 100);
		ISchedulingRule rule = resource.getProject();
		try {
			// Get a scheduling rule on the project since CVS may obtain a lock higher then
			// the resource itself.
			Platform.getJobManager().beginRule(rule, monitor);
			if (!resource.getProject().isAccessible()) {
				// The project is closed so silently skip it
				return new IResource[0];
			}
			
			monitor.setTaskName(Policy.bind("SynchronizationCacheRefreshOperation.0", resource.getFullPath().makeRelative().toString())); //$NON-NLS-1$
			
			// build the remote tree only if an initial tree hasn't been provided
			ISubscriberResource	tree = getRemoteTree(resource, depth, cacheFileContentsHint, Policy.subMonitorFor(monitor, 70));
			
			// update the known remote handles 
			IProgressMonitor sub = Policy.infiniteSubMonitorFor(monitor, 30);
			try {
				sub.beginTask(null, 64);
				changedResources = collectChanges(resource, tree, depth, sub);
			} finally {
				sub.done();	 
			}
		} finally {
			Platform.getJobManager().endRule(rule);
			monitor.done();
		}
		if (changedResources == null) return new IResource[0];
		return changedResources;
	}
	
	public IResource[] collectChanges(IResource local, ISubscriberResource remote, int depth, IProgressMonitor monitor) throws TeamException {
		List changedResources = new ArrayList();
		collectChanges(local, remote, changedResources, depth, monitor);
		return (IResource[]) changedResources.toArray(new IResource[changedResources.size()]);
	}
	
	protected void collectChanges(IResource local, ISubscriberResource remote, Collection changedResources, int depth, IProgressMonitor monitor) throws TeamException {
		SynchronizationCache cache = getSynchronizationCache();
		byte[] newRemoteBytes = getRemoteSyncBytes(local, remote);
		boolean changed;
		if (newRemoteBytes == null) {
			changed = cache.setRemoteDoesNotExist(local);
		} else {
			changed = cache.setSyncBytes(local, newRemoteBytes);
		}
		if (changed) {
			changedResources.add(local);
		}
		if (depth == IResource.DEPTH_ZERO) return;
		Map children = mergedMembers(local, remote, monitor);	
		for (Iterator it = children.keySet().iterator(); it.hasNext();) {
			IResource localChild = (IResource) it.next();
			ISubscriberResource remoteChild = (ISubscriberResource)children.get(localChild);
			collectChanges(localChild, remoteChild, changedResources,
					depth == IResource.DEPTH_INFINITE ? IResource.DEPTH_INFINITE : IResource.DEPTH_ZERO, 
					monitor);
		}
		
		// Look for resources that have sync bytes but are not in the resources we care about
		IResource[] resources = getChildrenWithSyncBytes(local);
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			if (!children.containsKey(resource)) {
				// These sync bytes are stale. Purge them
				cache.removeSyncBytes(resource, IResource.DEPTH_INFINITE);
				changedResources.add(resource);
			}
		}
	}

	/**
	 * Return all the children of the local resource, including phantoms, that have synchronozation bytes 
	 * associated with them in the synchronization cache of this operation. 
	 * @param local the local resource
	 * @return all children that have sychronization bytes.
	 * @throws TeamException
	 */
	protected IResource[] getChildrenWithSyncBytes(IResource local) throws TeamException {			
		try {
			if (local.getType() != IResource.FILE && (local.exists() || local.isPhantom())) {
				IResource[] allChildren = ((IContainer)local).members(true /* include phantoms */);
				List childrenWithSyncBytes = new ArrayList();
				for (int i = 0; i < allChildren.length; i++) {
					IResource resource = allChildren[i];
					if (getSynchronizationCache().getSyncBytes(resource) != null) {
						childrenWithSyncBytes.add(resource);
					}
				}
				return (IResource[]) childrenWithSyncBytes.toArray(
						new IResource[childrenWithSyncBytes.size()]);
			}
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}
		return new IResource[0];
	}
	
	protected Map mergedMembers(IResource local, ISubscriberResource remote, IProgressMonitor progress) throws TeamException {
		
		// {IResource -> IRemoteResource}
		Map mergedResources = new HashMap();
		
		ISubscriberResource[] remoteChildren;
		if (remote == null) {
			remoteChildren = new ISubscriberResource[0];
		} else {
			remoteChildren = getRemoteChildren(remote, progress);
		}
		
		
		IResource[] localChildren = getLocalChildren(local);		

		if (remoteChildren.length > 0 || localChildren.length > 0) {
			Set allSet = new HashSet(20);
			Map localSet = null;
			Map remoteSet = null;

			if (localChildren.length > 0) {
				localSet = new HashMap(10);
				for (int i = 0; i < localChildren.length; i++) {
					IResource localChild = localChildren[i];
					String name = localChild.getName();
					localSet.put(name, localChild);
					allSet.add(name);
				}
			}

			if (remoteChildren.length > 0) {
				remoteSet = new HashMap(10);
				for (int i = 0; i < remoteChildren.length; i++) {
					ISubscriberResource remoteChild = remoteChildren[i];
					String name = remoteChild.getName();
					remoteSet.put(name, remoteChild);
					allSet.add(name);
				}
			}
			
			Iterator e = allSet.iterator();
			while (e.hasNext()) {
				String keyChildName = (String) e.next();

				if (progress != null) {
					if (progress.isCanceled()) {
						throw new OperationCanceledException();
					}
					// XXX show some progress?
				}

				IResource localChild =
					localSet != null ? (IResource) localSet.get(keyChildName) : null;

					ISubscriberResource remoteChild =
						remoteSet != null ? (ISubscriberResource) remoteSet.get(keyChildName) : null;
						
						if (localChild == null) {
							// there has to be a remote resource available if we got this far
							Assert.isTrue(remoteChild != null);
							boolean isContainer = remoteChild.isContainer();				
							localChild = getResourceChild(local /* parent */, keyChildName, isContainer);
						}
						mergedResources.put(localChild, remoteChild);				
			}
		}
		return mergedResources;
	}
	
	/**
	 * Get the remote sync bytes from the given remote resource handle.
	 * @param local the local resource
	 * @param remote the corresponding remote resource
	 * @return the synchronization bytes for the remote resource.
	 */
	protected abstract byte[] getRemoteSyncBytes(IResource local, ISubscriberResource remote) throws TeamException;
	
	/**
	 * Get the remote children of the given remote resource handle.
	 * @param remote the remote resource
	 * @param progress a progress monitor
	 * @return an array of the children of the renmote resource.
	 */
	protected abstract ISubscriberResource[] getRemoteChildren(ISubscriberResource remote, IProgressMonitor progress) throws TeamException;

	/**
	 * Get the local children that are of interest to the subscriber for the given local resource.
	 * @param parent the local resource
	 * @return the children of the local resource
	 */
	protected abstract IResource[] getLocalChildren(IResource parent) throws TeamException;

	/**
	 * Get the root of the remote tree corresponding to the given resource. This method may build the tree
	 * or may just return the root, in which the remote tree will be built during the collectChanges phase.
	 * @param resource the local resource
	 * @param depth the depth of the refresh
	 * @param cacheFileContentsHint a hint that indicates that remote contents may be needed 
	 * when comparing local and remote resources.
	 * @param monitor a progress monitor
	 * @return the remote resource corresponding to the given local resource
	 */
	protected abstract ISubscriberResource getRemoteTree(IResource resource, int depth, boolean cacheFileContentsHint, IProgressMonitor monitor) throws TeamException;
	
	/**
	 * Create a corresponding local resource handle for a remote resource that does not yet have a
	 * corresponding local resource taht exists.
	 * @param parent the local parent
	 * @param childName the name of the local resource
	 * @param isContainer the type of resource (file or folder)
	 * @return a local resource handle
	 */
	protected IResource getResourceChild(IResource parent, String childName, boolean isContainer) {
		if (parent.getType() == IResource.FILE) {
			return null;
		}
		if (isContainer) {
			return ((IContainer) parent).getFolder(new Path(childName));
		} else {
			return ((IContainer) parent).getFile(new Path(childName));
		}
	}
}
