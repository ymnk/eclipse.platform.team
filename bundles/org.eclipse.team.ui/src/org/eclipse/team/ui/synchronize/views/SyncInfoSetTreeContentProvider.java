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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.team.core.subscribers.ISyncInfoSetChangeEvent;
import org.eclipse.team.core.subscribers.SyncInfo;

/**
 * This class provides the contents for a <code>AbstractTreeViewer</code> using the
 * <code>SyncInfo</code> contained in a <code>SyncInfoSet</code> as the model
 */
public class SyncInfoSetTreeContentProvider extends SyncInfoSetContentProvider implements ITreeContentProvider {
	
	// parents who need a label update accumulated while handling sync set changes
	private Set parentsToUpdate = new HashSet();
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ccvs.syncviews.views.SyncSetContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object element) {
		IResource resource = getResource(element);
		IResource[] children;
		if (resource != null) {
			children = getSyncInfoSet().members(resource);
		} else {
			// TODO: This doesn't make sense!
			children = getSyncInfoSet().members(ResourcesPlugin.getWorkspace().getRoot());
		}
		return getModelObjects(children);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	public Object getParent(Object element) {
		IResource resource = getResource(element);
		if (resource == null) return null;
		IContainer parent = resource.getParent();
		return getModelObject(parent);
	}
	
	/**
	 * Return the <code>AbstractTreeViewer</code> asociated with this content provider
	 * or <code>null</code> if the viewer is not of the proper type.
	 * @return
	 */
	public AbstractTreeViewer getTreeViewer() {
		StructuredViewer viewer = getViewer();
		if (viewer instanceof AbstractTreeViewer) {
			return (AbstractTreeViewer)viewer;
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.views.SyncSetContentProvider#handleChanges(org.eclipse.team.core.subscribers.ISyncInfoSetChangeEvent)
	 */
	protected void handleChanges(ISyncInfoSetChangeEvent event) {
		super.handleChanges(event);
		updateParentLabels();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.views.SyncSetContentProvider#handleResourceChanges(org.eclipse.team.core.subscribers.ISyncInfoSetChangeEvent)
	 */
	protected void handleResourceChanges(ISyncInfoSetChangeEvent event) {
		super.handleResourceChanges(event);
		AbstractTreeViewer tree = getTreeViewer();
		if (tree != null) {
			SyncInfo[] infos = event.getChangedResources();
			for (int i = 0; i < infos.length; i++) {
				SyncInfo info = infos[i];
				updateParentLabels(info.getLocal());
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ccvs.syncviews.views.SyncSetContentProvider#handleResourceAdditions(org.eclipse.team.ccvs.syncviews.views.SyncSetChangedEvent)
	 */
	protected void handleResourceAdditions(ISyncInfoSetChangeEvent event) {
		AbstractTreeViewer tree = getTreeViewer();
		if (tree != null) {
			IResource[] added = event.getAddedRoots();
			// TODO: Should group added roots by their parent
			for (int i = 0; i < added.length; i++) {
				IResource resource = added[i];
				Object parent = getModelObject(resource.getParent());				
				Object element = getModelObject(resource);				
				tree.add(parent, element);
				updateParentLabels(resource);		
			}
		} else {
			super.handleResourceAdditions(event);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ccvs.syncviews.views.SyncSetContentProvider#handleResourceRemovals(org.eclipse.team.ccvs.syncviews.views.SyncSetChangedEvent)
	 */
	protected void handleResourceRemovals(ISyncInfoSetChangeEvent event) {
		AbstractTreeViewer tree = getTreeViewer();
		if (tree != null) {
			IResource[] roots = event.getRemovedRoots();
			if (roots.length == 0) return;
			Object[] modelRoots = new Object[roots.length];
			for (int i = 0; i < modelRoots.length; i++) {
				modelRoots[i] = getModelObject(roots[i]);
				updateParentLabels(roots[i]);
			}
			tree.remove(modelRoots);
		} else {
			super.handleResourceRemovals(event);
		}
	}
	
	/**
	 * Forces the viewer to update the labels for parents whose children have changed
	 * during this round of sync set changes.
	 */
	protected void updateParentLabels() {
		try {
			getViewer().update(
					parentsToUpdate.toArray(new Object[parentsToUpdate.size()]),
					null 
			);	
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
	protected void updateParentLabels(IResource resource) {
		IResource parent = resource.getParent();
		while(parent.getType() != IResource.ROOT) {
			parentsToUpdate.add(getModelObject(parent));
			parent = parent.getParent();
		}
	}
}
