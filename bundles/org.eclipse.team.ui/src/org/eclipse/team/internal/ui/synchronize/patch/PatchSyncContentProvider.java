/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.patch;

import java.util.*;

import org.eclipse.compare.internal.patch.PatchDiffNode;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.mapping.SynchronizationResourceMappingContext;
import org.eclipse.team.ui.mapping.SynchronizationContentProvider;
import org.eclipse.ui.navigator.*;

public class PatchSyncContentProvider extends SynchronizationContentProvider
		implements IPipelinedTreeContentProvider {

	private PatchWorkbenchContentProvider delegate;

	public PatchSyncContentProvider() {
		super();
	}

	public void init(ICommonContentExtensionSite site) {
		super.init(site);
		delegate = new PatchWorkbenchContentProvider(/*getPatcher()*/);
		// delegate.init(site);
	}

	public void dispose() {
		super.dispose();
		if (delegate != null)
			delegate.dispose();
	}

	protected ITreeContentProvider getDelegateContentProvider() {
		return delegate;
	}

	protected String getModelProviderId() {
		return PatchModelProvider.ID;
	}
	
	protected Object getModelRoot() {
		return PatchWorkspace.getInstance();
	}

	/*
	 * Copied from
	 * org.eclipse.team.examples.model.ui.mapping.ModelSyncContentProvider
	 * .getTraversals(ISynchronizationContext, Object)
	 */
	protected ResourceTraversal[] getTraversals(
			ISynchronizationContext context, Object object) {
		if (object instanceof IDiffElement) {
			ResourceMapping mapping = PatchModelProvider.getResourceMapping((IDiffElement) object);
			ResourceMappingContext rmc = new SynchronizationResourceMappingContext(
					context);
			try {
				return mapping.getTraversals(rmc, new NullProgressMonitor());
			} catch (CoreException e) {
				TeamUIPlugin.log(e);
			}
		}
		return new ResourceTraversal[0];
	}

	protected Object[] getChildrenInContext(ISynchronizationContext context,
			Object parent, Object[] children) {
		// TODO Auto-generated method stub
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < children.length; i++) {
			sb.append(children[i].toString()).append(","); //$NON-NLS-1$
		}
		System.out
				.println(">> [super] PatchSyncContentProvider.getChildrenInContext: context-> " + context + "; parent-> " + parent.toString() + "; children-> " + sb.toString()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return super.getChildrenInContext(context, parent, children);
	}

	public void getPipelinedChildren(Object aParent, Set theCurrentChildren) {
		// Nothing to do
	}

	public void getPipelinedElements(Object anInput, Set theCurrentElements) {
		if (anInput instanceof ISynchronizationContext) {
			// Do not show hunks when all models are visible
			//XXX
			return;
		} else if (anInput == getModelProvider()) {
			List newProjects = new ArrayList();
			for (Iterator iter = theCurrentElements.iterator(); iter.hasNext();) {
				Object element = iter.next();
				IResource[] resources = Utils.getContributedResources(new Object[] {element});
				//TODO: what about the rest?
				IResource resource = resources[0];
				if (resource instanceof IProject) {
					IProject project = (IProject) resource;
					IDiffElement diffProject = PatchModelProvider.getModelObject(project/*, getPatcher()*/);
					if (diffProject != null) {
						iter.remove();
						newProjects.add(diffProject);
					}
				}
			}
			theCurrentElements.addAll(newProjects);
		} else if (anInput instanceof ISynchronizationScope) {
			// When the root is a scope, we should return
			// our model provider so all model providers appear
			// at the root of the viewer.
			theCurrentElements.add(getModelProvider());
		}
	}
	
	public Object getPipelinedParent(Object anObject, Object aSuggestedParent) {
		// TODO Auto-generated method stub
		System.out
				.println(">> [aSuggestedParent] PatchSyncContentProvider.getPipelinedParent: aSuggestedParent-> " + aSuggestedParent); //$NON-NLS-1$
		return aSuggestedParent;
	}

	public PipelinedShapeModification interceptAdd(
			PipelinedShapeModification anAddModification) {
		// TODO Auto-generated method stub
		System.out
				.println(">> [null] PatchSyncContentProvider.interceptAdd: anAddModification-> " + anAddModification); //$NON-NLS-1$
		return null;
	}

	public boolean interceptRefresh(
			PipelinedViewerUpdate aRefreshSynchronization) {
		// No need to intercept the refresh
		return false;
	}

	public PipelinedShapeModification interceptRemove(
			PipelinedShapeModification aRemoveModification) {
		return aRemoveModification;
	}

	public boolean interceptUpdate(PipelinedViewerUpdate anUpdateSynchronization) {
		// No need to intercept the update
		System.out
				.println(">> [false] PatchSyncContentProvider.interceptUpdate: anUpdateSynchronization-> " + anUpdateSynchronization); //$NON-NLS-1$
		return false;
	}

	protected boolean isInScope(ISynchronizationScope scope, Object parent,
			Object element) {
		if (element instanceof PatchDiffNode) {
			final IResource resource = PatchModelProvider
					.getResource((PatchDiffNode) element);
			if (resource == null)
				return false;
			if (scope.contains(resource))
				return true;
		}
		return false;
	}
}
