/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core.subscribers;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.subscribers.ISyncInfoSetChangeListener;
import org.eclipse.team.core.subscribers.MutableSyncInfoSet;
import org.eclipse.team.internal.core.Policy;

/**
 * This is a specialized sync info set that will run connects and other batched modifications
 * in the background
 */
public class SubscriberSyncInfoSet extends MutableSyncInfoSet {
	
	SubscriberEventHandler handler;
	
	public SubscriberSyncInfoSet(SubscriberEventHandler handler) {
		this.handler = handler;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.MutableSyncInfoSet#run(org.eclipse.core.resources.IWorkspaceRunnable, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(final IWorkspaceRunnable runnable, IProgressMonitor monitor) throws CoreException {
		handler.run(new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				// Perform a beginInput to ensure no modifications are performed on the set
				// while the runnable is being run in the background job
				beginInput();
				try {
					monitor.beginTask(null, 100);
					runnable.run(Policy.subMonitorFor(monitor, 95));
				} finally {
					endInput(Policy.subMonitorFor(monitor, 5));
					monitor.done();
				}
			}
		});
	}

	/**
	 * Propogate the error to any listeners who handle errors
	 * @param event
	 */
	public void handleErrorEvent(final SubscriberErrorEvent event, final IProgressMonitor monitor) {
		ISyncInfoSetChangeListener[] allListeners = getListeners();
		// Fire the events using an ISafeRunnable
		monitor.beginTask(null, 100 * allListeners.length);
		for (int i = 0; i < allListeners.length; i++) {
			final ISyncInfoSetChangeListener listener = allListeners[i];
			if (listener instanceof ISyncInfoSetChangeListener2) {
				Platform.run(new ISafeRunnable() {
					public void handleException(Throwable exception) {
						// don't log the exception....it is already being logged in Platform#run
					}
					public void run() throws Exception {
						((ISyncInfoSetChangeListener2)listener).handleError(event, Policy.subMonitorFor(monitor, 100));
		
					}
				});
			}
		}
		monitor.done();
	}
	
}
