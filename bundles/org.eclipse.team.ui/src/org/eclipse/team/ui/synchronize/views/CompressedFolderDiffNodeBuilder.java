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
package org.eclipse.team.ui.synchronize.views;

import java.util.*;

import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.*;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.team.core.subscribers.ISyncInfoSetChangeEvent;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.ui.synchronize.SyncInfoDiffNode;
import org.eclipse.team.ui.synchronize.SyncInfoDiffNodeRoot;

public class CompressedFolderDiffNodeBuilder extends SyncInfoDiffNodeBuilder {

	public CompressedFolderDiffNodeBuilder(SyncInfoDiffNodeRoot root) {
		super(root);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.views.SyncInfoDiffNodeBuilder#createChildren(org.eclipse.compare.structuremergeviewer.IDiffContainer)
	 */
	protected IDiffElement[] createChildren(DiffNode container) {
		if (container instanceof SyncInfoDiffNode) {
			SyncInfoDiffNode node = (SyncInfoDiffNode)container;
			if (node.getResource().getType() == IResource.PROJECT) {
				return getProjectChildren(container, (IProject)node.getResource());
			}
			if (container instanceof CompressedFolderDiffNode) {
				return getFolderChildren(container, node.getResource());
			}
		}
		return super.createChildren(container);
	}
	
	private IDiffElement[] getFolderChildren(DiffNode parent, IResource resource) {
		// Folders will only contain out-of-sync children
		IResource[] children = getRoot().getSyncInfoSet().members(resource);
		List result = new ArrayList();
		for (int i = 0; i < children.length; i++) {
			IResource child = children[i];
			if (child.getType() == IResource.FILE) {
				result.add(createChildNode(parent, child));
			}
		}
		return (IDiffElement[])result.toArray(new IDiffElement[result.size()]);
	}

	private IDiffElement[] getProjectChildren(DiffNode parent, IProject project) {
		// The out-of-sync elements could possibly include the project so the code 
		// below is written to ignore the project
		SyncInfo[] outOfSync = getRoot().getSyncInfoSet().getOutOfSyncDescendants(project);
		Set result = new HashSet();
		Set resourcesToShow = new HashSet();
		for (int i = 0; i < outOfSync.length; i++) {
			SyncInfo info = outOfSync[i];
			IResource local = info.getLocal();
			if (local.getProjectRelativePath().segmentCount() == 1 && local.getType() == IResource.FILE) {
				resourcesToShow.add(local);
			} else {
				if (local.getType() == IResource.FILE) {
					resourcesToShow.add(local.getParent());
				} else if (local.getType() == IResource.FOLDER){
					resourcesToShow.add(local);
				}
			}
		}
		for (Iterator iter = resourcesToShow.iterator(); iter.hasNext();) {
			IResource resource = (IResource) iter.next();
			result.add(createChildNode(parent, resource));
		}
		
		return (IDiffElement[])result.toArray(new IDiffElement[result.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.views.SyncInfoDiffNodeBuilder#createChildNode(org.eclipse.compare.structuremergeviewer.DiffNode, org.eclipse.core.resources.IResource)
	 */
	protected SyncInfoDiffNode createChildNode(DiffNode parent, IResource resource) {
		if (resource.getType() == IResource.FOLDER) {
			SyncInfoDiffNode node = new CompressedFolderDiffNode(parent, getRoot().getSyncInfoSet(), resource);
			associateDiffNode(resource, node);
			addToViewer(node);
			return node;
		}
		return super.createChildNode(parent, resource);
	}
	
	/**
	 * Update the viewer for the sync set additions in the provided event.
	 * This method is invoked by <code>handleChanges(ISyncInfoSetChangeEvent)</code>.
	 * Subclasses may override.
	 * @param event
	 */
	protected void handleResourceAdditions(ISyncInfoSetChangeEvent event) {
		SyncInfo[] infos = event.getAddedResources();
		for (int i = 0; i < infos.length; i++) {
			SyncInfo info = infos[i];
			IResource local = info.getLocal();
			if (local.getType() == IResource.FILE) {
				DiffNode compressedNode = getModelObject(local.getParent());
				if (compressedNode == null) {
					DiffNode projectNode = getModelObject(local.getProject());
					if (projectNode == null) {
						projectNode = createChildNode(getRoot(), local.getProject());
					}
					compressedNode = createChildNode(projectNode, local.getParent());
				}
				createChildNode(compressedNode, local);
			} else {
				DiffNode existingNode = getModelObject(local);
				if (existingNode != null) {
					// The node was added as the parent of a newly added out-of-sync file
					refreshInViewer(existingNode);
				} else {
					DiffNode projectNode = getModelObject(local.getProject());
					if (projectNode == null) {
						projectNode = createChildNode(getRoot(), local.getProject());
					}
					createChildNode(projectNode, local);
				}
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.sync.views.SyncSetContentProvider#handleResourceRemovals(org.eclipse.team.internal.ui.sync.views.SyncSetChangedEvent)
	 */
	protected void handleResourceRemovals(ISyncInfoSetChangeEvent event) {
		
		IResource[] roots = event.getRemovedSubtreeRoots();
		Set removals = new HashSet();
		
		// First, deal with any projects that have been removed
		List remainingRoots = new ArrayList();
		for (int i = 0; i < roots.length; i++) {
			IResource resource = roots[i];
			if (resource.getType() == IResource.PROJECT) {
				removals.add(getModelObject(resource));
				unassociateDeeply(resource);
			} else {
				remainingRoots.add(resource);
			}
		}
		roots = (IResource[]) remainingRoots.toArray(new IResource[remainingRoots.size()]);

		// Then determine the other model objects that must be removed
		if (roots.length > 0) {
			IResource[] resources = event.getRemovedResources();
			for (int i = 0; i < resources.length; i++) {
				IResource resource = resources[i];
				if (isChildOfRoot(resource, roots) || isCompressedParentEmpty(resource)) {
					// A root of the resource has also been removed.
					// However, the resource's model parent would be a 
					// compressed folder on the resource's parent folder.
					unassociateDiffNode(resource);
					resource = resource.getParent();
				}
				DiffNode modelObject = getModelObject(resource);
				removals.add(modelObject);
				modelObject.getParent().removeToRoot(modelObject);
				unassociateDiffNode(resource);
				updateParentLabels(modelObject);
			}
		}
		AbstractTreeViewer tree = getTreeViewer();
		if (tree != null) {
			tree.remove(removals.toArray(new Object[removals.size()]));
		}
	}
	
	private boolean isCompressedParentEmpty(IResource resource) {
		IContainer parent = resource.getParent();
		if (parent == null 
				|| parent.getType() == IResource.ROOT
				|| parent.getType() == IResource.PROJECT) {
			return false;
		}
		// Check if the sync set has any file children of the parent
		IResource[] members = getRoot().getSyncInfoSet().members(parent);
		for (int i = 0; i < members.length; i++) {
			IResource member = members[i];
			if (member.getType() == IResource.FILE) {
				return false;
			}
		}
		// The parent does not contain any files
		return true;
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
}
