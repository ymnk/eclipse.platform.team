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
package org.eclipse.team.internal.ui.jobs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.TeamSubscriber;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.synchronize.sets.SubscriberInput;

/**
 * Job to refresh a subscriber with its remote state.
 * 
 * There can be several refresh jobs created but they will be serialized.
 */
public class RefreshSubscriberInputJob extends RefreshSubscriberJob {
	
	/**
	 * The subscribers and roots to refresh. If these are changed when the job
	 * is running the job is cancelled.
	 */
	private List inputs = new ArrayList(3);
	
	public RefreshSubscriberInputJob(String name) {
		super(name, null, null);
	}

	public synchronized void addSubscriberInput(SubscriberInput input) {
		stop();
		inputs.add(input);
		start();
	}

	public synchronized void removeSubscriberInput(SubscriberInput input) {
		stop();
		inputs.remove(input);
		start();
	}
	
	private void stop() {
		int state = getState();
		if(state == Job.RUNNING) {
			cancel();
			try {
				join();
			} catch (InterruptedException e) {
				// continue
			}
		}
	}
	
	
	
	/**
	 * This is run by the job scheduler. A list of subscribers will be refreshed, errors will not stop the job 
	 * and it will continue to refresh the other subscribers.
	 */
	public IStatus runInWorkspace(IProgressMonitor monitor) {
		// Synchronized to ensure only one refresh job is running at a particular time
		synchronized (getFamily()) {	
			MultiStatus status = new MultiStatus(TeamUIPlugin.ID, TeamException.UNABLE, Policy.bind("RefreshSubscriberJob.0"), null); //$NON-NLS-1$
			
			// if there are no resources to refresh, just return
			if(inputs.isEmpty()) {
				return Status.OK_STATUS;
			}
					
			monitor.beginTask(getTaskName(), 100);
			try {
				// Only allow one refresh job at a time
				// NOTE: It would be cleaner if this was done by a scheduling
				// rule but at the time of writting, it is not possible due to
				// the scheduling rule containment rules.
				lastTimeRun = System.currentTimeMillis();
				if(monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				try {
					for (Iterator it = inputs.iterator(); it.hasNext();) {
						SubscriberInput input = (SubscriberInput) it.next();
						TeamSubscriber subscriber = input.getSubscriber();
						subscriber.refresh(input.workingSetRoots(), IResource.DEPTH_INFINITE, Policy.subMonitorFor(monitor, 100));
					}
				} catch(TeamException e) {
					status.merge(e.getStatus());
				}
			} catch(OperationCanceledException e2) {
				return Status.CANCEL_STATUS;
			} finally {
				monitor.done();
			}
			return status.isOK() ? Status.OK_STATUS : (IStatus) status;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.jobs.RefreshSubscriberJob#getTaskName(org.eclipse.team.core.subscribers.TeamSubscriber, org.eclipse.core.resources.IResource[])
	 */
	protected String getTaskName() {
		// Return a meaningfull task nam since the job name will be generic
		return Policy.bind("RefreshSubscriberJob.1", String.valueOf(inputs.size()), "all"); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#shouldRun()
	 */
	public boolean shouldRun() {
		return ! inputs.isEmpty();
	}
}