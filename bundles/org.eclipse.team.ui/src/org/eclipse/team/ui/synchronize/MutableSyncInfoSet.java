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
package org.eclipse.team.ui.synchronize;

import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.ui.synchronize.sets.SyncSetChangedEvent;
import org.eclipse.team.ui.synchronize.actions.SyncInfoFilter;
import org.eclipse.team.ui.synchronize.actions.SyncInfoFilter.SyncInfoDirectionFilter;

public class MutableSyncInfoSet extends SyncInfoSet {

	public MutableSyncInfoSet() {
		super();
	}
	
	public MutableSyncInfoSet(SyncInfo[] infos) {
		super(infos);
	}
	
	public synchronized void remove(IResource local) {
		IPath path = local.getFullPath();
		SyncInfo info = (SyncInfo)resources.remove(path);
		changes.removed(local);
		statistics.remove(info);
		removeFromParents(local, local);
	}
	
	public void removeAll(IResource[] resources) {
		for (int i = 0; i < resources.length; i++) {
			remove(resources[i]);			
		}
	}

	public synchronized void add(SyncInfo info) {
		internalAdd(info);
	}
	
	public void addAll(SyncInfoSet set) {
		SyncInfo[] infos = set.members();
		for (int i = 0; i < infos.length; i++) {
			add(infos[i]);
		}
	}
	
	public synchronized void changed(SyncInfo info) {
		internalAddSyncInfo(info);
		changes.changed(info);
	}

	/**
	 * Reset the sync set so it is empty
	 */
	public synchronized void clear() {
		resources.clear();
		parents.clear();
		changes.reset();
		statistics.clear();
	}

	public synchronized void removeAllChildren(IResource resource) {
		// The parent map contains a set of all out-of-sync children
		Set allChildren = (Set)parents.get(resource.getFullPath());
		if (allChildren == null) return;
		IResource [] removed = (IResource[]) allChildren.toArray(new IResource[allChildren.size()]);
		for (int i = 0; i < removed.length; i++) {
			remove(removed[i]);
		}
	}

	/**
	 * This method is invoked by a SyncSetInput provider when the 
	 * provider is starting to provide new input to the SyncSet
	 */
	public void beginInput() {
		synchronized(this) {
			resetChanges();
		}
	}

	/**
	 * This method is invoked by a SyncSetInput provider when the 
	 * provider is done providing new input to the SyncSet
	 */
	public void endInput() {
		fireChanges();
	}

	private void fireChanges() {
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
		for (int i = 0; i < allListeners.length; i++) {
			final ISyncSetChangedListener listener = allListeners[i];
			Platform.run(new ISafeRunnable() {
				public void handleException(Throwable exception) {
					// don't log the exception....it is already being logged in Platform#run
				}
				public void run() throws Exception {
					listener.syncSetChanged(event);
	
				}
			});
		}
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
			changes.removedRoot(parent);
		}
		return removedParent;
	}
	
	/**
	 * Removes all conflicting nodes from this set.
	 */
	public void removeConflictingNodes() {
		rejectNodes(new SyncInfoDirectionFilter(SyncInfo.CONFLICTING));
	}
	/**
	 * Removes all outgoing nodes from this set.
	 */
	public void removeOutgoingNodes() {
		rejectNodes(new SyncInfoDirectionFilter(SyncInfo.OUTGOING));
	}
	/**
	 * Removes all incoming nodes from this set.
	 */
	public void removeIncomingNodes() {
		rejectNodes(new SyncInfoDirectionFilter(SyncInfo.INCOMING));
	}
	
	/**
	 * Removes all nodes from this set that are not auto-mergeable conflicts
	 */
	public void removeNonMergeableNodes() {
		SyncInfo[] infos = members();
		for (int i = 0; i < infos.length; i++) {
			SyncInfo info = infos[i];
			if ((info.getKind() & SyncInfo.MANUAL_CONFLICT) != 0) {
				remove(info.getLocal());
			} else if ((info.getKind() & SyncInfo.DIRECTION_MASK) != SyncInfo.CONFLICTING) {
				remove(info.getLocal());
			}
		}
	}
	
	/**
	 * Indicate whether the set has nodes matching the given filter
	 */
	public boolean hasNodes(SyncInfoFilter filter) {
		SyncInfo[] infos = members();
		for (int i = 0; i < infos.length; i++) {
			SyncInfo info = infos[i];
			if (info != null && filter.select(info)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Removes all nodes from this set that do not match the given filter
	 */
	public void selectNodes(SyncInfoFilter filter) {
		SyncInfo[] infos = members();
		for (int i = 0; i < infos.length; i++) {
			SyncInfo info = infos[i];
			if (info == null || !filter.select(info)) {
				remove(info.getLocal());
			}
		}
	}
	
	/**
	 * Removes all nodes from this set that match the given filter
	 */
	public void rejectNodes(SyncInfoFilter filter) {
		SyncInfo[] infos = members();
		for (int i = 0; i < infos.length; i++) {
			SyncInfo info = infos[i];
			if (info != null && filter.select(info)) {
				remove(info.getLocal());
			}
		}
	}
	
	/**
	 * Return all nodes in this set that match the given filter
	 */
	public SyncInfo[] getNodes(SyncInfoFilter filter) {
		List result = new ArrayList();
		SyncInfo[] infos = members();
		for (int i = 0; i < infos.length; i++) {
			SyncInfo info = infos[i];
			if (info != null && filter.select(info)) {
				result.add(info);
			}
		}
		return (SyncInfo[]) result.toArray(new SyncInfo[result.size()]);
	}
}
