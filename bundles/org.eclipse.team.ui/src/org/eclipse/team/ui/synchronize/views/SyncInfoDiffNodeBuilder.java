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

import org.eclipse.compare.structuremergeviewer.*;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.ui.synchronize.SyncInfoDiffNode;
import org.eclipse.team.ui.synchronize.SyncInfoDiffNodeRoot;

public class SyncInfoDiffNodeBuilder implements ISyncSetChangedListener {

	private AbstractTreeViewer viewer;
	private SyncInfoDiffNodeRoot root;
	
	// parents who need a label update accumulated while handling sync set changes
	private Set parentsToUpdate = new HashSet();
	
	private Map resourceMap = Collections.synchronizedMap(new HashMap());
	
	public SyncInfoDiffNodeBuilder(SyncInfoDiffNodeRoot root) {
		this.root = root;
	}

	/**
	 * Build the tree
	 */
	public void buildTree() {
		// TODO: need to ensure taht sync set doesn't change while in this method
		buildTree(root);
		associateDiffNode(ResourcesPlugin.getWorkspace().getRoot(), root);
		root.getSyncInfoSet().addSyncSetChangedListener(this);
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
			if (resource != null) {
				IResource[] children = parentNode.getSyncInfoSet().members(resource);
				SyncInfoDiffNode[] nodes = new SyncInfoDiffNode[children.length];
				for (int i = 0; i < children.length; i++) {
					nodes[i] = createChildNode(parentNode, children[i]);
				}
				return nodes;
			}
		}
		return new IDiffElement[0];
	}
	
	protected void associateDiffNode(IResource childResource, SyncInfoDiffNode childNode) {
		resourceMap.put(childResource, childNode);
	}
	
	protected void unassociateDiffNode(IResource childResource) {
		resourceMap.remove(childResource);
	}
	
	protected void unassociateDeeply(IResource parentResource) {
		unassociateDiffNode(parentResource);
		for (Iterator iter = resourceMap.keySet().iterator(); iter.hasNext();) {
			IResource r = (IResource) iter.next();
			if (parentResource.getFullPath().isPrefixOf(r.getFullPath())) {
				iter.remove();
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ccvs.syncviews.views.ISyncSetChangedListener#syncSetChanged(org.eclipse.team.ccvs.syncviews.views.SyncSetChangedEvent)
	 */
	public void syncSetChanged(final ISyncInfoSetChangeEvent event, IProgressMonitor monitor) {
		if (viewer == null) return;
		final Control ctrl = viewer.getControl();
		if (ctrl != null && !ctrl.isDisposed()) {
			ctrl.getDisplay().asyncExec(new Runnable() {
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
		viewer.getControl().setRedraw(false);
		if (event.isReset()) {
			internalReset();
		} else {
			handleChanges(event);
		}
		viewer.getControl().setRedraw(true);
	}
	
	/**
	 * 
	 */
	protected void internalReset() {
		// On a reset, clear the diff node model and rebuild it. Then refresh the 
		// viewer.
		viewer.remove(getRoot().getChildren());
		clearModelObjects(getRoot());
		resourceMap.clear();
		associateDiffNode(ResourcesPlugin.getWorkspace().getRoot(), getRoot());
		buildTree(getRoot());
		// TODO: Is this refresh redundant?
		((StructuredViewer) viewer).refresh();
	}

	protected void reset() {
		viewer.getControl().setRedraw(false);
		internalReset();
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
		handleResourceChanges(event);
		handleResourceRemovals(event);
		handleResourceAdditions(event);
		updateParentLabels();
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
		IResource[] removedRoots = event.getRemovedRoots();
		if (removedRoots.length == 0) return;
		DiffNode[] nodes = new DiffNode[removedRoots.length];
		for (int i = 0; i < nodes.length; i++) {
			nodes[i] = getModelObject(removedRoots[i]);
			DiffNode node = nodes[i];
			node.getParent().removeToRoot(node);
			unassociateDeeply(removedRoots[i]);
			updateParentLabels(node);
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
		IResource[] added = event.getAddedRoots();
		for (int i = 0; i < added.length; i++) {
			IResource resource = added[i];
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

	protected void addToViewer(SyncInfoDiffNode node) {
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
		root.getSyncInfoSet().removeSyncSetChangedListener(this);
		resourceMap.clear();
	}
	/**
	 * @return Returns the root.
	 */
	protected SyncInfoDiffNodeRoot getRoot() {
		return root;
	}
	
	protected void clearModelObjects(DiffNode node) {
		IDiffElement[] children = node.getChildren();
		for (int i = 0; i < children.length; i++) {
			IDiffElement element = children[i];
			if(element instanceof DiffNode) {
				clearModelObjects((DiffNode)element);
			}
		}
		IDiffContainer parent = node.getParent();
		if(parent != null) {
			parent.removeToRoot(node);
		}
	}
}
