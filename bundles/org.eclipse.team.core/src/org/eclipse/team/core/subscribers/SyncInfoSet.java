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

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.team.internal.core.Policy;
import org.eclipse.team.internal.core.subscribers.SyncInfoStatistics;
import org.eclipse.team.internal.core.subscribers.SyncSetChangedEvent;

/**
 * A potentially dynamic collection of {@link SyncInfo} objects that provides
 * several methods for accessing the out-of-sync resources contained in the set. There are
 * generally two ways in which a sync info sets are used, both of which 
 * are implemented as subclasses of <code>SyncInfoSet</code>:
 * <ul>
 *   <li>{@link MutableSyncInfoSet}: used to dynamically collect changes 
 * from a team subscriber or some other
 * source and maintain the set of out-of-sync resources for display in a view.
 *   <li>{@link SelectionSyncInfoSet}: used to provide input to {@link Subscriber} 
 * specific operations that operate on a collection of {@link SyncInfo}.
 * </ul>
 * <p>
 * This class provides access to the sync set contents to clients and provides modification
 * operations to subclasses. In addition, this class defines the protocol for registering 
 * and deregistering change listeners but it is up to subclasses to record changes and 
 * notify any listeners about changes.
 * 
 * </p>
 * @see MutableSyncInfoSet
 * @see SelectionSyncInfoSet
 * @since 3.0
 */
public abstract class SyncInfoSet {
	// fields used to hold resources of interest
	// {IPath -> SyncInfo}
	private Map resources = Collections.synchronizedMap(new HashMap());

	// keep track of number of sync kinds in the set
	private SyncInfoStatistics statistics = new SyncInfoStatistics();
	
	/**
	 * Create an empty set.
	 */
	protected SyncInfoSet() {
	}

	/**
	 * Return an array of <code>SyncInfo</code> for all out-of-sync resources that are contained by the set.
	 * This call is equivalent in function to 
	 * <code>getSyncInfos(ResourcesPlugin.getWorkspace().getRoot(), IResource.DEPTH_INFINITE)</code>
	 * but is optimized to retrieve all contained <code>SyncInfo</code>.
	 * @return an array of <code>SyncInfo</code>
	 */
	public synchronized SyncInfo[] getSyncInfos() {
		return (SyncInfo[]) resources.values().toArray(new SyncInfo[resources.size()]);
	}
	
	/**
	 * Return the immediate children of the given resource who are either out-of-sync 
	 * or are ancestors of out-of-sync resources. The default implementation traverses all
	 * resources returned from <code>members()</code> and selects those that are
	 * children of the resource. Subclasses may override to optimize.
	 * 
	 * @param resource the parent resource 
	 * @return the children of the resource that are either out-of-sync or are ancestors of
	 * out-of-sync resources contained in the set
	 */
	public IResource[] members(IResource resource) {
		if (resource.getType() == IResource.FILE) {
			return new IResource[0];
		}
		IContainer container = (IContainer)resource;
		IPath containerFullPath = container.getFullPath();
		SyncInfo[] infos = getSyncInfos();
		Set result = new HashSet();
		for (int i = 0; i < infos.length; i++) {
			SyncInfo info = infos[i];
			IPath fullPath = info.getLocal().getFullPath();
			if (containerFullPath.segmentCount() > fullPath.segmentCount() 
					&& containerFullPath.isPrefixOf(fullPath)) {
				result.add(container.findMember(fullPath.segment(containerFullPath.segmentCount())));
			}
		}
		return (IResource[]) result.toArray(new IResource[result.size()]);
	}
	
	/**
	 * Return wether the given resource has any children in the sync set. The children
	 * could be either out-of-sync resources that are contained by the set or containers
	 * that are ancestors of out-of-sync resources contained by the set. The default implementation
	 * invokes <code>members(IResource)</code> in order to determine if there are members for the
	 * resource. Subclasses may override in order to optimize this.
	 * @param resource the parent resource
	 * @return the members of the parent in the set.
	 */
	public boolean hasMembers(IResource resource) {
		return members(resource).length > 0;
	}
	
	/**
	 * Return the <code>SyncInfo</code> for each out-of-sync resource in the subtree rooted at the given resource
	 * to the depth specified. The depth is one of:
	 * <ul>
	 * <li><code>IResource.DEPTH_ZERO</code>: the resource only,
	 * <li><code>IResource.DEPTH_ONE</code>: the resource or its direct children,
	 * <li><code>IResource.DEPTH_INFINITE</code>: the resource and all of it's descendants.
	 * <ul>
	 * If the given resource is out of sync, it will be included in the result.
	 * <p>
	 * The default implementation makes use of <code>getSyncInfo(IResource)</code>,
	 * <code>members(IResource)</code> and <code>getSyncInfos()</code>
	 * to provide the varying depths. Subclasses may override to optimize.
	 * 
	 * @param resource the root of the resource subtree
	 * @param depth the depth of the subtree
	 * @return the <code>SyncInfo</code> for any out-of-sync resources
	 */
	public synchronized SyncInfo[] getSyncInfos(IResource resource, int depth) {
		if (depth == IResource.DEPTH_ZERO || resource.getType() == IResource.FILE) {
			SyncInfo info = getSyncInfo(resource);
			if (info == null) {
				return new SyncInfo[0];
			} else {
				return new SyncInfo[] { info };
			}
		}
		if (depth == IResource.DEPTH_ONE) {
			List result = new ArrayList();
			SyncInfo info = getSyncInfo(resource);
			if (info != null) {
				result.add(info);
			}
			IResource[] members = members(resource);
			for (int i = 0; i < members.length; i++) {
				IResource member = members[i];
				info = getSyncInfo(member);
				if (info != null) {
					result.add(info);
				}
			}
			return (SyncInfo[]) result.toArray(new SyncInfo[result.size()]);
		}
		// if it's the root then return all out of sync resources.
		if(resource.getType() == IResource.ROOT) {
			return getSyncInfos();
		}
		// for folders return all children deep.
		return internalGetDeepSyncInfo((IContainer)resource);
	}

	/*
	 * Return the <code>SyncInfo</code> for all out-of-sync resources in the
	 * set that are at or below the given resource in the resource hierarchy.
	 * @param resource the root resource. This method is used by 
	 * <code>getSyncInfos(IResource, int)</code> when the depth is 
	 * <code>IResource.DEPTH_INFINITE</code> and the resource is a container.
	 * @return the <code>SyncInfo</code> for all out-of-sync resources at or below the given resource
	 */
	private SyncInfo[] internalGetDeepSyncInfo(IContainer container) {
		IPath containerFullPath = container.getFullPath();
		SyncInfo[] infos = getSyncInfos();
		Set result = new HashSet();
		for (int i = 0; i < infos.length; i++) {
			SyncInfo info = infos[i];
			IPath fullPath = info.getLocal().getFullPath();
			if (containerFullPath.isPrefixOf(fullPath)) {
				result.add(info);
			}
		}
		return (SyncInfo[]) result.toArray(new SyncInfo[result.size()]);
	}

	/**
	 * Return all out-of-sync resources contained in this set. The default implementation
	 * uses <code>getSyncInfos()</code> to determine the resources contained in the set.
	 * Subclasses may override to optimize.
	 * @return all out-of-sync resources contained in the set
	 */
	public IResource[] getResources() {
		SyncInfo[] infos = getSyncInfos();
		List resources = new ArrayList();
		for (int i = 0; i < infos.length; i++) {
			SyncInfo info = infos[i];
			resources.add(info.getLocal());
		}
		return (IResource[]) resources.toArray(new IResource[resources.size()]);
	}
	
	/**
	 * Return the <code>SyncInfo</code> for the given resource or <code>null</code>
	 * if the resource is not contained in the set.
	 * @param resource the resource
	 * @return the <code>SyncInfo</code> for the resource or <code>null</code>
	 */
	public synchronized SyncInfo getSyncInfo(IResource resource) {
		return (SyncInfo)resources.get(resource.getFullPath());
	}

	/**
	 * Return the number of out-of-sync resources contained in this set.
	 * @return the size of the set.
	 */
	public synchronized int size() {
		return resources.size();		
	}

	/**
	 * Return the number of out-of-sync resources in the given set whose sync kind
	 * matches the given kind and mask (e.g. <code>(SyncInfo#getKind() & mask) == kind</code>).
	 * @param kind the sync kind
	 * @param mask the sync kind mask
	 * @return the number of matching resources in the set.
	 */
	public long countFor(int kind, int mask) {
		return statistics.countFor(kind, mask);
	}
	
	/**
	 * Returns <code>true</code> if there are any conflicting nodes in the set, and
	 * <code>false</code> otherwise.
	 */
	public boolean hasConflicts() {
		return countFor(SyncInfo.CONFLICTING, SyncInfo.DIRECTION_MASK) > 0;
	}
	
	/**
	 * Return whether the set is empty.
	 * @return <code>true</code> if the set is empty
	 */
	public synchronized boolean isEmpty() {
		return resources.isEmpty();
	}
	
	/**
	 * Add the <code>SyncInfo</code> to the set, replacing any previously existing one.
	 * @param info the new <code>SyncInfo</code>
	 */
	protected synchronized void internalAdd(SyncInfo info) {
		IResource local = info.getLocal();
		IPath path = local.getFullPath();
		SyncInfo oldSyncInfo = (SyncInfo)resources.put(path, info); 
		if(oldSyncInfo == null) {
			statistics.add(info);
		} else {
			statistics.remove(oldSyncInfo);
			statistics.add(info);
		}
	}
	
	/**
	 * Remove the resource from the set, updating all internal data structures.
	 * @param resource the resource to be removed
	 * @return the <code>SyncInfo</code> that was just removed
	 */
	protected synchronized SyncInfo internalRemove(IResource resource) {
		IPath path = resource.getFullPath();
		SyncInfo info = (SyncInfo)resources.remove(path);
		if (info != null) {
			statistics.remove(info);
		}
		return info;
	}
	
	/**
	 * Registers the given listener for sync info set notifications if the
	 * <code>SyncInfoSet</code> implementation supports change notification.
	 * If change notification is not supported, an <code>UnsupportedOperationException</code>
	 * is thrown.
	 * Has no effect if an identical listener is already registered.
	 * 
	 * @param listener listener to register
	 */
	public void addSyncSetChangedListener(ISyncInfoSetChangeListener listener) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Deregisters the given listener for participant notifications. Has
	 * no effect if listener is not already registered.
	 * 
	 * @param listener listener to deregister
	 */
	public void removeSyncSetChangedListener(ISyncInfoSetChangeListener listener) {
		// Do nothing
	}
	
	/**
	 * Reset the sync set so it is empty.
	 */
	protected synchronized void clear() {
		resources.clear();
		statistics.clear();
	}

	/**
	 * Run the given runnable. For mutable subclasses this operation
	 * will block other threads from modifying the 
	 * set and postpone any change notifications until after the runnable
	 * has been executed. Mutable subclasses must override.
	 * <p>
	 * The given runnable may be run in the same thread as the caller or
	 * more be run asynchronously in another thread at the discretion of the
	 * subclass implementation. However, it is gaurenteed that two invocations
	 * of <code>run</code> performed in the same thread will be executed in the 
	 * same order even if run in different threads.
	 * 
	 * @param runnable a runnable
	 * @param progress a progress monitor or <code>null</code>
	 */
	public void run(IWorkspaceRunnable runnable, IProgressMonitor monitor) throws CoreException {
		runnable.run(Policy.monitorFor(monitor));		
	}
	
	/**
	 * Connect the listener to the sync set in such a fashion that the listener will
	 * be connected the the sync set using <code>addChangeListener</code>
	 * and issued a reset event. This is done to provide a means of connecting to the 
	 * sync set and initializing a model based on the sync set without worrying about 
	 * missing events.
	 * <p>
	 * The reset event may be done in the context of this method invocation or may be
	 * done in another thread at the discretion of the <code>SyncInfoSet</code>
	 * implementation. 
	 * <p>
	 * Disconnecting is done by calling <code>removeChangeListener</code>. Once disconnected,
	 * a listener can reconnect to be reinitialized.
	 * @param listener
	 * @param monitor
	 */
	public void connect(final ISyncInfoSetChangeListener listener, IProgressMonitor monitor) throws CoreException {
		run(new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) {
				try {
					monitor.beginTask(null, 100);
					addSyncSetChangedListener(listener);
					SyncSetChangedEvent event = new SyncSetChangedEvent(SyncInfoSet.this);
					event.reset();
					listener.syncSetChanged(event, Policy.subMonitorFor(monitor, 95));
				} finally {
					monitor.done();
				}
			}
		}, monitor);
	}
}