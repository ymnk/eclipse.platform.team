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

import org.eclipse.compare.internal.INavigatable;
import org.eclipse.compare.structuremergeviewer.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.internal.core.TeamPlugin;

public class SyncInfoDiffNodeBuilder implements ISyncInfoSetChangeListener {

	private AbstractTreeViewer viewer;
	private SyncInfoDiffNodeRoot root;
	
	// parents who need a label update accumulated while handling sync set changes
	private Set parentsToUpdate = new HashSet();
	
	private Map resourceMap = Collections.synchronizedMap(new HashMap());
	
	public SyncInfoDiffNodeBuilder(SyncInfoDiffNodeRoot root) {
		this.root = root;
	}

	/**
	 * Build the tree from the root diff node.
	 */
	public void buildTree() {
		associateDiffNode(ResourcesPlugin.getWorkspace().getRoot(), root);
		try {
			// Build the viewer tree and register for changes in a runnable
			// to ensure we don't miss anything
			root.getSyncInfoSet().run(new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					buildTree(root);
					root.getSyncInfoSet().addSyncSetChangedListener(SyncInfoDiffNodeBuilder.this);
				}
			}, null);
		} catch (CoreException e) {
			// Shouldn't happen
			TeamPlugin.log(e);
		}
	}
	
	protected IDiffElement[] buildTree(DiffNode node) {
		IDiffElement[] children = createChildren(node);
		for (int i = 0; i < children.length; i++) {
			IDiffElement element = children[i];
			if (element instanceof DiffNode) {
				buildTree((DiffNode)element);
			}
		}
		return children;
	}
	
	protected IDiffElement[] createChildren(DiffNode container) {
		if (container instanceof SyncInfoDiffNode) {
			SyncInfoDiffNode parentNode = (SyncInfoDiffNode)container;
			IResource resource = parentNode.getResource();
			if (resource == null) {
				resource = ResourcesPlugin.getWorkspace().getRoot();
			}
			IResource[] children = parentNode.getSyncInfoSet().members(resource);
			SyncInfoDiffNode[] nodes = new SyncInfoDiffNode[children.length];
			for (int i = 0; i < children.length; i++) {
				nodes[i] = createChildNode(parentNode, children[i]);
			}
			return nodes;
		}
		return new IDiffElement[0];
	}
	
	protected void associateDiffNode(IResource childResource, SyncInfoDiffNode childNode) {
		resourceMap.put(childResource, childNode);
	}
	
	protected void unassociateDiffNode(IResource childResource) {
		resourceMap.remove(childResource);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ccvs.syncviews.views.ISyncSetChangedListener#syncSetChanged(org.eclipse.team.ccvs.syncviews.views.SyncSetChangedEvent)
	 */
	public void syncSetChanged(final ISyncInfoSetChangeEvent event, IProgressMonitor monitor) {
		final Control ctrl = viewer.getControl();
		if (ctrl != null && !ctrl.isDisposed()) {
			ctrl.getDisplay().syncExec(new Runnable() {
				public void run() {
					if (!ctrl.isDisposed()) {
						BusyIndicator.showWhile(ctrl.getDisplay(), new Runnable() {
							public void run() {
								syncSetChanged(event);
							}
						});
					}
				}
			});
		}
	}
	
	/**
	 * Callback that is invoked from within an <code>asyncExec</code> when
	 * the model <code>SyncInfoSet</code> has changed. This method 
	 * disables redraw in the viewer and then either refreshes the
	 * viewer (if the event was a reset) or invokes the
	 * <code>handleChanges(ISyncInfoSetChangeEvent)</code> method.
	 * Subclasses not need to override this method to handle changes 
	 * but should instead override <code>handleChanges(ISyncInfoSetChangeEvent)</code>.
	 * @param event the <code>SyncInfoSet</code> change event.
	 * 
	 * @see handleChanges(ISyncInfoSetChangeEvent)
	 */
	protected void syncSetChanged(ISyncInfoSetChangeEvent event) {
		if (event.isReset()) {
			reset();
		} else {
			handleChanges(event);
		}
	}
	
	protected void removeAllFromTree() {
		IDiffElement[] elements = getRoot().getChildren();
		for (int i = 0; i < elements.length; i++) {
			viewer.remove(elements[i]);			
		}
	}

	protected void reset() {
		viewer.getControl().setRedraw(false);
		resourceMap.clear();
		clearModelObjects(getRoot());
		removeAllFromTree();
		buildTree(getRoot());
		viewer.refresh();
		if(viewer instanceof INavigatable) {
			((INavigatable)viewer).gotoDifference(true);
		}
		viewer.getControl().setRedraw(true);
	}
	
	/**
	 * Handle the changes made to the viewer's <code>SyncInfoSet</code>.
	 * This method delegates the changes to the three methods 
	 * <code>handleResourceChanges(ISyncInfoSetChangeEvent)</code>, 
	 * <code>handleResourceRemovals(ISyncInfoSetChangeEvent)</code> and
	 * <code>handleResourceAdditions(ISyncInfoSetChangeEvent)</code>.
	 * 
	 * @param event the event containing the changed resourcses.
	 */
	protected void handleChanges(ISyncInfoSetChangeEvent event) {
		try {
			viewer.getControl().setRedraw(false);
			handleResourceChanges(event);
			handleResourceRemovals(event);
			handleResourceAdditions(event);
			updateParentLabels();
		} finally {
			viewer.getControl().setRedraw(true);
		}
	}

	/**
	 * Return the <code>AbstractTreeViewer</code> asociated with this content provider
	 * or <code>null</code> if the viewer is not of the proper type.
	 * @return
	 */
	public AbstractTreeViewer getTreeViewer() {
		return viewer;
	}
	
	/**
	 * Update the viewer for the sync set changes in the provided event.
	 * This method is invoked by <code>handleChanges(ISyncInfoSetChangeEvent)</code>.
	 * Subclasses may override.
	 * @param event
	 * @see #handleSyncSetChanges(SyncSetChangedEvent)
	 */
	protected void handleResourceChanges(ISyncInfoSetChangeEvent event) {
		// Refresh the viewer for each changed resource
		SyncInfo[] infos = event.getChangedResources();
		for (int i = 0; i < infos.length; i++) {
			IResource local = infos[i].getLocal();
			DiffNode diffNode = getModelObject(local);
			if (diffNode != null) {
                refreshInViewer(diffNode);
			}
		}
	}
	
	protected void refreshInViewer(DiffNode diffNode) {
		AbstractTreeViewer tree = getTreeViewer();
		if (tree != null) {
			viewer.refresh(diffNode, true);
			updateParentLabels(diffNode);
		}
	}

	/**
	 * Update the viewer for the sync set removals in the provided event.
	 * This method is invoked by <code>handleChanges(ISyncInfoSetChangeEvent)</code>.
	 * Subclasses may override.
	 * @param event
	 */
	protected void handleResourceRemovals(ISyncInfoSetChangeEvent event) {
		IResource[] removedRoots = event.getRemovedSubtreeRoots();
		if (removedRoots.length == 0) return;
		DiffNode[] nodes = new DiffNode[removedRoots.length];
		for (int i = 0; i < nodes.length; i++) {
			nodes[i] = getModelObject(removedRoots[i]);
			DiffNode node = nodes[i];
			if (node != null) {
				clearModelObjects(node);
				updateParentLabels(node);
			}
		}
		AbstractTreeViewer tree = getTreeViewer();
		if (tree != null) {
			tree.remove(nodes);
		}
	}
	
	/**
	 * Update the viewer for the sync set additions in the provided event.
	 * This method is invoked by <code>handleChanges(ISyncInfoSetChangeEvent)</code>.
	 * Subclasses may override.
	 * @param event
	 */
	protected void handleResourceAdditions(ISyncInfoSetChangeEvent event) {
		IResource[] added = event.getAddedSubtreeRoots();
		for (int i = 0; i < added.length; i++) {
			IResource resource = added[i];
			DiffNode node = getModelObject(resource);
			if (node != null) {
				// Somehow the node exists. Remove it and read it to ensure what is
				// shown matches the contents of the sync set
				remove(resource);
			}
			buildSubTree(getModelObject(resource.getParent()), resource);	
		}
	}

	/**
	 * @param parent
	 * @param resource
	 * @return
	 */
	protected DiffNode buildSubTree(DiffNode parent, IResource resource) {
		SyncInfoDiffNode node = createChildNode(parent, resource);
		buildTree(node);
		return node;
	}

	protected SyncInfoDiffNode createChildNode(DiffNode parent, IResource resource) {
		SyncInfoSet set = parent instanceof SyncInfoDiffNode ? ((SyncInfoDiffNode)parent).getSyncInfoSet() : getRoot().getSyncInfoSet();
		SyncInfoDiffNode node = new SyncInfoDiffNode(parent, set, resource);
		associateDiffNode(resource, node);
		addToViewer(node);
		return node;
	}

	public void addToViewer(SyncInfoDiffNode node) {
		AbstractTreeViewer tree = getTreeViewer();
		if (tree != null) {
			tree.add(node.getParent(), node);
			updateParentLabels(node);
		}
	}

	/**
	 * Forces the viewer to update the labels for parents whose children have changed
	 * during this round of sync set changes.
	 */
	protected void updateParentLabels() {
		try {
			AbstractTreeViewer tree = getTreeViewer();
			if (tree != null) {
				tree.update(
						parentsToUpdate.toArray(new Object[parentsToUpdate.size()]),
						null 
				);
			}
		} finally {
			parentsToUpdate.clear();
		}
	}
	
	/**
	 * Forces the viewer to update the labels for parents of this element. This
	 * can be useful when parents labels include information about their children
	 * that needs updating when a child changes.
	 * <p>
	 * This method should only be called while processing sync set changes.
	 * Changed parents are accumulated and updated at the end of the change processing
	 */
	protected void updateParentLabels(DiffNode diffNode) {
		IDiffContainer parent = diffNode.getParent();
		while(parent != null) {
			parentsToUpdate.add(parent);
			parent = parent.getParent();
		}
	}
	
	/**
	 * Return the model object (i.e. an instance of <code>SyncInfoDiffNode</code> or one of its subclasses) 
	 * for the given IResource.
	 * 
	 * @param resource the resource
	 * @return the <code>SyncInfoDiffNode</code> for the given resource
	 */
	public DiffNode getModelObject(IResource resource) {
		return (DiffNode)resourceMap.get(resource);
	}
	
	/**
	 * Invokes <code>getModelObject(Object)</code> on an array of resources.
	 * 
	 * @param resources the resources
	 * @return the model objects for the resources
	 */
	protected Object[] getModelObjects(IResource[] resources) {
		Object[] result = new Object[resources.length];
		for (int i = 0; i < resources.length; i++) {
			result[i] = getModelObject(resources[i]);
		}
		return result;
	}

	/**
	 * Associate a viewer with the builder.
	 * @param v the viewer
	 */
	public void setViewer(AbstractTreeViewer v) {
		this.viewer = v;
	}

	/**
	 * Dispose of the builder
	 */
	public void dispose() {
		resourceMap.clear();
		root.getSyncInfoSet().removeSyncSetChangedListener(this);
	}
	
	/**
	 * @return Returns the root.
	 */
	protected SyncInfoDiffNodeRoot getRoot() {
		return root;
	}
	
	/**
	 * Clear the model objects from the diff tree, cleaning up any cached
	 * state (such as resource to model object map). This method recurses deeply
	 * on the tree to allow the cleanup of any cached state for the children as
	 * well.
	 * @param node the root node
	 */
	protected void clearModelObjects(DiffNode node) {
		IDiffElement[] children = node.getChildren();
		IResource resource = getResource(node);
		for (int i = 0; i < children.length; i++) {
			IDiffElement element = children[i];
			if(element instanceof DiffNode) {
				clearModelObjects((DiffNode)element);
			}
		}
		if (resource != null) {
			unassociateDiffNode(resource);
		}
		IDiffContainer parent = node.getParent();
		if(parent != null) {
			parent.removeToRoot(node);
		}
	}
	
	/**
	 * Return the resource associated with the node or <code>null</code> if the node
	 * is not directly associated with a resource.
	 * @param node a diff node
	 * @return a resource or <code>null</code>
	 */
	protected IResource getResource(DiffNode node) {
		if (node instanceof SyncInfoDiffNode) {
			return ((SyncInfoDiffNode)node).getResource();
		}
		return null;
	}

	/**
	 * Remove any traces of the resource and any of it's descendants in the
	 * hiearchy defined by the content provider from the content provider
	 * and the viewer it is associated with.
	 * @param resource
	 */
	protected void remove(IResource resource) {
		DiffNode node = getModelObject(resource);
		clearModelObjects(node);
		AbstractTreeViewer tree = getTreeViewer();
		if (tree != null) {
			tree.remove(node);
		}
		
	}
}
