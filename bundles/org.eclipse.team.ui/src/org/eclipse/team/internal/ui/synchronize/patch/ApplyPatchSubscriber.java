/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.patch;

import java.util.*;

import org.eclipse.compare.internal.core.patch.*;
import org.eclipse.compare.internal.patch.*;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.eclipse.team.internal.core.mapping.LocalResourceVariant;

public class ApplyPatchSubscriber extends Subscriber {
	
	private WorkspacePatcher patcher;
	private IResourceVariantComparator comparator;

	public ApplyPatchSubscriber(WorkspacePatcher patcher) {
		this.patcher = patcher;
		this.comparator = new PatchedFileVariantComparator();
		getPatcher().refresh();
		// FIXME: create instance, singleton 
		PatchWorkspace.create(ResourcesPlugin.getWorkspace().getRoot(), getPatcher());
	}
	
	public String getName() {
		// TODO: change to something like '{0} patch applied'
		return "Apply Patch Subscriber"; //$NON-NLS-1$
	}

	public IResourceVariantComparator getResourceComparator() {
		return comparator;
	}

	public SyncInfo getSyncInfo(IResource resource) throws TeamException {
		FilePatch2[] diffs = getPatcher().getDiffs();
		try {
			IResourceVariant variant = null;
			if (resource.getType() == IResource.FILE) {
				for (int i = 0; i < diffs.length; i++) {
					if (diffs[i] instanceof FilePatch2) {
						DiffProject diffProject = (diffs[i]).getProject();
						IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(diffProject.getName());
						IFile file = project.getFile(diffs[i].getPath(getPatcher().isReversed()));
						if (file.equals(resource)) {
							// null as 'variant' for deletions
							if (diffs[i].getDiffType(patcher.isReversed()) != FilePatch2.DELETION)
								variant =  new PatchedFileVariant(getPatcher(), diffs[i]);
							IResourceVariant base = resource.exists() ?  new LocalResourceVariant(resource) : null;
							SyncInfo info = new SyncInfo(resource, base, variant, getResourceComparator());
							info.init();
							return info;
						}
					}
				}
			}
			return null;
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}
	}

	public boolean isSupervised(IResource resource) throws TeamException {
		// TODO Auto-generated method stub
		System.out.println(">> [true] isSupervised: " + resource.getName()); //$NON-NLS-1$
		return true;
	}

	public IResource[] members(IResource resource) throws TeamException {
		FilePatch2[] diffs = getPatcher().getDiffs();
		try {
			if(resource.getType() == IResource.FILE)
				// file has no members
				return new IResource[0];
			IContainer container = (IContainer) resource;

			// workspace container members
			List existingChildren = new ArrayList(Arrays.asList(container.members()));

			// patch members, subscriber location
			for (int i = 0; i < diffs.length; i++) {
				DiffProject diffProject = diffs[i].getProject();
				IProject project = container.getProject();
				if (project.getName().equals(diffProject.getName())) {
					IResource file = project.getFile(diffs[i].getPath(getPatcher().isReversed()));
					if (!existingChildren.contains(file)) {
						existingChildren.add(file);
					}
				}
			}
			return (IResource[]) existingChildren.toArray(new IResource[existingChildren.size()]);
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}
	}

	public void refresh(IResource[] resources, int depth,
			IProgressMonitor monitor) throws TeamException {
		// TODO Auto-generated method stub
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < resources.length; i++) {
			sb.append(resources[i].getName()).append(","); //$NON-NLS-1$
		}
		System.out
				.println(">> [ignored] ApplyPatchSubscriber.refresh: " + sb.toString()); //$NON-NLS-1$
	}
	
	public IResource[] roots() {
		IDiffElement[] children = PatchWorkspace.getInstance().getChildren();
		Set roots = new HashSet();
		for (int i = 0; i < children.length; i++) {
			if (getPatcher().isWorkspacePatch()) {
				// return array of projects from the patch
				DiffProject diffProject = ((PatchProjectDiffNode)children[i]).getDiffProject();
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(diffProject.getName());
				roots.add(project);
			} else {
				FileDiffResult diffResult = ((PatchFileDiffNode)children[i]).getDiffResult();
				IFile file = ((WorkspaceFileDiffResult)diffResult).getTargetFile();
				roots.add(file);
			}
		}
		return (IResource[]) roots.toArray(new IResource[0]);
	}
	
	WorkspacePatcher getPatcher() {
		return patcher;
	}
}
