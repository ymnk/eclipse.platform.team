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

import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.synchronize.RefreshCompleteDialog;
import org.eclipse.team.internal.ui.synchronize.sets.SubscriberInput;
import org.eclipse.team.ui.synchronize.ITeamSubscriberSyncInfoSets;

/**
 * Job to refresh a subscriber with its remote state.
 * 
 * There can be several refresh jobs created but they will be serialized.
 * This is accomplished using a synchrnized block on the family id. It is
 * important that no scheduling rules are used for the job in order to
 * avoid possible deadlock. 
 */
public class RefreshSubscriberJob extends WorkspaceJob {
	
	/**
	 * Uniquely identifies this type of job. This is used for cancellation.
	 */
	private final static Object FAMILY_ID = new Object();
	
	/**
	 * If true this job will be restarted when it completes 
	 */
	private boolean reschedule = false;
	
	/**
	 * If true a rescheduled refresh job should be retarted when cancelled
	 */
	/* internal use only */ boolean restartOnCancel = true; 
	
	/**
	 * The schedule delay used when rescheduling a completed job 
	 */
	/* internal use only */ static long scheduleDelay = 20000; 
	
	/**
	 * The subscribers and roots to refresh. If these are changed when the job
	 * is running the job is cancelled.
	 */
	private IResource[] resources;
	private ITeamSubscriberSyncInfoSets input;
	
	protected class MD extends MessageDialog {
		public MD(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage, int dialogImageType, String[] dialogButtonLabels, int defaultIndex) {
			super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels, defaultIndex);
			setShellStyle(SWT.DIALOG_TRIM | SWT.MODELESS);
		}
	}
	
	protected class ChangeListener implements ITeamResourceChangeListener {
		private List changes = new ArrayList();
		private ITeamSubscriberSyncInfoSets input;
		ChangeListener(ITeamSubscriberSyncInfoSets input) {
			this.input = input;
		}
		public void teamResourceChanged(TeamDelta[] deltas) {
			for (int i = 0; i < deltas.length; i++) {
				TeamDelta delta = deltas[i];
				if(delta.getFlags() == TeamDelta.SYNC_CHANGED) {
					changes.add(delta);
				}
			}
		}
		public SyncInfo[] getChanges() {
			try {
				// wait for inputs to stop processing changes
				if(input instanceof SubscriberInput) {
					((SubscriberInput)input).getEventHandler().getEventHandlerJob().join();
				}
			} catch (InterruptedException e) {
				// continue
			}
			List changedSyncInfos = new ArrayList();
			for (Iterator it = changes.iterator(); it.hasNext(); ) {
				TeamDelta delta = (TeamDelta) it.next();
				SyncInfo info = input.getSubscriberSyncSet().getSyncInfo(delta.getResource());
				if(info != null) {
					int direction = info.getKind() & SyncInfo.DIRECTION_MASK;
					if(direction == SyncInfo.INCOMING || direction == SyncInfo.CONFLICTING) {
						changedSyncInfos.add(info);
					}
				}
			}
			return (SyncInfo[]) changedSyncInfos.toArray(new SyncInfo[changedSyncInfos.size()]);
		}
	}
		
	public RefreshSubscriberJob(String name, IResource[] resources, ITeamSubscriberSyncInfoSets input) {
		super(name);
		
		this.resources = resources;
		this.input = input;
		
		setPriority(Job.DECORATE);
		
		addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				if(shouldReschedule()) {
					if(event.getResult().getSeverity() == IStatus.CANCEL && ! restartOnCancel) {					
						return;
					}
					RefreshSubscriberJob.this.schedule(scheduleDelay);
					restartOnCancel = true;
				}
			}
		});
	}
	
	public boolean shouldRun() {
		return getSubscriber() != null;
	}
	
	public boolean belongsTo(Object family) {		
		return family == getFamily();
	}
	
	public static Object getFamily() {
		return FAMILY_ID;
	}
	
	/**
	 * This is run by the job scheduler. A list of subscribers will be refreshed, errors will not stop the job 
	 * and it will continue to refresh the other subscribers.
	 */
	public IStatus runInWorkspace(IProgressMonitor monitor) {
		// Synchronized to ensure only one refresh job is running at a particular time
		synchronized (getFamily()) {	
			MultiStatus status = new MultiStatus(TeamUIPlugin.ID, TeamException.UNABLE, Policy.bind("RefreshSubscriberJob.0"), null); //$NON-NLS-1$
			TeamSubscriber subscriber = getSubscriber();
			IResource[] roots = getResources();
			
			// if there are no resources to refresh, just return
			if(subscriber == null || roots == null) {
				return Status.OK_STATUS;
			}
			
			monitor.beginTask(null, 100);
			try {
				// Only allow one refresh job at a time
				// NOTE: It would be cleaner if this was done by a scheduling
				// rule but at the time of writting, it is not possible due to
				// the scheduling rule containment rules.
				long lastTimeRun = System.currentTimeMillis();
				if(monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				try {
					final ChangeListener listener = new ChangeListener(input);
					subscriber.addListener(listener);
					subscriber.refresh(roots, IResource.DEPTH_INFINITE, Policy.subMonitorFor(monitor, 100));
					input.getParticipant().setLastRefreshTime(lastTimeRun);
					subscriber.removeListener(listener);

					TeamUIPlugin.getStandardDisplay().asyncExec(new Runnable() {
							public void run() {
								RefreshCompleteDialog d = new RefreshCompleteDialog(
									new Shell(TeamUIPlugin.getStandardDisplay()), listener.getChanges(), new  ITeamSubscriberSyncInfoSets[] {input});
								d.setBlockOnOpen(false);
								d.open();
							}
						});					
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
	
	protected IResource[] getResources() {
		return resources;
	}
	
	protected TeamSubscriber getSubscriber() {
		return input.getSubscriber();
	}
	
	public long getScheduleDelay() {
		return scheduleDelay;
	}
	
	protected void start() {
		if(getState() == Job.NONE) {
			if(shouldReschedule()) {
				schedule(getScheduleDelay());
			}
		}
	}
	
	/**
	 * Specify the interval in seconds at which this job is scheduled.
	 * @param seconds delay specified in seconds
	 */
	public void setRefreshInterval(long seconds) {
		boolean restart = false;
		if(getState() == Job.SLEEPING) {
			restart = true;
			cancel();
		}
		scheduleDelay = seconds * 1000;
		if(restart) {
			start();
		}
	}
	
	/**
	 * Returns the interval of this job in seconds. 
	 * @return
	 */
	public long getRefreshInterval() {
		return scheduleDelay / 1000;
	}
	
	public void setRestartOnCancel(boolean restartOnCancel) {
		this.restartOnCancel = restartOnCancel;
	}
	
	public void setReschedule(boolean reschedule) {
		this.reschedule = reschedule;
	}
	
	public boolean shouldReschedule() {
		return reschedule;
	}	
}