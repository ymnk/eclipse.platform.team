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

import java.util.*;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.team.core.synchronize.ISyncInfoSetChangeListener;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * This class is reponsible for creating and maintaining a presentation model of 
 * {@link SynchronizeModelElement} elements that can be shown in a viewer. The model
 * is based on the synchronization information contained in the provided {@link SyncInfoSet}.
 * <p>
 * label updates (property propagation to parent nodes)
 * sync change listener (changes, additions, removals, reset)
 * batching busy updates
 * </p>
 * 
 * @see HierarchicalModelProvider
 * @see CompressedFoldersModelProvider
 * @since 3.0
 */
public abstract class SynchronizeModelProvider extends AbstractSynchronizeModelProvider implements ISyncInfoSetChangeListener {
	
	protected Map resourceMap = Collections.synchronizedMap(new HashMap());

    protected static final boolean DEBUG = false;
	
	/**
	 * Create an input based on the provide sync set. The input is not
	 * initialized until <code>prepareInput</code> is called.
	 * @param set
	 *            the sync set used as the basis for the model created by this
	 *            input.
	 */
	public SynchronizeModelProvider(ISynchronizePageConfiguration configuration, SyncInfoSet set) {
		this(new UnchangedResourceModelElement(null, ResourcesPlugin.getWorkspace().getRoot()) {
			/* 
			 * Override to ensure that the diff viewer will appear in CompareEditorInputs
			 */
			public boolean hasChildren() {
				return true;
			}
		}, configuration, set);
	}

	public SynchronizeModelProvider(SynchronizeModelElement parent, ISynchronizePageConfiguration configuration, SyncInfoSet set) {
		super(null, parent, configuration, set);
	}
	
	/**
	 * The provider can try and return a mapping for the provided object. Providers often use mappings
	 * to store the source of a logical element they have created. For example, when displaying resource
	 * based logical elements, a provider will cache the resource -> element mapping for quick retrieval
	 * of the element when resource based changes are made.
	 * 
	 * @param object the object to query for a mapping
	 * @return an object created by this provider that would be shown in a viewer, or <code>null</code>
	 * if the provided object is not mapped by this provider.
	 */
	public Object getMapping(Object object) {
		return resourceMap.get(object);
	}
	
	/**
	 * Dispose of the builder
	 */
	public void dispose() {
		resourceMap.clear();
		super.dispose();
	}

	/**
	 * Returns the sorter for this model provider.
	 * 
	 * @return the sorter for this model provider. 
	 */
	public abstract ViewerSorter getViewerSorter();

	/**
	 * Return the model object (i.e. an instance of <code>SyncInfoModelElement</code>
	 * or one of its subclasses) for the given IResource.
	 * @param resource
	 *            the resource
	 * @return the <code>SyncInfoModelElement</code> for the given resource
	 */
	protected ISynchronizeModelElement getModelObject(IResource resource) {
		return (ISynchronizeModelElement) resourceMap.get(resource);
	}

	/**
	 * For each node create children based on the contents of
	 * @param node
	 * @return
	 */
	protected abstract IDiffElement[] buildModelObjects(ISynchronizeModelElement node);
	
	protected void associateDiffNode(ISynchronizeModelElement node) {
		IResource resource = node.getResource();
		if(resource != null) {
			resourceMap.put(resource, node);
		}
	}

	protected void unassociateDiffNode(IResource resource) {
		resourceMap.remove(resource);
	}

	protected void reset() {
		// save expansion state
		if(! resourceMap.isEmpty()) {
			saveViewerState();
		}
		
		// Clear existing model, but keep the root node
		resourceMap.clear();
		clearModelObjects(getModelRoot());
		// remove all from tree viewer
		IDiffElement[] elements = getModelRoot().getChildren();
		for (int i = 0; i < elements.length; i++) {
			doRemove((ISynchronizeModelElement)elements[i]);
		}
		
		// Rebuild the model
		associateDiffNode(getModelRoot());
		buildModelObjects(getModelRoot());
		
		// Notify listeners that model has changed
		ISynchronizeModelElement root = getModelRoot();
		if(root instanceof SynchronizeModelElement) {
			((SynchronizeModelElement)root).fireChanges();
		}
		TeamUIPlugin.getStandardDisplay().asyncExec(new Runnable() {
			public void run() {
				StructuredViewer viewer = getViewer();
				if (viewer != null && !viewer.getControl().isDisposed()) {
					viewer.refresh();
					//	restore expansion state
					restoreViewerState();
				}
			}
		});
	}
	
	/**
	 * Helper method to remove a resource from the viewer. If the resource
	 * is not mapped to a model element, this is a no-op.
	 * @param resource the resource to remove
	 */
	protected void removeFromViewer(IResource resource) {
		ISynchronizeModelElement element = getModelObject(resource);
		if(element != null) {
			removeFromViewer(element);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.AbstractSynchronizeModelProvider#clearModelObjects(org.eclipse.team.ui.synchronize.ISynchronizeModelElement)
	 */
	protected void clearModelObjects(ISynchronizeModelElement node) {
		super.clearModelObjects(node);
		IResource resource = node.getResource();
		if (resource != null) {
			unassociateDiffNode(resource);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.AbstractSynchronizeModelProvider#addToViewer(org.eclipse.team.ui.synchronize.ISynchronizeModelElement)
	 */
	protected void addToViewer(ISynchronizeModelElement node) {
		associateDiffNode(node);
		super.addToViewer(node);
	}

	protected void saveViewerState() {
		//	save visible expanded elements and selection
	    final StructuredViewer viewer = getViewer();
		if (viewer != null && !viewer.getControl().isDisposed() && viewer instanceof AbstractTreeViewer) {
			final Object[][] expandedElements = new Object[1][1];
			final Object[][] selectedElements = new Object[1][1];
			viewer.getControl().getDisplay().syncExec(new Runnable() {
				public void run() {
					if (viewer != null && !viewer.getControl().isDisposed()) {
						expandedElements[0] = ((AbstractTreeViewer) viewer).getVisibleExpandedElements();
						selectedElements[0] = ((IStructuredSelection) viewer.getSelection()).toArray();
					}
				}
			});
			//
			// Save expansion
			//
			if (expandedElements[0].length > 0) {
				ISynchronizePageConfiguration config = getConfiguration();
				ArrayList savedExpansionState = new ArrayList();
				for (int i = 0; i < expandedElements[0].length; i++) {
					if (expandedElements[0][i] instanceof ISynchronizeModelElement) {
						IResource resource = ((ISynchronizeModelElement) expandedElements[0][i]).getResource();
						if(resource != null)
							savedExpansionState.add(resource.getFullPath().toString());
					}
				}
				config.setProperty(P_VIEWER_EXPANSION_STATE, savedExpansionState);
			}
			//
			// Save selection
			//
			if (selectedElements[0].length > 0) {
				ISynchronizePageConfiguration config = getConfiguration();
				ArrayList savedSelectedState = new ArrayList();
				for (int i = 0; i < selectedElements[0].length; i++) {
					if (selectedElements[0][i] instanceof ISynchronizeModelElement) {
						IResource resource = ((ISynchronizeModelElement) selectedElements[0][i]).getResource();
						if(resource != null)
							savedSelectedState.add(resource.getFullPath().toString());
					}
				}
				config.setProperty(P_VIEWER_SELECTION_STATE, savedSelectedState);
			}
		}
	}
	
	protected void restoreViewerState() {
		// restore expansion state and selection state
	    final StructuredViewer viewer = getViewer();
		if (viewer != null && !viewer.getControl().isDisposed() && viewer instanceof AbstractTreeViewer) {
			List savedExpansionState = (List)getConfiguration().getProperty(P_VIEWER_EXPANSION_STATE);
			List savedSelectionState = (List)getConfiguration().getProperty(P_VIEWER_SELECTION_STATE);
			IContainer container = ResourcesPlugin.getWorkspace().getRoot();
			final ArrayList expandedElements = new ArrayList();
			if (savedExpansionState != null) {
				for (Iterator it = savedExpansionState.iterator(); it.hasNext();) {
					String path = (String) it.next();
					IResource resource = container.findMember(path, true /* include phantoms */);
					ISynchronizeModelElement element = getModelObject(resource);
					if (element != null) {
						expandedElements.add(element);
					}
				}
			}
			final ArrayList selectedElements = new ArrayList();
			if (savedSelectionState != null) {
				for (Iterator it = savedSelectionState.iterator(); it.hasNext();) {
					String path = (String) it.next();
					IResource resource = container.findMember(path, true /* include phantoms */);
					ISynchronizeModelElement element = getModelObject(resource);
					if (element != null) {
						selectedElements.add(element);
					}
				}
			}
			Utils.asyncExec(new Runnable() {
				public void run() {
					((AbstractTreeViewer) viewer).setExpandedElements(expandedElements.toArray());
					viewer.setSelection(new StructuredSelection(selectedElements));
				}
			}, viewer);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.ISynchronizeModelProvider#saveState()
	 */
	public void saveState() {
		saveViewerState();
	}

	protected ISynchronizeModelElement[] getClosestExistingParents(IResource resource) {
		ISynchronizeModelElement element = getModelObject(resource);
		if(element == null) {
			do {
				resource = resource.getParent();
				element = getModelObject(resource);
			} while(element == null && resource != null);
		}
		return new ISynchronizeModelElement[] { element };
	}
}
