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
package org.eclipse.team.core.subscribers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.core.Policy;

/**
 * Job to periodically refresh the registered subscribers with their remote state. 
 * 
 * When the user explicitly requests a refresh the current background refreshes are
 * cancelled and the subscriber and resources that the user asked to refresh are processed.
 * Upon completion of the user initiated refresh, the scheduled background refreshes
 * will resume.
 * 
 * [Note: this job currently updates all roots of every subscriber. It may be better to have API 
 * to specify a more constrained set of resources and subscribers to refresh.] 
 */
public class RefreshSubscribersJob extends Job implements ITeamResourceChangeListener, IJobChangeListener {
	
	private final static boolean DEBUG = Policy.DEBUG_REFRESH_JOB;
	private static long refreshInterval = 20000; //5 /* minutes */ * (60 * 1000); 
	
	private Map subscribers = Collections.synchronizedMap(new  HashMap());
	private List importantSubscribers = Collections.synchronizedList(new  ArrayList());
	
	private RefreshSubscribersJob instance;
	
	public RefreshSubscribersJob() {
		TeamProvider.addListener(this);
		Platform.getJobManager().addJobChangeListener(this);
		setPriority(Job.DECORATE);
		if(! subscribers.isEmpty()) {
			if(DEBUG) System.out.println("refreshJob: starting job in constructor");
			startup();
		}
		
		instance = this;
	}
	
	public RefreshSubscribersJob getInstance() {
		if(instance == null) {
			new RefreshSubscribersJob();
		}
		return instance;
	}
	
	/**
	 * Specify the interval in seconds at which this job is scheduled.
	 * @param seconds delay specified in seconds
	 */
	synchronized public void setRefreshInterval(long seconds) {
		refreshInterval = seconds * 1000;
		
		// if the job hasn't been run yet then update the interval time,
		// otherwise wait until the job is finished and the interval time
		// will be used when it is rescheduled.
		if(getState() == Job.WAITING) {
			cancel();
			startup();
		}
	}
	
	/**
	 * Returns the interval of this job in seconds. 
	 * @return
	 */
	synchronized public long getRefreshInterval() {
		return refreshInterval / 1000;
	}
	
	/**
	 * Called to schedule a subscriber to be refreshed immediately. If the job is currently running it
	 * will be cancelled and the job will be restarted to refresh the given subscriber only.   
	 */
	synchronized public void refreshNow(IResource[] resources, TeamSubscriber subscriber) {
	}
	
	/**
	 * This is run by the job scheduler. A list of subscribers will be refreshed, errors will not stop the job 
	 * and it will continue to refresh the other subscribers.
	 */
	public IStatus run(IProgressMonitor monitor) {		
		monitor.beginTask("", subscribers.size() * 100);
		try {		
			Iterator it;
			if(importantSubscribers != null && ! importantSubscribers.isEmpty()) {
				it = importantSubscribers.iterator();
			} else {
				it = subscribers.values().iterator();
			}
			while (it.hasNext()) {
				if(monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				TeamSubscriber s = (TeamSubscriber) it.next();
				try {
					if(DEBUG) System.out.println("refreshJob: starting refresh for " + s.getName());
					s.refresh(s.roots(), IResource.DEPTH_INFINITE, Policy.subMonitorFor(monitor, 100));
					if(DEBUG) System.out.println("refreshJob: finished refresh for " + s.getName());
				} catch(TeamException e) {
					if(DEBUG) System.out.println("refreshJob: exception in refresh " + s.getName() + ":" + e.getMessage());
					//TeamPlugin.log(e);
					// keep going'
				}
			}
		} catch(OperationCanceledException e2) {
			return Status.CANCEL_STATUS;
		} finally {
			monitor.done();
			importantSubscribers.clear();
		}
		return Status.OK_STATUS;
	}

	/**
	 * This job will update it's list of subscribers to refresh based on the create/delete 
	 * subscriber events. 
	 * 
	 * If a new subscriber is created it will be added to the list of subscribers
	 * to refresh and the job will be started if it isn't already.
	 * 
	 * If a subscriber is deleted, the job is cancelled to ensure that the subscriber being 
	 * deleted can be properly shutdown. After removing the subscriber from the list the
	 * job is restarted is there are any subscribers left.  
	 */
	public void teamResourceChanged(TeamDelta[] deltas) {
		for (int i = 0; i < deltas.length; i++) {
			TeamDelta delta = deltas[i];
			if(delta.getFlags() == TeamDelta.SUBSCRIBER_CREATED) {				
				TeamSubscriber s = delta.getSubscriber();
				subscribers.put(s.getId(), s);
				if(DEBUG) System.out.println("refreshJob: adding subscriber " + s.getName());
				if(this.getState() == Job.NONE) {
					if(DEBUG) System.out.println("refreshJob: starting job after adding " + s.getName());
					startup();
				}				
			} else if(delta.getFlags() == TeamDelta.SUBSCRIBER_DELETED) {
				// cancel current refresh just to make sure that the subscriber being deleted can
				// be properly shutdown
				cancel();
				TeamSubscriber s = delta.getSubscriber();
				subscribers.remove(s.getId());
				if(DEBUG) System.out.println("refreshJob: removing subscriber " + s.getName());
				if(! subscribers.isEmpty()) {
					startup();
				}
			}
		}
	}

	private void startup() {
		schedule(refreshInterval);
	}

	/**
	 * IJobChangeListener overrides. The only one of interest is done so that we can
	 * restart this job.
	 */
	public void done(Job job, IStatus result) {
		if(job == this) {
			if(DEBUG) System.out.println("refreshJob: restarting job");
			startup();
		}
	}
	public void aboutToRun(Job job) {
	}
	public void awake(Job job) {
	}
	public void running(Job job) {
	}
	public void scheduled(Job job) {
	}
	public void sleeping(Job job) {
	}
}