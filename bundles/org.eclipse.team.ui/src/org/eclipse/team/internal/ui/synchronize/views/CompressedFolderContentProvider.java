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
package org.eclipse.team.internal.ui.synchronize.views;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.team.core.subscribers.ISyncInfoSetChangeEvent;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.core.subscribers.SyncSetChangedEvent;
import org.eclipse.team.ui.synchronize.SyncInfoDiffNode;
import org.eclipse.team.ui.synchronize.views.*;

/**
 * The contents provider compressed in-sync folder paths
 */
public class CompressedFolderContentProvider extends SyncInfoSetTreeContentProvider {

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.views.SyncSetContentProvider#handleResourceChanges(org.eclipse.team.core.subscribers.ISyncInfoSetChangeEvent)
	 */
	protected void handleResourceChanges(ISyncInfoSetChangeEvent event) {
		AbstractTreeViewer tree = getTreeViewer();
		if (tree != null) {
			SyncInfo[] infos = event.getChangedResources();
			
			// Determine if any folders changed sync state
			Set projectsToRefresh = new HashSet();
			for (int i = 0; i < infos.length; i++) {
				SyncInfo info = infos[i];
				if (info.getLocal().getType() != IResource.FILE) {
					// For folder sync changes, we refresh the whole project
					// so that any compressed folders are adjusted properly
					// (as rebalancing is tricky)
					// TODO: Perhaps this could be optimized
					projectsToRefresh.add(info.getLocal().getProject());
				}
			}
			if (!projectsToRefresh.isEmpty()) {
				
				// Exclude any resources whose project will be refreshed
				// Create a new event
				SyncSetChangedEvent remainingChanges = new SyncSetChangedEvent(event.getSet());
				for (int i = 0; i < infos.length; i++) {
					SyncInfo info = infos[i];
					if (!projectsToRefresh.contains(info.getLocal().getProject())) {
						remainingChanges.changed(info);
					}
				}
				// Refresh the projects
				for (Iterator iter = projectsToRefresh.iterator(); iter.hasNext();) {
					IResource resource = (IResource) iter.next();
					tree.refresh(getModelObject(resource), true);
				}
				event = remainingChanges;
			}
		}
		super.handleResourceChanges(event);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.sync.views.SyncSetContentProvider#handleResourceAdditions(org.eclipse.team.internal.ui.sync.views.SyncSetChangedEvent)
	 */
	protected void handleResourceAdditions(ISyncInfoSetChangeEvent event) {
		AbstractTreeViewer tree = getTreeViewer();
		if (tree != null) {
			IResource[] roots = event.getAddedRoots();
			for (int i = 0; i < roots.length; i++) {
				IResource resource = roots[i];
				if (resource.getType() == IResource.PROJECT) {
					// Add the project
					tree.add(getModelObject(resource.getParent()), getModelObject(resource));
					updateParentLabels(resource);
				} else {
					// TODO: Refresh the resources project for now
					// because trying to rebalance compressed folder may be tricky
					// perhaps we could be smarter
					tree.refresh(getModelObject(resource.getProject()), true);
				}
			}
		} else {
			super.handleResourceAdditions(event);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.sync.views.SyncSetContentProvider#handleResourceRemovals(org.eclipse.team.internal.ui.sync.views.SyncSetChangedEvent)
	 */
	protected void handleResourceRemovals(ISyncInfoSetChangeEvent event) {
		AbstractTreeViewer tree = getTreeViewer();
		if (tree != null) {
			IResource[] roots = event.getRemovedRoots();
			IResource[] resources = event.getRemovedResources();
			Set removals = new HashSet();
			
			// First, deal with any projects that have been removed
			List remainingRoots = new ArrayList();
			for (int i = 0; i < roots.length; i++) {
				IResource resource = roots[i];
				if (resource.getType() == IResource.PROJECT) {
					removals.add(getModelObject(resource));
				} else {
					remainingRoots.add(resource);
				}
			}
			roots = (IResource[]) remainingRoots.toArray(new IResource[remainingRoots.size()]);
			
			// Then determine the other model objects that must be removed
			if (roots.length > 0) {
				for (int i = 0; i < resources.length; i++) {
					IResource resource = resources[i];
					if (isChildOfRoot(resource, roots)) {
						// A root of the resource has also been removed.
						// However, the resource's model parent would be a 
						// compressed folder on the resource's parent folder.
						removals.add(getModelObject(resource.getParent()));
						updateParentLabels(resource);
					} else {
						// The resources parent still has children so just remove 
						// the resource's model object
						removals.add(getModelObject(resource));
						updateParentLabels(resource);
					}
				}
			}
			tree.remove(removals.toArray(new Object[removals.size()]));
		} else {
			super.handleResourceRemovals(event);
		}
	}

	private boolean isChildOfRoot(IResource resource, IResource[] roots) {
		for (int i = 0; i < roots.length; i++) {
			IResource root = roots[i];
			if (!root.equals(resource)
					&& root.getFullPath().isPrefixOf(resource.getFullPath())) {
				return true;
			}
		}
		return false;
	}
	
	public Object getParent(Object element) {
		if (element instanceof CompressedFolder) {
			// The parent of a compressed folder is always the project
			return getModelObject(getResource(element).getProject());
		}
		Object parent = super.getParent(element);
		if (parent instanceof SyncInfoDiffNode) {
			SyncInfo info = ((SyncInfoDiffNode)parent).getSyncInfo();
			if (info == null) {
				// The resource is in-sync so return a compressed folder
				IResource resource = ((SyncInfoDiffNode)parent).getResource();
				if (resource.getType() == IResource.FOLDER) {					
					return new CompressedFolder(((SyncInfoDiffNode)parent).getSyncInfoSet(), resource);
					
				}
			}
		}
		return parent;
	}

	public Object[] getChildren(Object element) {
		IResource resource = getResource(element);
		if (resource != null) {
			if (resource.getType() == IResource.PROJECT) {
				return getProjectChildren((IProject)resource);
			} else if (resource.getType() == IResource.FOLDER) {
				return getFolderChildren(resource);
			}
		}
		return super.getChildren(element);
	}

	private Object[] getFolderChildren(IResource resource) {
		// Folders will only contain out-of-sync children
		IResource[] children = getSyncInfoSet().members(resource);
		List result = new ArrayList();
		for (int i = 0; i < children.length; i++) {
			IResource child = children[i];
			SyncInfo info = getSyncInfoSet().getSyncInfo(child);
			if (info != null) {
				result.add(getModelObject(info.getLocal()));
			}
		}
		return result.toArray(new Object[result.size()]);
	}

	private Object[] getProjectChildren(IProject project) {
		SyncInfo[] outOfSync = getSyncInfoSet().getOutOfSyncDescendants(project);
		Set result = new HashSet();
		for (int i = 0; i < outOfSync.length; i++) {
			SyncInfo info = outOfSync[i];
			IResource local = info.getLocal();
			if (local.getProjectRelativePath().segmentCount() == 1) {
				// If the resource is a child of the project, include it uncompressed
				result.add(getModelObject(local));
			} else {
				IContainer container = getLowestInSyncParent(local);
				if (container.getType() == IResource.FOLDER) {
					result.add(getModelObject(container));
				}
			}
		}
		return result.toArray(new Object[result.size()]);
	}

	/**
	 * Return a compressed folder if the provided resource is an in-sync folder.
	 * Warning: This method will return a compressed folder for any in-sync
	 * folder, even those that do not contain out-of-sync resources (i.e. those that
	 * are not visible in the view).
	 */
	public Object getModelObject(IResource resource) {
		if (resource.getType() == IResource.FOLDER && getSyncInfoSet().getSyncInfo(resource) == null) {
			return new CompressedFolder(getSyncInfoSet(), resource);
		}
		return super.getModelObject(resource);
	}
	
	private IContainer getLowestInSyncParent(IResource resource) {
		if (resource.getType() == IResource.ROOT) return (IContainer)resource;
		IContainer parent = resource.getParent();
		if (getSyncInfoSet().getSyncInfo(parent) == null) {
			return parent;
		}
		return getLowestInSyncParent(parent);
	}
}
