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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.ui.synchronize.*;

/**
 * This class provides the contents for a StructuredViewer using a SyncSet as the model
 */
public abstract class SyncSetContentProvider implements IStructuredContentProvider, ISyncSetChangedListener {
	
	protected Viewer viewer;
	
	// parents who need a label update accumulated while handling sync set changes
	private Set parentsToUpdate = new HashSet();
	
	protected SyncInfoSet getSyncSet() {
		if(viewer == null || viewer.getControl().isDisposed()) {
			return null;	
		}
		if(viewer.getInput() instanceof SyncInfoDiffNode) {
			return (SyncInfoSet)((SyncInfoDiffNode)viewer.getInput()).getSyncInfoSet();
		}
		return (SyncInfoSet)viewer.getInput();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		
		this.viewer = v;
		SyncInfoSet oldSyncSet = null;
		SyncInfoSet newSyncSet = null;
		
		if(newInput instanceof SyncInfoDiffNode && oldInput != null) {
			return;
		}
		
		if (oldInput instanceof SyncInfoSet) {
			oldSyncSet = (SyncInfoSet) oldInput;
		}
		if (newInput instanceof SyncInfoSet) {
			newSyncSet = (SyncInfoSet) newInput;
		}
		if (oldSyncSet != newSyncSet) {
			if (oldSyncSet != null) {
				((SyncInfoSet)oldSyncSet).removeSyncSetChangedListener(this);
			}
			if (newSyncSet != null) {
				((SyncInfoSet)newSyncSet).addSyncSetChangedListener(this);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public abstract Object[] getElements(Object inputElement);
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose() {
		SyncInfoSet input = getSyncSet();
		if (input != null) {
			input.removeSyncSetChangedListener(this);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ccvs.syncviews.views.ISyncSetChangedListener#syncSetChanged(org.eclipse.team.ccvs.syncviews.views.SyncSetChangedEvent)
	 */
	public void syncSetChanged(final ISyncInfoSetChangeEvent event) {
		final Control ctrl = viewer.getControl();
		if (ctrl != null && !ctrl.isDisposed()) {
			ctrl.getDisplay().asyncExec(new Runnable() {
				public void run() {
					if (!ctrl.isDisposed()) {
						BusyIndicator.showWhile(ctrl.getDisplay(), new Runnable() {
							public void run() {
								handleSyncSetChanges(event);
							}
						});
					}
				}
			});
		}
	}
	
	/**
	 * Update the viewer with the sync-set changes, aditions and removals
	 * in the given event. This method is invoked from within the UI thread.
	 * @param event
	 */
	protected void handleSyncSetChanges(ISyncInfoSetChangeEvent event) {
		viewer.getControl().setRedraw(false);
		if (event.isReset()) {
			// On a reset, refresh the entire view
			((StructuredViewer) viewer).refresh();
		} else {
			handleResourceChanges(event);
			handleResourceRemovals(event);
			handleResourceAdditions(event);
			updateParentLabels();
		}
		viewer.getControl().setRedraw(true);
	}
	
	/**
	 * Update the viewer for the sync set changes in the provided event.
	 * This method is invoked by <code>handleSyncSetChanges</code>.
	 * Subclasses may override.
	 * @param event
	 * @see #handleSyncSetChanges(SyncSetChangedEvent)
	 */
	protected void handleResourceChanges(ISyncInfoSetChangeEvent event) {
		// Refresh the viewer for each changed resource
		SyncInfo[] infos = event.getChangedResources();
		for (int i = 0; i < infos.length; i++) {
			IResource local = infos[i].getLocal();
			((StructuredViewer) viewer).refresh(getModelObject(local), true);
			updateParentLabels(local);
		}
	}
	
	/**
	 * Update the viewer for the sync set removals in the provided event.
	 * This method is invoked by <code>handleSyncSetChanges</code>.
	 * Subclasses may override.
	 * @param event
	 */
	protected void handleResourceRemovals(ISyncInfoSetChangeEvent event) {
		// Update the viewer for each removed resource
		IResource[] removed = event.getRemovedRoots();
		for (int i = 0; i < removed.length; i++) {
			IResource resource = removed[i];
			((StructuredViewer) viewer).refresh(getModelObject(resource));
			updateParentLabels(resource);
		}
	}
	
	/**
	 * Update the viewer for the sync set additions in the provided event.
	 * This method is invoked by <code>handleSyncSetChanges</code>.
	 * Subclasses may override.
	 * @param event
	 */
	protected void handleResourceAdditions(ISyncInfoSetChangeEvent event) {
		// Update the viewer for each of the added resource's parents
		IResource[] added = event.getAddedRoots();
		for (int i = 0; i < added.length; i++) {
			IResource resource = added[i];
			((StructuredViewer) viewer).refresh(getModelObject(resource.getParent()));
			updateParentLabels(resource);
		}
	}
	
	public StructuredViewer getViewer() {
		return (StructuredViewer)viewer;
	}
	
	/**
	 * Return the children of the given container who are either out-of-sync or contain
	 * out-of-sync resources.
	 */
	public Object[] members(IResource resource) {
		IResource[] resources = getSyncSet().members(resource);
		Object[] result = new Object[resources.length];
		for (int i = 0; i < resources.length; i++) {
			IResource child = resources[i];
			result[i] = getModelObject(child);
		}
		return result;
	}
	
	/**
	 * Return the SyncInfo for the given model object that was returned by 
	 * SyncSet#members(IResource). If syncSet is null, then the 
	 * sync info will also be null.
	 * 
	 * @param element
	 * @return
	 */
	public static SyncInfo getSyncInfo(Object element) {
		if (element instanceof SyncInfo) {
			return ((SyncInfo) element);
		}  else if (element instanceof SyncInfoDiffNode) {
			SyncInfoDiffNode syncResource = (SyncInfoDiffNode)element;
			return syncResource.getSyncInfo();
		}
		throw new NullPointerException();
	}
	
	/**
	 * Return the IResource for the given model object that was returned by 
	 * SyncSet#members(IResource). Return <code>null</code> if the given
	 * object does not have a corresponding IResource.
	 * 
	 * @param element
	 * @return
	 */
	public static IResource getResource(Object obj) {
		if (obj instanceof SyncInfo) {
			return ((SyncInfo) obj).getLocal();
		}  else if (obj instanceof SyncInfoDiffNode) {
			return ((SyncInfoDiffNode)obj).getResource();
		}
		return null;
	}
	
	/**
	 * Return the sync kind for the given model object that was returned by 
	 * SyncSet#members(IResource). If syncSet is null, then the 
	 * sync kind for SyncContainers will always be 0.
	 * 
	 * @param element
	 * @return
	 */
	public static int getSyncKind(Object element) {
		SyncInfo info = getSyncInfo(element);
		if (info != null) {
			return info.getKind();
		}
		return SyncInfo.IN_SYNC;
	}
	
	/**
	 * Get the model object (SyncSet, SyncInfo or SyncContainer) that is the
	 * parent of the given model object.
	 * 
	 * @param syncSet
	 * @param object
	 * @return
	 */
	public Object getParent(Object object) {
		IResource resource = getResource(object);
		if (resource == null) return null;
		IContainer parent = resource.getParent();
		return getModelObject(parent);
	}
	
	/**
	 * Return the model object for the given IResource.
	 * @param resource
	 */
	public Object getModelObject(IResource resource) {
		if (resource.getType() == IResource.ROOT) {
			return getSyncSet();
		} else {
			return new SyncInfoDiffNode(getSyncSet(), resource);
		}
	}
	
	protected Object[] getModelObjects(IResource[] resources) {
		Object[] result = new Object[resources.length];
		for (int i = 0; i < resources.length; i++) {
			result[i] = getModelObject(resources[i]);
		}
		return result;
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