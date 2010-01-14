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

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.navigator.*;

public class PatchWorkbenchContentProvider extends BaseWorkbenchContentProvider
		implements IPipelinedTreeContentProvider {
	private boolean isWorkspaceRoot;

	public PatchWorkbenchContentProvider() {
	}

	public Object getParent(Object element) {
		Object parent = super.getParent(element);
		if (isWorkspaceRoot && parent instanceof PatchWorkspace) {
			return ((PatchWorkspace) parent).getResource();
		}
		return parent;
	}

	public void getPipelinedChildren(Object aParent, Set theCurrentChildren) {
		// Nothing to do
	}

	public void getPipelinedElements(Object anInput, Set theCurrentElements) {
		// Replace any model projects with a DiffProject
		// TODO: does it ever happen? the provider is use *only* as delegate in
		// PatchSyncContentProvider
		if (anInput instanceof IWorkspaceRoot) {
			List newProjects = new ArrayList();
			for (Iterator iter = theCurrentElements.iterator(); iter.hasNext();) {
				Object element = iter.next();
				if (element instanceof IProject) {
					IProject project = (IProject) element;
					IDiffElement diffProject = PatchModelProvider
							.getModelObject(project);
					if (diffProject != null) {
						iter.remove();
						newProjects.add(diffProject);
					}
				}
			}
			theCurrentElements.addAll(newProjects);
		}
	}

	public Object getPipelinedParent(Object anObject, Object aSuggestedParent) {
		return aSuggestedParent;
	}

	public PipelinedShapeModification interceptAdd(
			PipelinedShapeModification anAddModification) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean interceptRefresh(
			PipelinedViewerUpdate aRefreshSynchronization) {
		// No need to intercept the refresh
		return false;
	}

	public PipelinedShapeModification interceptRemove(
			PipelinedShapeModification aRemoveModification) {
		// No need to intercept the remove
		return aRemoveModification;
	}

	public boolean interceptUpdate(PipelinedViewerUpdate anUpdateSynchronization) {
		// No need to intercept the update
		return false;
	}

	public void init(ICommonContentExtensionSite aConfig) {
		// TODO Auto-generated method stub

	}

	public void restoreState(IMemento aMemento) {
		// TODO Auto-generated method stub

	}

	public void saveState(IMemento aMemento) {
		// TODO Auto-generated method stub

	}
}
