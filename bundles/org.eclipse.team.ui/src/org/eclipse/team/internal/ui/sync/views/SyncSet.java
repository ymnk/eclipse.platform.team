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
package org.eclipse.team.internal.ui.sync.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.sync.SyncInfo;

/**
 * This class keeps track of a set of resources that are associated with 
 * a sychronization view/operation. 
 */
public class SyncSet {
	// fields used to hold resources of interest
	// {IPath -> SyncInfo}
	protected Map resources = new HashMap();
	
	// {IPath -> Set of deep out of sync child IResources}
	// weird thing is that the child set will include the
	// parent if the parent is out of sync
	protected Map parents = new HashMap();

	// fields used for change notification
	protected SyncSetChangedEvent changes;
	protected Set listeners = new HashSet();

	/**
	 * Return the IResource for the given model object that was returned by 
	 * SyncSet#members(IResource). Return <code>null</code> if the given
	 * object does not have a corresponding IResource.
	 * 
	 * @param element
	 * @return
	 */
	public static IResource getIResource(Object element) {
		IResource resource = null;
		if (element instanceof IResource) {
			return (IResource)element;
		} if (element instanceof SyncInfo) {
			resource = ((SyncInfo) element).getLocal();
		} else if (element instanceof SyncContainer) {
			resource = ((SyncContainer)element).getContainer();
		}
		return resource;
	}
	
	/**
	 * Return the sync kind for the given model object that was returned by 
	 * SyncSet#members(IResource). If syncSet is null, then the 
	 * sync kind for SyncContainers will always be 0.
	 * 
	 * @param element
	 * @return
	 */
	public static int getSyncKind(SyncSet syncSet, Object element) {
		if (element instanceof SyncInfo) {
			return ((SyncInfo) element).getKind();
		}  else if (element instanceof SyncContainer) {
			if (syncSet != null) {
				SyncInfo info = syncSet.getSyncInfo(getIResource(element));
				if (info != null) {
					return info.getKind();
				}
			}
		}
		return 0;
	}
	
	/**
	 * Get the model object (SyncSet, SyncInfo or SyncContainer) that is the
	 * parent of the given model object.
	 * 
	 * @param syncSet
	 * @param object
	 * @return
	 */
	public static Object getParent(SyncSet syncSet, Object object) {
		IResource resource = getIResource(object);
		if (resource == null) return null;
		IContainer parent = resource.getParent();
		return getModelObject(syncSet, parent);
	}
	

	/**
	 * Return the model object for the given IResource.
	 * @param resource
	 */
	public static Object getModelObject(SyncSet syncSet, IResource resource) {
		SyncInfo info = syncSet.getSyncInfo(resource);
		if (info != null) {
			return info;
		}
		// TODO: A subscriber may not be rooted at the project!!!
		if (resource.getType() == IResource.ROOT) {
			return syncSet;
		} else if (resource.getType() != IResource.FILE) {
			return new SyncContainer((IContainer)resource);
		}
		return null;
	}
	
	protected void resetChanges() {
		changes = new SyncSetChangedEvent(this);
	}

	protected void fireChanges() {
		// Use a synchronized block to ensure that the event we send is static
		SyncSetChangedEvent event;
		synchronized(this) {
			event = changes;
			resetChanges();
		}
		// Fire the events
		for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			ISyncSetChangedListener listener = (ISyncSetChangedListener) iter.next();
			listener.syncSetChanged(event);
		}
	}

	/**
	 * Add a change listener
	 * @param provider
	 */
	public void addSyncSetChangedListener(ISyncSetChangedListener listener) {
		listeners.add(listener);
	}

	/**
	 * Remove a change listener
	 * @param provider
	 */
	public void removeSyncSetChangedListener(ISyncSetChangedListener listener) {
		listeners.remove(listener);
	}

	protected void add(SyncInfo info) {
		IResource local = info.getLocal();
		IPath path = local.getFullPath();
		resources.put(path, info);
		changes.added(info);
		addToParents(local, local);
	}

	protected void remove(SyncInfo info) {
		IResource local = info.getLocal();
		IPath path = local.getFullPath();
		resources.remove(path);
		changes.removed(info);
		removeFromParents(local, local);
	}
	
	protected void changed(SyncInfo info) {
		changes.changed(info);
	}

	/**
	 * Reset the sync set so it is empty
	 */
	public void reset() {
		resources.clear();
		parents.clear();
		changes.reset();
	}
	
	protected boolean addToParents(IResource resource, IResource parent) {
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
			changes.addedRoot(parent);
		}
		return addedParent;
	}

	protected boolean removeFromParents(IResource resource, IResource parent) {
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
	 * Return the children of the given container who are either out-of-sync or contain
	 * out-of-sync resources.
	 * 
	 * The children will either be SyncInfo or SyncContainer.
	 * 
	 * @param container
	 * @return
	 */
	public Object[] members(IResource parent) {
		// TODO: must be optimized so that we don't traverse all the deep children to find
		// the immediate ones.
		List children = new ArrayList();
		IPath path = parent.getFullPath();
		Set possibleChildren = (Set)parents.get(path);
		if(possibleChildren != null) {
			for (Iterator it = possibleChildren.iterator(); it.hasNext();) {
				IResource element = (IResource) it.next();
				IPath childPath = element.getFullPath();
				if(childPath.segmentCount() == (path.segmentCount() +  1)) {
					SyncInfo childInfo = getSyncInfo(element);
					if (childInfo != null && isMember(childInfo)) {
						children.add(childInfo);
					} else if (element.getType() != IResource.FILE && hasMembers((IContainer)element)) {
						children.add(new SyncContainer((IContainer)element));
					}
				}				
			}
		}
		return (Object[]) children.toArray(new Object[children.size()]);
	}

	protected boolean isMember(SyncInfo info) {
		return resources.containsKey(info.getLocal().getFullPath());
	}

	protected boolean hasMembers(IContainer container) {
		return parents.containsKey(container.getFullPath());
	}

	/**
	 * Return an array of all the resources that are known to be out-of-sync
	 * @return
	 */
	public SyncInfo[] allMembers() {
		return (SyncInfo[]) resources.values().toArray(new SyncInfo[resources.size()]);
	}

	/**
	 * @param e
	 */
	protected void log(TeamException e) {
		// TODO: log or throw
	}
	
	protected void removeAllChildren(IResource resource) {
		// The parent map contains a set of all out-of-sync children
		Set allChildren = (Set)parents.get(resource.getFullPath());
		if (allChildren == null) return;
		removeAll(allChildren);
	}

	protected void removeAll(Set allChildren) {
		IResource [] removed = (IResource[]) allChildren.toArray(new IResource[allChildren.size()]);
		for (int i = 0; i < removed.length; i++) {
			remove(getSyncInfo(removed[i]));
		}
	}

	protected SyncInfo getSyncInfo(IResource resource) {
		return (SyncInfo)resources.get(resource.getFullPath());
	}

	/**
	 * This method is invoked by a SyncSetInput provider when the 
	 * provider is starting to provide new input to the SyncSet
	 */
	/* package */ void beginInput() {
		resetChanges();
	}
	
	/**
	 * This method is invoked by a SyncSetInput provider when the 
	 * provider is done providing new input to the SyncSet
	 */
	/* package */ void endInput() {
		fireChanges();
	}
}
