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

import org.eclipse.compare.internal.core.patch.DiffProject;
import org.eclipse.compare.internal.core.patch.FilePatch2;
import org.eclipse.compare.internal.patch.Patcher;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberMergeContext;

class ApplyPatchSubscriberMergeContext extends SubscriberMergeContext {

	protected ApplyPatchSubscriberMergeContext(Subscriber subscriber,
			ISynchronizationScopeManager manager) {
		super(subscriber, manager);
	}

	public static ApplyPatchSubscriberMergeContext createContext(
			Subscriber subscriber, ISynchronizationScopeManager manager) {
		ApplyPatchSubscriberMergeContext mergeContext = new ApplyPatchSubscriberMergeContext(
				subscriber, manager);
		// Initialize using the ApplyPatchSubscriber to populate the diff tree.
		mergeContext.initialize();
		return mergeContext;
	}

	protected void makeInSync(IDiff diff, IProgressMonitor monitor)
			throws CoreException {
		IResource resource = getDiffTree().getResource(diff);
		// IDiffElement element = PatchModelProvider.createModelObject(resource);
		Patcher patcher = ((ApplyPatchSubscriber)getSubscriber()).getPatcher();
		FilePatch2[] diffs = patcher.getDiffs();
		if (resource.getType() == IResource.FILE) {
			for (int i = 0; i < diffs.length; i++) {
				if (diffs[i] instanceof FilePatch2) {
					DiffProject diffProject = diffs[i].getProject();
					IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(diffProject.getName());
					IFile file = project.getFile(diffs[i].getPath(patcher.isReversed()));
					if (file.equals(resource)) {
						patcher.setEnabled(diffs[i], false);
						System.out.println(">> ApplyPatchSubscriberMergeContext.makeInSync: disable " + diffs[i]); //$NON-NLS-1$
					}
				}
			}
		}
	}

	public void markAsMerged(IDiff node, boolean inSyncHint,
			IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		System.out
				.println(">> [ignore] ApplyPatchSubscriberMergeContext.markAsMerged: " + node.toDiffString() + ", inSyncHint " + inSyncHint); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void reject(IDiff diff, IProgressMonitor monitor)
			throws CoreException {
		// TODO Auto-generated method stub
		System.out.println(">> [ignore] ApplyPatchSubscriberMergeContext.reject: " + diff.toDiffString()); //$NON-NLS-1$
	}
}
