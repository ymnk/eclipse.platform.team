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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

/**
 * This class is reponisble for notifying views when jobs that effect
 * the contents of the view start and stop
 */
public class ViewFeedbackManager {
	
	private List listeners = new ArrayList();
	private Map jobTypes = new HashMap();
	
	private static ViewFeedbackManager instance;
	
	public synchronized static ViewFeedbackManager getInstance() {
		if (instance == null) {
			instance = new ViewFeedbackManager();
		}
		return instance;
	}
	public void schedule(Job job, final QualifiedName jobType) {
		job.addJobChangeListener(getJobChangeListener(jobType));
		// indicate that the job has started since it will be schdulued immediatley
		jobStarted(job, jobType);
		job.schedule();
	}

	public void schedule(Job job, long delay, final QualifiedName jobType) {
		job.addJobChangeListener(getJobChangeListener(jobType));
		job.schedule(delay);
	}
	
	private JobChangeAdapter getJobChangeListener(final QualifiedName jobType) {
		return new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				jobDone(event.getJob(), jobType);

			}
			public void running(IJobChangeEvent event) {
				jobStarted(event.getJob(), jobType);
			}
		};
	}

	public void addJobListener(IJobListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}
	public void removeJobListener(IJobListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	private IJobListener[] getJobListeners() {
		synchronized (listeners) {
			return (IJobListener[]) listeners.toArray(new IJobListener[listeners.size()]);
		}
	}
	
	/* internal use only */ void jobStarted(Job job, QualifiedName jobType) {
		if (recordJob(job, jobType)) {
			fireStartNotification(jobType);
		}
	}

	/*
	 * Record the job and return true if it's the first job of that type
	 */
	private boolean recordJob(Job job, QualifiedName jobType) {
		Set jobs = (Set)jobTypes.get(jobType);
		if (jobs == null) {
			jobs = new HashSet();
			jobTypes.put(jobType, jobs);
		}
		if (!jobs.add(job)) {
			// The job was already in the set. Invalid?
			return false;
		}
		return jobs.size() == 1;
	}

	/*
	 * Remove the job and return true if it is the last job for the type
	 */
	private boolean removeJob(Job job, QualifiedName jobType) {
		Set jobs = (Set)jobTypes.get(jobType);
		if (jobs == null) {
			// TODO: Is this invalid?
			return false;
		}
		if (!jobs.remove(job)) {
			// The job wasn't in the list. Probably invalid
			return false;
		}
		return jobs.isEmpty();
	}
	
	private void fireStartNotification(QualifiedName jobType) {
		IJobListener[] listenerArray = getJobListeners();
		for (int i = 0; i < listenerArray.length; i++) {
			IJobListener listener = listenerArray[i];
			listener.started(jobType);
		}
	}

	/* internal use only */ void jobDone(Job job, QualifiedName jobType) {
		if (removeJob(job, jobType)) {
			fireEndNotification(jobType);
		}
	}

	private void fireEndNotification(QualifiedName jobType) {
		IJobListener[] listenerArray = getJobListeners();
		for (int i = 0; i < listenerArray.length; i++) {
			IJobListener listener = listenerArray[i];
			listener.finished(jobType);
		}
	}
	
	public boolean hasRunningJobs(QualifiedName jobType) {
		Set jobs = (Set)jobTypes.get(jobType);
		return jobs != null && !jobs.isEmpty();
	}
}
