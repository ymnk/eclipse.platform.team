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
package org.eclipse.team.ui.synchronize.viewers;

import java.util.*;

import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.*;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.team.core.subscribers.*;

public class CompressedFolderViewerInput extends SyncInfoSetViewerInput {

	public CompressedFolderViewerInput(SyncInfoTree set) {
		super(set);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncInfoDiffNodeRoot#getSorter()
	 */
	public ViewerSorter getViewerSorter() {
		return new SyncInfoDiffNodeSorter() {
			protected int compareNames(IResource resource1, IResource resource2) {
				if (resource1.getType() == IResource.FOLDER && resource2.getType() == IResource.FOLDER) {
					return collator.compare(resource1.getParent().toString(), resource2.getProjectRelativePath().toString());
				}
				return super.compareNames(resource1, resource2);
			}
		};
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.views.SyncInfoSetViewerInput#createChildren(org.eclipse.compare.structuremergeviewer.IDiffContainer)
	 */
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.viewers.SyncInfoSetViewerInput#createModelObjects(org.eclipse.compare.structuremergeviewer.DiffNode)
	 */
	protected IDiffElement[] createModelObjects(DiffNode container) {
		if (container instanceof SyncInfoDiffNode) {
			SyncInfoDiffNode node = (SyncInfoDiffNode)container;
			if (node.getResource().getType() == IResource.PROJECT) {
				return getProjectChildren(container, (IProject)node.getResource());
			}
			if (container instanceof CompressedFolderDiffNode) {
				return getFolderChildren(container, node.getResource());
			}
		}
		return super.createModelObjects(container);
	}
	
	private IDiffElement[] getFolderChildren(DiffNode parent, IResource resource) {
		// Folders will only contain out-of-sync children
		IResource[] children = getSyncInfoTree().members(resource);
		List result = new ArrayList();
		for (int i = 0; i < children.length; i++) {
			IResource child = children[i];
			if (child.getType() == IResource.FILE) {
				result.add(createModelObject(parent, child));
			}
		}
		return (IDiffElement[])result.toArray(new IDiffElement[result.size()]);
	}

	private IDiffElement[] getProjectChildren(DiffNode parent, IProject project) {
		// The out-of-sync elements could possibly include the project so the code 
		// below is written to ignore the project
		SyncInfo[] outOfSync = getSyncInfoTree().getSyncInfos(project, IResource.DEPTH_INFINITE);
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
			result.add(createModelObject(parent, resource));
		}
		
		return (IDiffElement[])result.toArray(new IDiffElement[result.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.views.SyncInfoSetViewerInput#createChildNode(org.eclipse.compare.structuremergeviewer.DiffNode, org.eclipse.core.resources.IResource)
	 */
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.viewers.SyncInfoSetViewerInput#createModelObject(org.eclipse.compare.structuremergeviewer.DiffNode, org.eclipse.core.resources.IResource)
	 */
	protected SyncInfoDiffNode createModelObject(DiffNode parent, IResource resource) {
		if (resource.getType() == IResource.FOLDER) {
			SyncInfoDiffNode node = new CompressedFolderDiffNode(parent, getSyncInfoTree(), resource);
			associateDiffNode(resource, node);
			addToViewer(node);
			return node;
		}
		return super.createModelObject(parent, resource);
	}
	
	/**
	 * Update the viewer for the sync set additions in the provided event.
	 * This method is invoked by <code>handleChanges(ISyncInfoSetChangeEvent)</code>.
	 * Subclasses may override.
	 * @param event
	 */
	protected void handleResourceAdditions(ISyncInfoTreeChangeEvent event) {
		SyncInfo[] infos = event.getAddedResources();
		for (int i = 0; i < infos.length; i++) {
			SyncInfo info = infos[i];
			IResource local = info.getLocal();
			DiffNode existingNode = getModelObject(local);
			if (existingNode == null) {
				if (local.getType() == IResource.FILE) {
					DiffNode compressedNode = getModelObject(local.getParent());
					if (compressedNode == null) {
						DiffNode projectNode = getModelObject(local.getProject());
						if (projectNode == null) {
							projectNode = createModelObject(this, local.getProject());
						}
						compressedNode = createModelObject(projectNode, local.getParent());
					}
					createModelObject(compressedNode, local);
				} else {
					DiffNode projectNode = getModelObject(local.getProject());
					if (projectNode == null) {
						projectNode = createModelObject(this, local.getProject());
					}
					createModelObject(projectNode, local);
				}
			} else {
				// Either The folder node was added as the parent of a newly added out-of-sync file
				// or the file was somehow already there so just refresh
				refreshInViewer(existingNode);
				
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.sync.views.SyncSetContentProvider#handleResourceRemovals(org.eclipse.team.internal.ui.sync.views.SyncSetChangedEvent)
	 */
	protected void handleResourceRemovals(ISyncInfoTreeChangeEvent event) {
		IResource[] roots = event.getRemovedSubtreeRoots();
		Set removals = new HashSet();
		
		// First, deal with any projects that have been removed
		List remainingRoots = new ArrayList();
		for (int i = 0; i < roots.length; i++) {
			IResource resource = roots[i];
			if (resource.getType() == IResource.PROJECT) {
				DiffNode modelObject = getModelObject(resource);
				removals.add(modelObject);
				clearModelObjects(modelObject);
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
					resource = resource.getParent();
				}
				DiffNode modelObject = getModelObject(resource);
				if (modelObject != null) {
					removals.add(modelObject);
					clearModelObjects(modelObject);
					updateParentLabels(modelObject);
				}
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
		IResource[] members = getSyncInfoTree().members(parent);
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
