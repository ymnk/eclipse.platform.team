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
import org.eclipse.team.internal.core.TeamPlugin;
import org.eclipse.team.internal.core.subscribers.SyncInfoStatistics;

/**
 * A dynamic collection of {@link SyncInfo} objects. This data structure is optimized 
 * for fast retrieval of out-of-sync resources . There are
 * generally two methods for obtaining instances of this collection:
 * <ul>
 *   <li>Create a {@link SyncInfoCollector} to dynamically collect changes from a team
 *  subscriber and reference its sync info set.
 *   <li>Create a set with a pre-defined set of sync info objects.
 * </ul>
 * <p>
 * This set generates a {@link ISyncInfoSetChangeEvent} every time it is modified. 
 * </p>
 * @see TeamSubscriberSyncInfoCollector
 * @see MutableSyncInfoSet
 * @since 3.0
 */
public class SyncInfoSet {
	// fields used to hold resources of interest
	// {IPath -> SyncInfo}
	protected Map resources = Collections.synchronizedMap(new HashMap());
	
	// {IPath -> Set of deep out of sync child IResources}
	// Weird thing is that the child set will include the parent if the parent is out of sync
	private Map parents = Collections.synchronizedMap(new HashMap());

	// keep track of number of sync kinds in the set
	private SyncInfoStatistics statistics = new SyncInfoStatistics();
	
	/**
	 * Don't directly allow creating an empty immutable set.
	 */
	protected SyncInfoSet() {
	}
	
	/**
	 * Create a new set that will contain the provided sync infos.
	 * @param infos a list of <code>SyncInfo</code> that are added to
	 * the new set. 
	 */
	public SyncInfoSet(SyncInfo[] infos) {
		this();
		for (int i = 0; i < infos.length; i++) {
			internalAdd(infos[i]);
		}
	}

	/**
	 * Return an array of all the resources that are known to be out-of-sync
	 * @return
	 */
	public synchronized SyncInfo[] members() {
		return (SyncInfo[]) resources.values().toArray(new SyncInfo[resources.size()]);
	}
	
	/**
	 * Return the immediate children of the given resource who are either out-of-sync 
	 * or contain out-of-sync resources.
	 * 
	 * @param resource 
	 * @return
	 */
	public synchronized IResource[] members(IResource resource) {
		if (resource.getType() == IResource.FILE) return new IResource[0];
		IContainer parent = (IContainer)resource;
		if (parent.getType() == IResource.ROOT) return getRoots(parent);
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
	

	/**
	 * Return wether the given resource has any children in the sync set
	 * @param resource
	 * @return
	 */
	public boolean hasMembers(IResource resource) {
		if (resource.getType() == IResource.FILE) return false;
		IContainer parent = (IContainer)resource;
		if (parent.getType() == IResource.ROOT) return !resources.isEmpty();
		IPath path = parent.getFullPath();
		Set allDescendants = (Set)parents.get(path);
		return (allDescendants != null && !allDescendants.isEmpty());
	}
	
	/**
	 * Return the out-of-sync descendants of the given resource. If the given resource
	 * is out of sync, it will be included in the result.
	 * 
	 * @param container
	 * @return
	 */
	public synchronized SyncInfo[] getOutOfSyncDescendants(IResource resource) {
		if (resource.getType() == IResource.FILE) {
			SyncInfo info = getSyncInfo(resource);
			if (info == null) {
				return new SyncInfo[0];
			} else {
				return new SyncInfo[] { info };
			}
		}
		// if it's the root then return all out of sync resources.
		if(resource.getType() == IResource.ROOT) {
			return members();
		}
		// for folders return all children deep.
		List infos = new ArrayList();
		IResource[] children = internalGetDescendants(resource);
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

	public synchronized IResource[] getResources() {
		SyncInfo[] infos = members();
		List resources = new ArrayList();
		for (int i = 0; i < infos.length; i++) {
			SyncInfo info = infos[i];
			resources.add(info.getLocal());
		}
		return (IResource[]) resources.toArray(new IResource[resources.size()]);
	}
	
	public synchronized SyncInfo getSyncInfo(IResource resource) {
		return (SyncInfo)resources.get(resource.getFullPath());
	}

	public int size() {
		return resources.size();		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISyncInfoSet#dispose()
	 */
	public void dispose() {
		// Nothing to do
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISyncInfoSet#countFor(int, int)
	 */
	public long countFor(int kind, int mask) {
		return statistics.countFor(kind, mask);
	}
	
	/**
	 * Returns true if there are any conflicting nodes in the set, and
	 * false otherwise.
	 */
	public boolean hasConflicts() {
		return countFor(SyncInfo.CONFLICTING, SyncInfo.DIRECTION_MASK) > 0;
	}
	
	/**
	 * Returns true if this sync set has incoming changes.
	 * Note that conflicts are not considered to be incoming changes.
	 */
	public boolean hasIncomingChanges() {
		return countFor(SyncInfo.INCOMING, SyncInfo.DIRECTION_MASK) > 0;
	}
	
	/**
	 * Returns true if this sync set has outgoing changes.
	 * Note that conflicts are not considered to be outgoing changes.
	 */
	public boolean hasOutgoingChanges() {
		return countFor(SyncInfo.OUTGOING, SyncInfo.DIRECTION_MASK) > 0;
	}
		
	public boolean isEmpty() {
		return resources.isEmpty();
	}

	
	protected synchronized void internalAdd(SyncInfo info) {
		internalAddSyncInfo(info);
		IResource local = info.getLocal();
		addToParents(local, local);
	}
	
	protected void internalAddSyncInfo(SyncInfo info) {
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

	/*
	 * This sync set maintains a data structure that maps a folder to the
	 * set of out-of-sync resources at or below the folder. This method updates
	 * this data sructure.
	 */
	private boolean addToParents(IResource resource, IResource parent) {
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
	
	/**
	 * This method is invoked when a resource is added to the sync set.
	 * The argument will be the highest node that previously had no
	 * descendants in the sync set but now has descendants.
	 * Subclasses may override this method in order to capture
	 * this event.
	 * @param parent the added subtree root
	 */
	protected void internalAddedSubtreeRoot(IResource parent) {
		// do nothing by default
	}

	protected synchronized SyncInfo internalRemove(IResource local) {
		IPath path = local.getFullPath();
		SyncInfo info = (SyncInfo)resources.remove(path);
		if (info != null) {
			statistics.remove(info);
		}
		removeFromParents(local, local);
		return info;
	}
	
	private boolean removeFromParents(IResource resource, IResource parent) {
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
	
	/**
	 * This method is invoked when a resource is removed from the set.
	 * The resource srgument is the highest resource that used to have
	 * descendants in the set but no longer does.
	 * @param parent the removed subtree root
	 */
	protected void internalRemovedSubtreeRoot(IResource parent) {
		// do nothing by default
	}

	private IResource[] getRoots(IContainer root) {
		Set possibleChildren = parents.keySet();
		Set children = new HashSet();
		for (Iterator it = possibleChildren.iterator(); it.hasNext();) {
			Object next = it.next();
			IResource element = ((IWorkspaceRoot)root).findMember((IPath)next);
			if (element != null) {
				children.add(element.getProject());
			}
		}
		return (IResource[]) children.toArray(new IResource[children.size()]);
	}

	protected Set listeners = Collections.synchronizedSet(new HashSet());

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
	 * Reset the sync set so it is empty
	 */
	protected void clear() {
		resources.clear();
		parents.clear();
		statistics.clear();
	}
	
	protected synchronized IResource[] internalGetDescendants(IResource resource) {
		// The parent map contains a set of all out-of-sync children
		Set allChildren = (Set)parents.get(resource.getFullPath());
		if (allChildren == null) return new IResource[0];
		return (IResource[]) allChildren.toArray(new IResource[allChildren.size()]);
	}

	/**
	 * Run the given runnable, blocking other threads from modifying the 
	 * set and postponing any changes to the end.
	 * @param runnable a runnable
	 * @param progress a o\progress monitor or <code>null</code>
	 */
	public void run(IWorkspaceRunnable runnable, IProgressMonitor monitor) throws CoreException {
		runnable.run(Policy.monitorFor(monitor));		
	}
}