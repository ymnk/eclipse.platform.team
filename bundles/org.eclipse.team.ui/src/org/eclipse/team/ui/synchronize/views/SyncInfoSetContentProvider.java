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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.internal.core.Assert;
import org.eclipse.team.ui.synchronize.SyncInfoDiffNode;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;

/**
 * This class provides the contents for a StructuredViewer using the <code>SyncInfo</code>
 * contained in a <code>SyncInfoSet</code> as the model.
 * 
 * @see SyncInfo
 * @see SyncInfoSet
 */
public abstract class SyncInfoSetContentProvider extends BaseWorkbenchContentProvider implements IStructuredContentProvider, ISyncSetChangedListener {
	
	private Viewer viewer;
	
	/**
	 * Return the <code>SyncInfoSet</code> associated with the given object.
	 * The default implementation will extract the set from a <code>SyncInfoDiffNode</code>.
	 * Objects that implement <code>IAdaptable</code> will be queried for an adapter
	 * for the <code>SyncInfoSet</code> class. Subclasses may override to extract
	 * a <code>SyncInfoSet</code> from other model object types but should invoke
	 * the inherited method if extraction for their type fails.
	 * 
	 * @param input the object from which to obtain the <code>SyncInfoSet</code>
	 * @return the <code>SyncInfoSet</code> or <code>null</code> if no set could be obtained
	 */
	protected SyncInfoSet getSyncInfoSet(Object input) {
		if (input == null) {
			return null;
		}
		if(input instanceof SyncInfoDiffNode) {
			return ((SyncInfoDiffNode)input).getSyncInfoSet();
		}
		return null;
	}
	
	/**
	 * Return the <code>SyncInfoSet</code> of the viewer associated with this
	 * content provider.
	 * @return the <code>SyncInfoSet</code> of the viewer
	 */
	protected SyncInfoSet getSyncInfoSet() {
		if(viewer == null || viewer.getControl().isDisposed()) {
			return null;	
		}
		return getSyncInfoSet(viewer.getInput());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		this.viewer = v;
		SyncInfoSet oldSyncSet = getSyncInfoSet(oldInput);
		SyncInfoSet newSyncSet = getSyncInfoSet(newInput);
		if (oldSyncSet != newSyncSet) {
			if (oldSyncSet != null) {
				oldSyncSet.removeSyncSetChangedListener(this);
			}
			if (newSyncSet != null) {
				newSyncSet.addSyncSetChangedListener(this);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose() {
		SyncInfoSet input = getSyncInfoSet();
		if (input != null) {
			input.removeSyncSetChangedListener(this);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ccvs.syncviews.views.ISyncSetChangedListener#syncSetChanged(org.eclipse.team.ccvs.syncviews.views.SyncSetChangedEvent)
	 */
	public void syncSetChanged(final ISyncInfoSetChangeEvent event, IProgressMonitor monitor) {
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
			// On a reset, refresh the entire view
			((StructuredViewer) viewer).refresh();
		} else {
			handleChanges(event);
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
		handleResourceChanges(event);
		handleResourceRemovals(event);
		handleResourceAdditions(event);
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
			((StructuredViewer) viewer).refresh(getModelObject(local), true);
		}
	}
	
	/**
	 * Update the viewer for the sync set removals in the provided event.
	 * This method is invoked by <code>handleChanges(ISyncInfoSetChangeEvent)</code>.
	 * Subclasses may override.
	 * @param event
	 */
	protected void handleResourceRemovals(ISyncInfoSetChangeEvent event) {
		// Update the viewer for each removed resource
		IResource[] removed = event.getRemovedRoots();
		for (int i = 0; i < removed.length; i++) {
			IResource resource = removed[i];
			((StructuredViewer) viewer).refresh(getModelObject(resource));
		}
	}
	
	/**
	 * Update the viewer for the sync set additions in the provided event.
	 * This method is invoked by <code>handleChanges(ISyncInfoSetChangeEvent)</code>.
	 * Subclasses may override.
	 * @param event
	 */
	protected void handleResourceAdditions(ISyncInfoSetChangeEvent event) {
		// Update the viewer for each of the added resource's parents
		IResource[] added = event.getAddedRoots();
		for (int i = 0; i < added.length; i++) {
			IResource resource = added[i];
			((StructuredViewer) viewer).refresh(getModelObject(resource.getParent()));
		}
	}
	
	/**
	 * Return the viewer to which this content provider is associated.
	 * @return the content provider's viewer
	 */
	public StructuredViewer getViewer() {
		return (StructuredViewer)viewer;
	}
	
	/**
	 * Return the model object (i.e. an instance of <code>SyncInfoDiffNode</code> or one of its subclasses) 
	 * for the given IResource.
	 * 
	 * @param resource the resource
	 * @return the <code>SyncInfoDiffNode</code> for the given resource
	 */
	public Object getModelObject(IResource resource) {
		return new SyncInfoDiffNode(getSyncInfoSet(), resource);
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
	 * Return the SyncInfo for the given model object 
	 * (i.e. <code>SyncInfoDiffNode</code>). This method wiull return <code>null</code>
	 * if the <code>SuncInfo</code> associated with the given object is null. However,
	 * it is an error to invoke this method with <code>null</code> as an argument or with an object
	 * that does not have an associated <code>SyncInfo</code> as an argument.
	 * 
	 * @param element the model object
	 * @return the <code>SyncInfo</code> associated with the modle object or <code>null</code>
	 */
	public static SyncInfo getSyncInfo(Object element) {
		if (element instanceof SyncInfo) {
			return ((SyncInfo) element);
		}
		if (element instanceof SyncInfoDiffNode) {
			return ((SyncInfoDiffNode)element).getSyncInfo();
		}
		if (element instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable)element;
			return (SyncInfo)adaptable.getAdapter(SyncInfo.class);
		}
		Assert.isTrue(false, "Provided object must have an associated SyncInfo"); //$NON-NLS-1$
		// This point is never reached
		return null;
	}
	
	/**
	 * Return the IResource for the given model object (i.e. <code>SyncInfoDiffNode</code>). 
	 * Return <code>null</code> if the given
	 * object does not have a corresponding IResource.
	 * 
	 * @param element the modle object
	 * @return the corresponding <code>IResource</code>
	 */
	public static IResource getResource(Object element) {
		if (element instanceof SyncInfo) {
			return ((SyncInfo) element).getLocal();
		}  else if (element instanceof SyncInfoDiffNode) {
			return ((SyncInfoDiffNode)element).getResource();
		}
		return null;
	}
	
	/**
	 * Return the sync kind for the given model object (i.e. <code>SyncInfoDiffNode</code>). 
	 * This method uses <code>getSyncInfo(Object)</code> to obtain the sync info for the object.
	 * The kind associated with the sync info is returned unless he sync info is <code>null</code>
	 * in which case <code>SyncInfo.IN_SYNC</code> is eturned.
	 * 
	 * @param element the model object
	 * @return the sync kind of the object
	 */
	public static int getSyncKind(Object element) {
		SyncInfo info = getSyncInfo(element);
		if (info != null) {
			return info.getKind();
		}
		return SyncInfo.IN_SYNC;
	}
}