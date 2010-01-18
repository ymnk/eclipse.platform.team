/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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

import org.eclipse.compare.internal.core.patch.DiffProject;
import org.eclipse.compare.internal.core.patch.FilePatch2;
import org.eclipse.compare.internal.patch.PatchProjectDiffNode;
import org.eclipse.compare.internal.patch.WorkspacePatcher;
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
					if (resource.equals(PatchModelProvider.getFile(diffs[i], getPatcher()))) {
						// null as 'variant' for deletions
						if (diffs[i].getDiffType(patcher.isReversed()) != FilePatch2.DELETION)
							variant =  new PatchedFileVariant(getPatcher(), diffs[i]);
						IResourceVariant base = resource.exists() ?  new LocalResourceVariant(resource) : null;
						SyncInfo info = new SyncInfo(resource, base, variant, getResourceComparator()) {
							protected int calculateKind() throws TeamException {
								// TODO: this will work only for files, what about excluding individual hunks?
								if (!getPatcher().isEnabled(PatchModelProvider.getPatchObject(getLocal(), patcher)))
									return IN_SYNC;
								if (getRemote() != null 
										&& getPatcher().getDiffResult(((PatchedFileVariant)getRemote()).getDiff()).containsProblems())
									return CONFLICTING;
								return super.calculateKind();
							}
						};
						info.init();
						return info;
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
		try {
			if(resource.getType() == IResource.FILE)
				// file has no members
				return new IResource[0];
			IContainer container = (IContainer) resource;

			// workspace container members
			List existingChildren = new ArrayList(Arrays.asList(container.members()));

			// patch members, subscriber location
			FilePatch2[] diffs = getPatcher().getDiffs();
			for (int i = 0; i < diffs.length; i++) {
				IResource file = PatchModelProvider.getFile(diffs[i], getPatcher());
				if (!container.exists(file.getProjectRelativePath())) {
					existingChildren.add(file);
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
		Set roots = new HashSet();
		if (getPatcher().isWorkspacePatch()) {
			IDiffElement[] children = PatchWorkspace.getInstance().getChildren();
			for (int i = 0; i < children.length; i++) {
				// return array of projects from the patch
				DiffProject diffProject = ((PatchProjectDiffNode)children[i]).getDiffProject();
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(diffProject.getName());
				roots.add(project);
			}
		} else {
			roots.add(getPatcher().getTarget());
		}
		return (IResource[]) roots.toArray(new IResource[0]);
	}
	
	WorkspacePatcher getPatcher() {
		return patcher;
	}
}
