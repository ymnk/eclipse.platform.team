/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.*;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.team.core.ITeamStatus;
import org.eclipse.team.core.synchronize.*;
import org.eclipse.team.internal.core.Assert;
import org.eclipse.team.internal.core.TeamPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;


/**
 * This class is reponsible for creating and maintaining a presentation model of 
 * {@link SynchronizeModelElement} elements that can be shown in a viewer. The model
 * is based on the synchronization information contained in the provided {@link SyncInfoSet}.
 */
public abstract class AbstractSynchronizeModelProvider implements ISynchronizeModelProvider, ISyncInfoSetChangeListener {
    
	// The viewer this input is being displayed in
	private StructuredViewer viewer;
	
	protected SynchronizeModelElement root;
	
	private ISynchronizePageConfiguration configuration;
	
	private SyncInfoSet set;
	
	private SynchronizeModelUpdateHandler updateHandler;
	
	private boolean disposed = false;
	
	protected AbstractSynchronizeModelProvider(AbstractSynchronizeModelProvider parentProvider, SynchronizeModelElement parentNode, ISynchronizePageConfiguration configuration, SyncInfoSet set) {
		Assert.isNotNull(set);
		Assert.isNotNull(parentNode);
		this.root = parentNode;
		this.set = set;
		this.configuration = configuration;
		if (parentProvider == null) {
		    // The update handler will register for sync change events 
		    // with the sync set when the handler is activated
		    updateHandler = new SynchronizeModelUpdateHandler(this);
		} else {
		    // We will use the parent's update handler and register for changes with the given set
		    updateHandler = parentProvider.updateHandler;
		    set.addSyncSetChangedListener(this);
		}
	}
	
	/**
	 * Return the set that contains the elements this provider is using as
	 * a basis for creating a presentation model. This cannot be null.
	 * 
	 * @return the set that contains the elements this provider is
	 * using as a basis for creating a presentation model.
	 */
	public SyncInfoSet getSyncInfoSet() {
		return set;
	}
	
	/**
	 * Returns the input created by this provider or <code>null</code> if 
	 * {@link #prepareInput(IProgressMonitor)} hasn't been called on this object yet.
	 * 
	 * @return the input created by this provider.
	 */
	public ISynchronizeModelElement getModelRoot() {
		return root;
	}
	
	/**
	 * Return the page configuration for this provider.
	 * 
	 * @return the page configuration for this provider.
	 */
	public ISynchronizePageConfiguration getConfiguration() {
		return configuration;
	}
	
	/**
	 * Return the <code>AbstractTreeViewer</code> associated with this
	 * provider or <code>null</code> if the viewer is not of the proper type.
	 * @return the structured viewer that is displaying the model managed by this provider
	 */
	public StructuredViewer getViewer() {
		return viewer;
	}

	/**
	 * Set the viewer that is being used to display the model created by this
	 * provider.
	 * @param viewer the structured viewer that is displaying the model managed by this provider
	 */
	public void setViewer(StructuredViewer viewer) {
		Assert.isTrue(viewer instanceof AbstractTreeViewer);
		this.viewer = (AbstractTreeViewer) viewer;
	}

	/**
	 * Builds the viewer model based on the contents of the sync set.
	 */
	public ISynchronizeModelElement prepareInput(IProgressMonitor monitor) {
		// Connect to the sync set which will register us as a listener and give us a reset event
		// in a background thread
	    if (isRootProvider()) {
	        updateHandler.connect(monitor);
	    } else {
	        getSyncInfoSet().connect(this, monitor);
	    }
		return getModelRoot();
	}
	
	/**
	 * Calculate the problem marker that should be shown on the given 
	 * element. The returned property can be either
	 * ISynchronizeModelElement.PROPAGATED_ERROR_MARKER_PROPERTY or
	 * ISynchronizeModelElement.PROPAGATED_WARNING_MARKER_PROPERTY.
	 * @param element a synchronize model element
	 * @return the marker property that should be displayed on the element
	 * or <code>null</code> if no marker should be displayed
	 */
	public String calculateProblemMarker(ISynchronizeModelElement element) {
		IResource resource = element.getResource();
		String property = null;
		if (resource != null && resource.exists()) {
			try {
				IMarker[] markers = resource.findMarkers(IMarker.PROBLEM, true, getLogicalModelDepth(resource));
				for (int i = 0; i < markers.length; i++) {
					IMarker marker = markers[i];
					try {
						Integer severity = (Integer) marker.getAttribute(IMarker.SEVERITY);
						if (severity != null) {
							if (severity.intValue() == IMarker.SEVERITY_ERROR) {
								property = ISynchronizeModelElement.PROPAGATED_ERROR_MARKER_PROPERTY;
								break;
							} else if (severity.intValue() == IMarker.SEVERITY_WARNING) {
								property = ISynchronizeModelElement.PROPAGATED_WARNING_MARKER_PROPERTY;
								// Keep going because there may be errors on other resources
							}
						}
					} catch (CoreException e) {
						if (!resource.exists()) {
							// The resource was deleted concurrently. Forget any previously found property
							property = null;
							break;
						}
						// If the marker exists, log the exception and continue.
						// Otherwise, just ignore the exception and keep going
						if (marker.exists()) {
							TeamPlugin.log(e);
						}
					}
				}
			} catch (CoreException e) {
				// If the resource exists (is accessible), log the exception and continue.
				// Otherwise, just ignore the exception
				if (resource.isAccessible() 
						&& e.getStatus().getCode() != IResourceStatus.RESOURCE_NOT_FOUND
						&& e.getStatus().getCode() != IResourceStatus.PROJECT_NOT_OPEN) {
					TeamPlugin.log(e);
				}
			}
		}
		return property;
	}
    
	/**
	 * Return the logical model depth used for marker propogation
	 * @param resource the resoure
	 * @return the depth the resources should be traversed
	 */
	protected int getLogicalModelDepth(IResource resource) {
		return IResource.DEPTH_INFINITE;
	}
	
	/**
	 * Update the label of the given diff node. The label for nodes queued 
	 * using this method will not be updated until <code>firePendingLabelUpdates</code>
	 * is called.
	 * @param diffNode the diff node to be updated
	 */
	protected void queueForLabelUpdate(ISynchronizeModelElement diffNode) {
		updateHandler.queueForLabelUpdate(diffNode);
	}
    
    /**
     * Throw away any old state associated with this provider and
     * rebuild the model from scratch.
     */
	protected abstract void reset();
	
	/**
	 * Method invoked when a sync element is added or removed or its state changes.
	 * This method can be invoked from the UI thread or a background thread.
	 * @param element synchronize element
	 * @param clear <code>true</code> if the conflict bit of the element was cleared 
	 * (i.e. the element has been deleted)
	 */
	protected void propogateConflictState(ISynchronizeModelElement element, boolean clear) {
		boolean isConflict = clear ? false : isConflicting(element);
		boolean wasConflict = element.getProperty(ISynchronizeModelElement.PROPAGATED_CONFLICT_PROPERTY);
		// Only propogate and update parent labels if the state of the element has changed
		if (isConflict != wasConflict) {
			element.setPropertyToRoot(ISynchronizeModelElement.PROPAGATED_CONFLICT_PROPERTY, isConflict);
			updateHandler.updateParentLabels(element);
		}
	}
	
	/**
	 * Return whether the given model element represets a conflict.
	 * @param element the element being tested
	 * @return
	 */
	protected boolean isConflicting(ISynchronizeModelElement element) {
		return (element.getKind() & SyncInfo.DIRECTION_MASK) == SyncInfo.CONFLICTING;
	}
	
	/**
	 * Dispose of the provider
	 */
	public void dispose() {
	    // Only dispose the update handler if it is
	    // directly associated with this provider
	    if (isRootProvider()) {
	        updateHandler.dispose();
	    } else {
	        set.removeSyncSetChangedListener(this);
	    }
		this.disposed = true;
	}
	
    private boolean isRootProvider() {
        return updateHandler.getProvider() == this;
    }

    /**
	 * Return whether this provide has been disposed.
     * @return whether this provide has been disposed
     */
	public boolean isDisposed() {
        return disposed;
    }

    /**
     * Return the closest parent elements that represents a model element that
     * could contains the given resource. Multiple elements need only be returned
     * if two or more logical views are being shown and each view has an element
     * that could contain the resource.
     * @param resource the resource
     * @return one or more lowest level parents that could contain the resource
     */
    protected abstract ISynchronizeModelElement[] getClosestExistingParents(IResource resource);
    
	/**
	 * Handle the changes made to the viewer's <code>SyncInfoSet</code>.
	 * This method delegates the changes to the three methods <code>handleResourceChanges(ISyncInfoSetChangeEvent)</code>,
	 * <code>handleResourceRemovals(ISyncInfoSetChangeEvent)</code> and
	 * <code>handleResourceAdditions(ISyncInfoSetChangeEvent)</code>.
	 * @param event
	 *            the event containing the changed resourcses.
	 */
	protected final void handleChanges(ISyncInfoTreeChangeEvent event) {
		handleResourceChanges(event);
		handleResourceRemovals(event);
		handleResourceAdditions(event);
	}

    /**
	 * Update the viewer for the sync set additions in the provided event. This
	 * method is invoked by <code>handleChanges(ISyncInfoSetChangeEvent)</code>.
	 * Subclasses may override.
	 * @param event
	 */
	protected abstract void handleResourceAdditions(ISyncInfoTreeChangeEvent event);

	/**
	 * Update the viewer for the sync set changes in the provided event. This
	 * method is invoked by <code>handleChanges(ISyncInfoSetChangeEvent)</code>.
	 * Subclasses may override.
	 * @param event
	 */
	protected abstract void handleResourceChanges(ISyncInfoTreeChangeEvent event);

	/**
	 * Update the viewer for the sync set removals in the provided event. This
	 * method is invoked by <code>handleChanges(ISyncInfoSetChangeEvent)</code>.
	 * Subclasses may override.
	 * @param event
	 */
	protected abstract void handleResourceRemovals(ISyncInfoTreeChangeEvent event);
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.synchronize.ISyncInfoSetChangeListener#syncInfoChanged(org.eclipse.team.core.synchronize.ISyncInfoSetChangeEvent, org.eclipse.core.runtime.IProgressMonitor)
	 */
    public void syncInfoChanged(ISyncInfoSetChangeEvent event, IProgressMonitor monitor) {
		if (! (event instanceof ISyncInfoTreeChangeEvent)) {
			reset();
		} else {
		    handleChanges((ISyncInfoTreeChangeEvent)event);
		}
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.core.synchronize.ISyncInfoSetChangeListener#syncInfoSetErrors(org.eclipse.team.core.synchronize.SyncInfoSet, org.eclipse.team.core.ITeamStatus[], org.eclipse.core.runtime.IProgressMonitor)
     */
    public void syncInfoSetErrors(SyncInfoSet set, ITeamStatus[] errors, IProgressMonitor monitor) {
        // Not handled

    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.core.synchronize.ISyncInfoSetChangeListener#syncInfoSetReset(org.eclipse.team.core.synchronize.SyncInfoSet, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void syncInfoSetReset(SyncInfoSet set, IProgressMonitor monitor) {
        reset();
    }
    
	protected void addToViewer(ISynchronizeModelElement node) {
	    updateHandler.nodeAdded(node);
		propogateConflictState(node, false);
		// Set the marker property on this node.
		// There is no need to propogate this to the parents 
		// as they will be displaying the proper marker already
		String property = calculateProblemMarker(node);
		if (property != null) {
			node.setProperty(property, true);
		}
		if (Utils.canUpdateViewer(getViewer())) {
			doAdd((SynchronizeModelElement)node.getParent(), node);
		}
	}
	
	/**
	 * Remove any traces of the model element and any of it's descendants in the
	 * hiearchy defined by the content provider from the content provider and
	 * the viewer it is associated with.
	 * @param node the model element to remove
	 */
	protected void removeFromViewer(ISynchronizeModelElement node) {
		propogateConflictState(node, true /* clear the conflict */);
		clearModelObjects(node);
		if (Utils.canUpdateViewer(getViewer())) {
			doRemove(node);
		}
	}
	
	/**
	 * Clear the model objects from the diff tree, cleaning up any cached state
	 * (such as resource to model object map). This method recurses deeply on
	 * the tree to allow the cleanup of any cached state for the children as
	 * well.
	 * @param node the root node
	 */
	protected void clearModelObjects(ISynchronizeModelElement node) {
		IDiffElement[] children = node.getChildren();
		for (int i = 0; i < children.length; i++) {
			IDiffElement element = children[i];
			if (element instanceof ISynchronizeModelElement) {
			    ISynchronizeModelElement sme = (ISynchronizeModelElement) element;
                AbstractSynchronizeModelProvider provider = getProvider(sme);
				provider.clearModelObjects(sme);
			}
		}
		IDiffContainer parent = node.getParent();
		if (parent != null) {
			parent.removeToRoot(node);
		}
	}
	
	/**
	 * Return the provider that created and manages the given
	 * model element. The default is to return the receiver.
	 * Subclasses may override.
     * @param element the synchronizew model element
     * @return the provider that created the element
     */
    protected AbstractSynchronizeModelProvider getProvider(ISynchronizeModelElement element) {
        return this;
    }

    protected abstract void doAdd(ISynchronizeModelElement parent, ISynchronizeModelElement element);
	
	protected abstract void doRemove(ISynchronizeModelElement element);
}
