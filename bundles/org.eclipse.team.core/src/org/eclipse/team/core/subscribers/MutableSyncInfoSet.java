/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.team.internal.core.Policy;
import org.eclipse.team.internal.core.TeamPlugin;
import org.eclipse.team.internal.core.subscribers.SyncSetChangedEvent;

/**
 * The <code>MutableSyncInfoSet</code> is a <code>SyncInfoSet</code> that provides the ability to add,
 * remove and change <code>SyncInfo</code> and fires change event notifications to registered listeners. 
 * It also provides the ability
 * to batch changes in a single change notification as well as optimizations for sync info retrieval.
 * 
 * This class uses synchronized methods and synchronized blocks to protect internal data structures during both access
 * and modify operations and uses an <code>ILock</code> to make modification operations thread-safe. The events
 * are fired while this lock is held so clients responding to these events should not obtain their own internal locks
 * while processing change events.
 * 
 */
public class MutableSyncInfoSet extends SyncInfoSet {

	private SyncSetChangedEvent changes = new SyncSetChangedEvent(this);
	private ILock lock = Platform.getJobManager().newLock();
	private Set listeners = Collections.synchronizedSet(new HashSet());
	
	// {IPath -> Set of deep out of sync child IResources}
	// Weird thing is that the child set will include the parent if the parent is out of sync
	private Map parents = Collections.synchronizedMap(new HashMap());
	
	/**
	 * Create an empty set.
	 */
	public MutableSyncInfoSet() {
	}
	
	/**
	 * Create a set that initially contains the given <code>SyncInfo</code>.
	 * @param infos the <code>SyncInfo</code> to be added to the set
	 */
	public MutableSyncInfoSet(SyncInfo[] infos) {
		// Use the add method so out internal data structures are updated.
		// We don't need to worry about event generation since no listeners can be registered yet.
		try {
			beginInput();
			for (int i = 0; i < infos.length; i++) {
				SyncInfo info = infos[i];
				add(info);
			}
		} finally {
			endInput(null /* progress monitor */);
		}
	}
	
	/**
	 * Registers the given listener for sync info set notifications. Has
	 * no effect if an identical listener is already registered.
	 * 
	 * @param listener listener to register
	 */
	public void addSyncSetChangedListener(ISyncSetChangedListener listener) {
		synchronized(listeners) {
			listeners.add(listener);
		}
	}

	/**
	 * Deregisters the given listener for participant notifications. Has
	 * no effect if listener is not already registered.
	 * 
	 * @param listener listener to deregister
	 */
	public void removeSyncSetChangedListener(ISyncSetChangedListener listener) {
		synchronized(listeners) {
			listeners.remove(listener);
		}
	}
	
	/**
	 * Add the given <code>SyncInfo</code> to the set. A change event will
	 * be generated unless the call to this method is nested in between calls
	 * to <code>beginInput()</code> and <code>endInput(IProgressMonitor)</code>
	 * in which case the event for this addition and any other sync set
	 * change will be fired in a batched event when <code>endInput</code>
	 * is invoked.
	 * Invoking this method outside of the above mentioned block will result
	 * in the <code>endInput(IProgressMonitor)</code> being invoked with a null
	 * progress monitor. If responsiveness is required, the client should always
	 * nest sync set modifications.
	 * @param info
	 */
	public void add(SyncInfo info) {
		try {
			beginInput();
			internalAdd(info);
			changes.added(info);
			IResource local = info.getLocal();
			addToParents(local, local);
		} finally {
			endInput(null);
		}
	}
	
	/**
	 * Add all the syncinfo from the given set to this set.
	 * @param set the set whose sync info should be added to this set
	 */
	public void addAll(SyncInfoSet set) {
		try {
			beginInput();
			SyncInfo[] infos = set.getSyncInfos();
			for (int i = 0; i < infos.length; i++) {
				add(infos[i]);
			}
		} finally {
			endInput(null);
		}
	}
	
	/**
	 * Replace the existing sync info for the corresponding local resource (i.e. <code>SyncInfo#getLocal()</code>)
	 * with the provided sync info.
	 * @param info the new <code>SyncInfo</code> for the corresponding local resource
	 */
	public void changed(SyncInfo info) {
		try {
			beginInput();
			internalAdd(info);
			changes.changed(info);
		} finally {
			endInput(null);
		}
	}

	/**
	 * Remove the <code>SyncInfo</code> for the given resource from this set.
	 * @param resource the resource to be removed
	 */
	public void remove(IResource resource) {
		try {
			beginInput();
			SyncInfo info = internalRemove(resource);
			removeFromParents(resource, resource);
			changes.removed(resource, info);
		} finally {
			endInput(null);
		}

	}

	/**
	 * Reset the sync set so it is empty.
	 */
	public void clear() {
		try {
			beginInput();
			super.clear();
			synchronized(this) {
				parents.clear();
			}
			changes.reset();
		} finally {
			endInput(null);
		}
	}

	/**
	 * Remove from this set the <code>SyncInfo</code> for the given resource and any of its descendants
	 * within the specified depth. The depth is one of:
	 * <ul>
	 * <li><code>IResource.DEPTH_ZERO</code>: the resource only,
	 * <li><code>IResource.DEPTH_ONE</code>: the resource or its direct children,
	 * <li><code>IResource.DEPTH_INFINITE</code>: the resource and all of it's descendants.
	 * <ul>
	 * @param resource the root of the resource subtree
	 * @param depth the depth of the subtree
	 */
	public void remove(IResource resource, int depth) {
		try {
			beginInput();
			if (getSyncInfo(resource) != null) {
				remove(resource);
			}
			if (depth == IResource.DEPTH_ZERO || resource.getType() == IResource.FILE) return;
			if (depth == IResource.DEPTH_ONE) {
				IResource[] members = members(resource);
				for (int i = 0; i < members.length; i++) {
					IResource member = members[i];
					if (getSyncInfo(member) != null) {
						remove(member);
					}
				}
			} else if (depth == IResource.DEPTH_INFINITE) {
				IResource [] toRemove = internalGetOutOfSyncDescendants((IContainer)resource);
				for (int i = 0; i < toRemove.length; i++) {
					remove(toRemove[i]);
				}
			} 
		} finally {
			endInput(null);
		}
	}

	/**
	 * This method is used to obtain a lock on the set which ensures thread safety
	 * and batches change notification. If the set is locked by another thread, 
	 * the calling thread will block until the lock 
	 * becomes available. This method uses an <code>org.eclipse.core.runtime.jobs.ILock</code>.
	 * <p>
	 * It is important that the lock is released after it is obtained. Calls to <code>endInput</code>
	 * should be done in a finally block as illustrated in the following code snippet.
	 * <pre>
	 *   set.beginInput();
	 *   try {
	 *       // do stuff
	 *   } finally {
	 *      set.endInput(progress);
	 *   }
	 * </pre>
	 * Calls to <code>beginInput</code> and <code>endInput</code> can be nested and must be matched.
	 */
	public void beginInput() {
		lock.acquire();
	}

	/**
	 * This method is used to release the lock on this set. The prgress monitor is needed to allow
	 * listeners to perform long-running operations is reponse to the set change. The lock is held
	 * while the listeners are notified so listeners must be cautious in order to avoid deadlock.
	 */
	public void endInput(IProgressMonitor monitor) {
		if (lock.getDepth() == 1) {
			// Remain locked while firing the events so the handlers 
			// can expect the set to remain constant while they process the events
			fireChanges(Policy.monitorFor(monitor));
		}
		lock.release();
	}
	
	private void resetChanges() {
		changes = new SyncSetChangedEvent(this);
	}

	/*
	 * Fire an event to all listeners containing the events (add, remove, change)
	 * accumulated so far. 
	 * @param monitor the progress monitor
	 */
	private void fireChanges(final IProgressMonitor monitor) {
		// Use a synchronized block to ensure that the event we send is static
		final SyncSetChangedEvent event;
		synchronized(this) {
			event = changes;
			resetChanges();
		}
		// Ensure that the list of listeners is not changed while events are fired.
		// Copy the listeners so that addition/removal is not blocked by event listeners
		if(event.isEmpty() && ! event.isReset()) return;
		ISyncSetChangedListener[] allListeners;
		synchronized(listeners) {
			allListeners = (ISyncSetChangedListener[]) listeners.toArray(new ISyncSetChangedListener[listeners.size()]);
		}
		// Fire the events using an ISafeRunnable
		monitor.beginTask(null, 100 * allListeners.length);
		for (int i = 0; i < allListeners.length; i++) {
			final ISyncSetChangedListener listener = allListeners[i];
			Platform.run(new ISafeRunnable() {
				public void handleException(Throwable exception) {
					// don't log the exception....it is already being logged in Platform#run
				}
				public void run() throws Exception {
					listener.syncSetChanged(event, Policy.subMonitorFor(monitor, 100));
	
				}
			});
		}
		monitor.done();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.SyncInfoSet#run(org.eclipse.core.resources.IWorkspaceRunnable, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IWorkspaceRunnable runnable, IProgressMonitor monitor) throws CoreException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(null, 100);
		beginInput();
		try {
			super.run(runnable, Policy.subMonitorFor(monitor, 80));
		} finally {
			endInput(Policy.subMonitorFor(monitor, 20));
		}
		
	}

	/*
	 * This sync set maintains a data structure that maps a folder to the
	 * set of out-of-sync resources at or below the folder. This method updates
	 * this data structure. It also invokes the <code>internalAddedSubtreeRoot</code>
	 * for the highest added parent to allow subclass to record this in change events.
	 */
	private synchronized boolean addToParents(IResource resource, IResource parent) {
		if (parent.getType() == IResource.ROOT) {
			return false;
		}
		// this flag is used to indicate if the parent was previosuly in the set
		boolean addedParent = false;
		if (parent.getType() == IResource.FILE) {
			// the file is new
			addedParent = true;
		} else {
			Set children = (Set)parents.get(parent.getFullPath());
			if (children == null) {
				children = new HashSet();
				parents.put(parent.getFullPath(), children);
				// this is a new folder in the sync set
				addedParent = true;
			}
			children.add(resource);
		}
		// if the parent already existed and the resource is new, record it
		if (!addToParents(resource, parent.getParent()) && addedParent) {
			internalAddedSubtreeRoot(parent);
		}
		return addedParent;
	}
	
	/*
	 * Recursively remove the resource from it's parents in the internal data
	 * structures of the set. Also, invoke the <code>internalRemoveSubtreeRoot</code>
	 * method to allow subclass to include the subtree removal in a change event.
	 */
	private synchronized boolean removeFromParents(IResource resource, IResource parent) {
		if (parent.getType() == IResource.ROOT) {
			return false;
		}
		// this flag is used to indicate if the parent was removed from the set
		boolean removedParent = false;
		if (parent.getType() == IResource.FILE) {
			// the file will be removed
			removedParent = true;
		} else {
			Set children = (Set)parents.get(parent.getFullPath());
			if (children != null) {
				children.remove(resource);
				if (children.isEmpty()) {
					parents.remove(parent.getFullPath());
					removedParent = true;
				}
			}
		}
		//	if the parent wasn't removed and the resource was, record it
		if (!removeFromParents(resource, parent.getParent()) && removedParent) {
			internalRemovedSubtreeRoot(parent);
		}
		return removedParent;
	}
	
	/*
	 * This method is invoked when a resource is added to the sync set.
	 * The argument will be the highest node that previously had no
	 * descendants in the sync set but now has descendants.
	 * Subclasses may override this method in order to capture
	 * this event.
	 * @param parent the added subtree root
	 */
	private void internalAddedSubtreeRoot(IResource parent) {
		changes.addedSubtreeRoot(parent);
	}
	
	/*
	 * This method is invoked when a resource is removed from the set.
	 * The resource argument is the highest resource that used to have
	 * descendants in the set but no longer does.
	 * @param parent the removed subtree root
	 */
	private void internalRemovedSubtreeRoot(IResource parent) {
		changes.removedSubtreeRoot(parent);
	}
	
	/*
	 * Return the projects that contain out-of-sync resources in the set
	 */
	private synchronized IResource[] internalMembers(IWorkspaceRoot root) {
		Set possibleChildren = parents.keySet();
		Set children = new HashSet();
		for (Iterator it = possibleChildren.iterator(); it.hasNext();) {
			Object next = it.next();
			IResource element = root.findMember((IPath)next);
			if (element != null) {
				children.add(element.getProject());
			}
		}
		return (IResource[]) children.toArray(new IResource[children.size()]);
	}

	/*
	 * Return the <code>SyncInfo</code> for all out-of-sync resources in the
	 * set that are at or below the given resource in the resource hierarchy.
	 * @param resource the root resource
	 * @return the <code>SyncInfo</code> for all out-of-sync resources at or below the given resource
	 */
	private synchronized IResource[] internalGetOutOfSyncDescendants(IContainer resource) {
		// The parent map contains a set of all out-of-sync children
		Set allChildren = (Set)parents.get(resource.getFullPath());
		if (allChildren == null) return new IResource[0];
		return (IResource[]) allChildren.toArray(new IResource[allChildren.size()]);
	}
	
	/**
	 * Return the <code>SyncInfo</code> for all out-of-sync resources in the
	 * set that are at or below the given resource in the resource hierarchy.
	 * @param resource the root resource
	 * @return the <code>SyncInfo</code> for all out-of-sync resources at or below the given resource
	 */
	private synchronized SyncInfo[] internalGetDeepSyncInfo(IContainer resource) {
		List infos = new ArrayList();
		IResource[] children = internalGetOutOfSyncDescendants((IContainer)resource);
		for (int i = 0; i < children.length; i++) {
			IResource child = children[i];
			SyncInfo info = getSyncInfo(child);
			if(info != null) {
				infos.add(info);
			} else {
				TeamPlugin.log(IStatus.INFO, "missing sync info: " + child.getFullPath(), null);
			}
		}
		return (SyncInfo[]) infos.toArray(new SyncInfo[infos.size()]);
	}	

	/**
	 * Return wether the given resource has any children in the sync set. The children
	 * could be either out-of-sync resources that are contained by the set or containers
	 * that are ancestors of out-of-sync resources contained by the set.
	 * @param resource the parent resource
	 * @return the members of the parent in the set.
	 */
	public synchronized boolean hasMembers(IResource resource) {
		if (resource.getType() == IResource.FILE) return false;
		IContainer parent = (IContainer)resource;
		if (parent.getType() == IResource.ROOT) return !isEmpty();
		IPath path = parent.getFullPath();
		Set allDescendants = (Set)parents.get(path);
		return (allDescendants != null && !allDescendants.isEmpty());
	}
	
	/**
	 * Return the immediate children of the given resource who are either out-of-sync 
	 * or contain out-of-sync resources.
	 * 
	 * @param resource the parent resource 
	 * @return the children of the resource that are either out-of-sync or are ancestors of
	 * out-of-sync resources contained in the set
	 */
	public synchronized IResource[] members(IResource resource) {
		if (resource.getType() == IResource.FILE) return new IResource[0];
		IContainer parent = (IContainer)resource;
		if (parent.getType() == IResource.ROOT) return internalMembers((IWorkspaceRoot)parent);
		// OPTIMIZE: could be optimized so that we don't traverse all the deep 
		// children to find the immediate ones.
		Set children = new HashSet();
		IPath path = parent.getFullPath();
		Set possibleChildren = (Set)parents.get(path);
		if(possibleChildren != null) {
			for (Iterator it = possibleChildren.iterator(); it.hasNext();) {
				Object next = it.next();
				IResource element = (IResource)next;
				IPath childPath = element.getFullPath();
				IResource modelObject = null;
				if(childPath.segmentCount() == (path.segmentCount() +  1)) {
					modelObject = element;

				} else if (childPath.segmentCount() > path.segmentCount()) {
					IContainer childFolder = parent.getFolder(new Path(childPath.segment(path.segmentCount())));
					modelObject = childFolder;
				}
				if (modelObject != null) {
					children.add(modelObject);
				}
			}
		}
		return (IResource[]) children.toArray(new IResource[children.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.SyncInfoSet#getSyncInfos(org.eclipse.core.resources.IResource, int)
	 */
	public synchronized SyncInfo[] getSyncInfos(IResource resource, int depth) {
		if (depth == IResource.DEPTH_INFINITE && resource.getType() != IResource.FILE) {
			// Optimize the deep retrieval
			return internalGetDeepSyncInfo((IContainer)resource);
		}
		return super.getSyncInfos(resource, depth);
	}
}
