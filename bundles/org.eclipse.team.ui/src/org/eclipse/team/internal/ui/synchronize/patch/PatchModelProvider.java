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

import org.eclipse.compare.internal.core.patch.DiffProject;
import org.eclipse.compare.internal.core.patch.FilePatch2;
import org.eclipse.compare.internal.patch.*;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.internal.core.TeamPlugin;

public class PatchModelProvider extends ModelProvider {

	public static final String ID = "org.eclipse.team.ui.patchModelProvider"; //$NON-NLS-1$
	private static PatchModelProvider provider;

	public static PatchModelProvider getProvider() {
		if (provider == null) {
			try {
				provider = (PatchModelProvider) ModelProvider
						.getModelProviderDescriptor(PatchModelProvider.ID)
						.getModelProvider();
			} catch (CoreException e) {
				TeamPlugin.log(e);
			}
		}
		return provider;
	}

	/*
	public ResourceMapping[] getMappings(IResource resource,
			ResourceMappingContext context, IProgressMonitor monitor)
			throws CoreException {
		if (context instanceof SubscriberResourceMappingContext) {
			SubscriberResourceMappingContext srmc = (SubscriberResourceMappingContext) context;
			if (resource instanceof IProject) {
				IProject[] projects = srmc.getProjects();
				for (int i = 0; i < projects.length; i++) {
					if (projects[i].equals(resource)) {
						IDiffElement object = createModelObject(resource);
						if (object != null)
							return new ResourceMapping[] { getResourceMapping(object) };
					}
				}
			}
		}
		return super.getMappings(resource, context, monitor);
	}*/

	static ResourceMapping getResourceMapping(IDiffElement object) {
		if (object instanceof PatchProjectDiffNode) {
			return new DiffProjectResourceMapping(
					((PatchProjectDiffNode) object).getDiffProject());
		} else if (object instanceof PatchFileDiffNode) {
			return new FilePatchResourceMapping(((PatchFileDiffNode) object)
					.getDiffResult());
		} else if (object instanceof HunkDiffNode) {
			return new HunkResourceMapping(((HunkDiffNode) object)
					.getHunkResult());
		}
		return null;
	}
	
	/*
	static IDiffElement getModelObject(IResource resource, Subscriber subscriber) {
		PatchWorkspace pw = getPatchWorkspace(subscriber);
		if (pw != null) {
			IDiffElement[] children = pw.getChildren();
			switch (resource.getType()) {
			case IResource.PROJECT: {
				for (int i = 0; i < children.length; i++) {
					if (((PatchProjectDiffNode) children[i]).getDiffProject()
							.getName().equals(resource.getName()))
						return children[i];
				}
			}
			case IResource.FILE: {
				for (int i = 0; i < children.length; i++) {
					IDiffElement[] c = ((PatchProjectDiffNode) children[i])
							.getChildren();
					for (int j = 0; j < c.length; j++) {
						FileDiffResult diffResult = ((PatchFileDiffNode) c[j])
							.getDiffResult();
						IFile file = ((WorkspaceFileDiffResult) diffResult)
							.getTargetFile();
						if (resource.equals(file)) {
							return c[j];
						}
					}
				}
			}
			}
		}
		return null;
	}*/

	/**
	 * Returns the resource associated with the corresponding model element.
	 *
	 * @param element
	 *            the model element
	 * @return the associated resource, or <code>null</code>
	 */
	static IResource getResource(PatchDiffNode element) {
		IResource resource= null;
		if (element instanceof PatchProjectDiffNode) {
			return ((PatchProjectDiffNode) element).getResource();
		} else if (element instanceof PatchFileDiffNode) {
			return ((PatchFileDiffNode) element).getResource();
		} else if (element instanceof HunkDiffNode) {
			return ((HunkDiffNode) element).getResource();
		} /*else if (element instanceof IResource) {
			resource= (IResource) element;
		} else if (element instanceof IAdaptable) {
			final IAdaptable adaptable= (IAdaptable) element;
			final Object adapted= adaptable.getAdapter(IResource.class);
			if (adapted instanceof IResource)
				resource= (IResource) adapted;
		} else {
			final Object adapted= Platform.getAdapterManager().getAdapter(element, IResource.class);
			if (adapted instanceof IResource)
				resource= (IResource) adapted;
		}*/
		return resource;
	}

	static Object/*DiffProject or FilePatch2*/ getPatchObject(IResource resource, WorkspacePatcher patcher) {
		switch (resource.getType()) {
		case IResource.PROJECT: {
			if (patcher.isWorkspacePatch()) {
				DiffProject[] diffProjects = patcher.getDiffProjects();
				for (int i = 0; i < diffProjects.length; i++) {
					if (diffProjects[i].getName().equals(resource.getName()))
						return diffProjects[i];
				}
			} else {
				// TODO:
			}
		}
		case IResource.FILE: {
			FilePatch2[] diffs = patcher.getDiffs();
			for (int i = 0; i < diffs.length; i++) {
				if (resource.equals(getFile(diffs[i], patcher))) {
					return diffs[i];
				}
			}
		}
		}
		return null;
	}
	
	static IFile getFile(FilePatch2 diff, WorkspacePatcher patcher) {
		IProject project = null;
		if (patcher.isWorkspacePatch()) {
			DiffProject diffProject = (diff).getProject();
			project = ResourcesPlugin.getWorkspace().getRoot().getProject(diffProject.getName());
			if (project.getName().equals(diffProject.getName())) {
				return project.getFile(diff.getPath(patcher.isReversed()));
			}
		} else {
			project = patcher.getTarget().getProject();
			if (project.getName().equals(patcher.getTarget().getProject().getName())) {
				return project.getFile(diff.getPath(patcher.isReversed()));
			}
		}
		return project.getFile(diff.getPath(patcher.isReversed()));
	}

	public static PatchWorkspace getPatchWorkspace(Subscriber subscriber) {
		if (subscriber instanceof ApplyPatchSubscriber) {
			ApplyPatchSubscriber aps = (ApplyPatchSubscriber) subscriber;
			return new PatchWorkspace(aps.getPatcher());
		}
		// TODO: assertion?
		return null;
	}
}
