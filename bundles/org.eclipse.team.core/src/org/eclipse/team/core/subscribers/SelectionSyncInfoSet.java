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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.subscribers.FastSyncInfoFilter.SyncInfoDirectionFilter;

/**
 * This <code>SyncInfoSet</code> implements a collection of <code>SyncInfo</code> instances
 * that represent a selection of some sort. As such, the set can be pruned but cannot be 
 * expanded. Also, the set does not generate change events when it changes.
 */
public class SelectionSyncInfoSet extends SyncInfoSet {
	
	/**
	 * Create a <code>SelectionSyncInfoSet</code> containing the given <code>SyncInfo</code>
	 * instances
	 * @param infos the <code>SyncInfo</code> instances to be contained by this set
	 */
	public SelectionSyncInfoSet(SyncInfo[] infos) {
		for (int i = 0; i < infos.length; i++) {
			internalAdd(infos[i]);
		}
	}
	
	public synchronized void remove(IResource local) {
		internalRemove(local);
	}
	
	public void removeAll(IResource[] resources) {
		for (int i = 0; i < resources.length; i++) {
			remove(resources[i]);			
		}
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
		SyncInfo[] infos = getSyncInfos();
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
	public boolean hasNodes(FastSyncInfoFilter filter) {
		SyncInfo[] infos = getSyncInfos();
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
	public void selectNodes(FastSyncInfoFilter filter) {
		SyncInfo[] infos = getSyncInfos();
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
	public void rejectNodes(FastSyncInfoFilter filter) {
		SyncInfo[] infos = getSyncInfos();
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
	public SyncInfo[] getNodes(FastSyncInfoFilter filter) {
		List result = new ArrayList();
		SyncInfo[] infos = getSyncInfos();
		for (int i = 0; i < infos.length; i++) {
			SyncInfo info = infos[i];
			if (info != null && filter.select(info)) {
				result.add(info);
			}
		}
		return (SyncInfo[]) result.toArray(new SyncInfo[result.size()]);
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

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.SyncInfoSet#internalAddedSubtreeRoot(org.eclipse.core.resources.IResource)
	 */
	protected void internalAddedSubtreeRoot(IResource parent) {
		// Do nothing
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.SyncInfoSet#internalRemovedSubtreeRoot(org.eclipse.core.resources.IResource)
	 */
	protected void internalRemovedSubtreeRoot(IResource parent) {
		// Do nothing
	}
}
