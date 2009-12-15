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
		// TODO Auto-generated method stub
		System.out.println(">> [ignore] makeInSync: " + diff.toDiffString()); //$NON-NLS-1$
	}

	public void markAsMerged(IDiff node, boolean inSyncHint,
			IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		System.out
				.println(">> [ignore] markAsMerged: " + node.toDiffString() + ", inSyncHint " + inSyncHint); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void reject(IDiff diff, IProgressMonitor monitor)
			throws CoreException {
		// TODO Auto-generated method stub
		System.out.println(">> [ignore] reject: " + diff.toDiffString()); //$NON-NLS-1$
	}
}
