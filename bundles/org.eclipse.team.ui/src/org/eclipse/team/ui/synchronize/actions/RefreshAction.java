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
package org.eclipse.team.ui.synchronize.actions;

import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.*;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.jobs.RefreshSubscriberJob;
import org.eclipse.team.internal.ui.synchronize.IRefreshSubscriberListener;
import org.eclipse.team.internal.ui.synchronize.views.TreeViewerUtils;

/**
 * A general refresh action that will refresh a subscriber in the background.
 */
public class RefreshAction extends Action {
	
	private ISelectionProvider selectionProvider;
	private boolean refreshAll;
	private TeamSubscriberSyncInfoCollector collector;
	private IRefreshSubscriberListener listener;
	private String description;
	
	public static void run(String description, IResource[] resources, TeamSubscriberSyncInfoCollector collector, IRefreshSubscriberListener listener) {
		// Cancel the scheduled background refresh or any other refresh that is happening.
		// The scheduled background refresh will restart automatically.
		Platform.getJobManager().cancel(RefreshSubscriberJob.getFamily());
		RefreshSubscriberJob job = new RefreshSubscriberJob(Policy.bind("SyncViewRefresh.taskName", description), resources, collector); //$NON-NLS-1$
		if (listener != null) {
			RefreshSubscriberJob.addRefreshListener(listener);
		}
		JobStatusHandler.schedule(job, TeamSubscriber.SUBSCRIBER_JOB_TYPE);
	}
	
	public RefreshAction(ISelectionProvider page, String description, TeamSubscriberSyncInfoCollector collector, IRefreshSubscriberListener listener, boolean refreshAll) {
		this.selectionProvider = page;
		this.description = description;
		this.collector = collector;
		this.listener = listener;
		this.refreshAll = refreshAll;
		Utils.initAction(this, "action.refreshWithRemote."); //$NON-NLS-1$
	}
	
	public void run() {
		ISelection selection = selectionProvider.getSelection();
		if(selection instanceof IStructuredSelection) {
			IResource[] resources = getResources((IStructuredSelection)selection);
			if (refreshAll || resources.length == 0) {
				// If no resources are selected, refresh all the subscriber roots
				resources = collector.getRoots();
			}
			run(description, resources, collector, listener);
		}					
	}
	
	private IResource[] getResources(IStructuredSelection selection) {
		if(selection == null) {
			return new IResource[0];
		}
		List resources = new ArrayList();
		Iterator it = selection.iterator();
		while(it.hasNext()) {
			IResource resource = TreeViewerUtils.getResource(it.next());
			if(resource != null) {
				resources.add(resource);
			}
		}
		return (IResource[]) resources.toArray(new IResource[resources.size()]);					
	}
}
