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
package org.eclipse.team.internal.core.subscribers;

import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.subscribers.*;

/**
 * This event keeps track of the changes in a sync set
 */
public class SyncSetChangedEvent implements ISyncInfoSetChangeEvent {
	
	private SyncInfoSet set;
	
	// List that accumulate changes
	// SyncInfo
	private Set changedResources = new HashSet();
	private Set removedResources = new HashSet();
	private Set addedResources = new HashSet();
	
	// IResources
	private Set removedSubtrees = new HashSet();
	private Set addedSubtrees = new HashSet();
	
	private boolean reset = false;

	public SyncSetChangedEvent(SyncInfoSet set) {
		super();
		this.set = set;
	}

	public void added(SyncInfo info) {
		if (removedResources.contains(info.getLocal())) {
			// A removal followed by an addition is treated as a change
			removedResources.remove(info.getLocal());
			changed(info);
		} else {
			addedResources.add(info);
		}
	}
	
	public void removed(IResource resource, SyncInfo info) {
		if (changedResources.contains(info)) {
			// No use in reporting the change since it has subsequently been removed
			changedResources.remove(info);
		} else if (addedResources.contains(info)) {
			// An addition followed by a removal can be dropped 
			addedResources.remove(info);
			return;
		}
		removedResources.add(resource);
	}
	
	public void changed(SyncInfo info) {
		changedResources.add(info);
	}
	
	public void removedSubtreeRoot(IResource root) {
		if (addedSubtrees.contains(root)) {
			// The root was added and removed which is a no-op
			addedSubtrees.remove(root);
		} else if (isDescendantOfAddedRoot(root)) {
			// Nothing needs to be done since no listeners ever knew about the root
		} else {
			// check if the root is a child of an existing root
			// (in which case it need not be added).
			// Also, remove any exisiting roots that are children
			// of the new root
			for (Iterator iter = removedSubtrees.iterator(); iter.hasNext();) {
				IResource element = (IResource) iter.next();
				// check if the root is already in the list
				if (root.equals(element)) return;
				if (isParent(root, element)) {
					// the root invalidates the current element
					iter.remove();
				} else if (isParent(element, root)) {
					// the root is a child of an existing element
					return;
				}
			}
			removedSubtrees.add(root);
		}
	}
	
	private boolean isParent(IResource root, IResource element) {
		return root.getFullPath().isPrefixOf(element.getFullPath());
	}

	public void addedSubtreeRoot(IResource parent) {
		if (removedSubtrees.contains(parent)) {
			// The root was re-added. Just removing the removedRoot
			// may not give the proper event.
			// Since we can't be sure, just force a reset.
			reset();
		} else {
			// only add the root if their isn't a higher root in the list already
			if (!isDescendantOfAddedRoot(parent)) {
				addedSubtrees.add(parent);
			}
		}
	}
	
	private boolean isDescendantOfAddedRoot(IResource resource) {
		for (Iterator iter = addedSubtrees.iterator(); iter.hasNext();) {
			IResource root = (IResource) iter.next();
			if (isParent(root, resource)) {
				// There is a higher added root already in the list
				return true;
			}
		}
		return false;
	}

	public SyncInfo[] getAddedResources() {
		return (SyncInfo[]) addedResources.toArray(new SyncInfo[addedResources.size()]);
	}

	public IResource[] getAddedSubtreeRoots() {
		return (IResource[]) addedSubtrees.toArray(new IResource[addedSubtrees.size()]);
	}

	public SyncInfo[] getChangedResources() {
		return (SyncInfo[]) changedResources.toArray(new SyncInfo[changedResources.size()]);
	}

	public IResource[] getRemovedResources() {
		return (IResource[]) removedResources.toArray(new IResource[removedResources.size()]);
	}

	public IResource[] getRemovedSubtreeRoots() {
		return (IResource[]) removedSubtrees.toArray(new IResource[removedSubtrees.size()]);
	}
		
	public SyncInfoSet getSet() {
		return set;
	}

	public void reset() {
		reset = true;
	}
	
	public boolean isReset() {
		return reset;
	}
	
	public boolean isEmpty() {
		return changedResources.isEmpty() && removedResources.isEmpty() && addedResources.isEmpty() && removedSubtrees.isEmpty() && addedSubtrees.isEmpty();
	}
}
