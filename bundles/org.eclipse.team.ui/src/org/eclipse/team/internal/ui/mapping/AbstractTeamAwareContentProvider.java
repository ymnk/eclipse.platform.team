/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.*;
import org.eclipse.team.core.ITeamStatus;
import org.eclipse.team.core.synchronize.*;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.mapping.IResourceMappingScope;
import org.eclipse.team.ui.mapping.ISynchronizationContext;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.IExtensionStateModel;
import org.eclipse.ui.navigator.internal.extensions.ICommonContentProvider;

/**
 * Abstract team aware content provider that delegates to anotehr content provider
 */
public abstract class AbstractTeamAwareContentProvider implements ICommonContentProvider, ISyncInfoSetChangeListener {

	private ModelProvider modelProvider;
	private IResourceMappingScope scope;
	private ISynchronizationContext context;
	private Viewer viewer;

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parentElement) {
		if (parentElement == getModelProvider()) {
			return filter(parentElement, getDelegateContentProvider().getChildren(getModelRoot()));
		}
		return filter(parentElement, getDelegateContentProvider().getChildren(parentElement));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	public Object getParent(Object element) {
		if (element instanceof ModelProvider)
			return null;
		if (element == getModelRoot())
			return null;
		Object parent = getDelegateContentProvider().getParent(element);
		if (parent == getModelRoot())
			return getModelProvider();
		return parent;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	public boolean hasChildren(Object element) {
		return getDelegateContentProvider().hasChildren(element) && filter(element, getChildren(element)).length > 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		if (inputElement == getModelProvider()) {
			return filter(inputElement, getDelegateContentProvider().getChildren(getModelRoot()));
		}
		return filter(inputElement, getDelegateContentProvider().getElements(inputElement));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose() {
		// Nothing to do
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = viewer;
		getDelegateContentProvider().inputChanged(viewer, oldInput, newInput);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.internal.extensions.ICommonContentProvider#init(org.eclipse.ui.navigator.IExtensionStateModel, org.eclipse.ui.IMemento)
	 */
	public void init(IExtensionStateModel aStateModel, IMemento aMemento) {
		scope = (IResourceMappingScope)aStateModel.getProperty(TeamUI.RESOURCE_MAPPING_SCOPE);
		context = (ISynchronizationContext)aStateModel.getProperty(TeamUI.SYNCHRONIZATION_CONTEXT);
		ITreeContentProvider provider = getDelegateContentProvider();
		if (provider instanceof ICommonContentProvider) {
			((ICommonContentProvider) provider).init(aStateModel, aMemento);	
		}
	}

	protected ISynchronizationContext getContext() {
		return context;
	}

	protected IResourceMappingScope getScope() {
		return scope;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.IMementoAware#restoreState(org.eclipse.ui.IMemento)
	 */
	public void restoreState(IMemento aMemento) {
		ITreeContentProvider provider = getDelegateContentProvider();
		if (provider instanceof ICommonContentProvider) {
			((ICommonContentProvider) provider).restoreState(aMemento);	
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.IMementoAware#saveState(org.eclipse.ui.IMemento)
	 */
	public void saveState(IMemento aMemento) {
		ITreeContentProvider provider = getDelegateContentProvider();
		if (provider instanceof ICommonContentProvider) {
			((ICommonContentProvider) provider).saveState(aMemento);	
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.synchronize.ISyncInfoSetChangeListener#syncInfoSetReset(org.eclipse.team.core.synchronize.SyncInfoSet, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void syncInfoSetReset(SyncInfoSet set, IProgressMonitor monitor) {
		// Nothing can be done at this level (i.e. content provider).
		// The controller at the viewer level needs to handle this.
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.synchronize.ISyncInfoSetChangeListener#syncInfoSetErrors(org.eclipse.team.core.synchronize.SyncInfoSet, org.eclipse.team.core.ITeamStatus[], org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void syncInfoSetErrors(SyncInfoSet set, ITeamStatus[] errors, IProgressMonitor monitor) {
		// This should happen infrequently enough that a blanket refresh is acceptable
		refresh();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.synchronize.ISyncInfoSetChangeListener#syncInfoChanged(org.eclipse.team.core.synchronize.ISyncInfoSetChangeEvent, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void syncInfoChanged(ISyncInfoSetChangeEvent event, IProgressMonitor monitor) {
		if (event instanceof ISyncInfoTreeChangeEvent) {
			ISyncInfoTreeChangeEvent treeEvent = (ISyncInfoTreeChangeEvent) event;
			syncStateChanged(treeEvent, monitor);
		}
	}
	
	/**
	 * The set of out-of-sync resources has changed. The changes are described in the event.
	 * This method is invoked by a non-ui thread. By default, this method refreshs the
	 * subtree of this content provider. Subclasses may override.
	 * 
	 * TODO: Should reduce the resources to those that apply to this model.
	 * 
	 * @param event the event that indicates which resources have change sync state
	 * @param monitor a progress monitor
	 */
	protected void syncStateChanged(ISyncInfoTreeChangeEvent event, IProgressMonitor monitor) {
		refresh();
	}

	protected void refresh() {
		Utils.syncExec(new Runnable() {
			public void run() {
				((TreeViewer)getViewer()).refresh(getModelProvider());
			}
		
		}, getViewer().getControl());
	}

	/**
	 * Return the model content provider that the team aware content
	 * provider delegates to.
	 * @return the model content provider
	 */
	protected abstract ITreeContentProvider getDelegateContentProvider();
	
	/**
	 * Filter the obtained children of the given parent so that only the
	 * desired elements are shown.
	 * @param parentElement the parent element
	 * @param children the children
	 * @return the filtered children
	 */
	protected abstract Object[] filter(Object parentElement, Object[] children);
	
	/**
	 * Return the model provider for this content provider.
	 * @return the model provider for this content provider
	 */
	protected abstract ModelProvider getModelProvider();
	
	/**
	 * Return the object that acts as the model root. It is used when getting the children
	 * for a model provider.
	 * @return the object that acts as the model root
	 */
	protected abstract Object getModelRoot();

	protected Viewer getViewer() {
		return viewer;
	}
}
