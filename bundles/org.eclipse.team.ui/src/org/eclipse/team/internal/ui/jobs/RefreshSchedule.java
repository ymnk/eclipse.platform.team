
package org.eclipse.team.internal.ui.jobs;

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.ui.synchronize.TeamSubscriberParticipant;

public class RefreshSchedule {
	private long refreshInterval = 3600; // 1 hour default
	
	private IRefreshEvent lastRefreshEvent;
	
	private boolean enabled = false;
	
	private RefreshSubscriberJob job;
	
	private TeamSubscriberParticipant participant;
	
	public RefreshSchedule(TeamSubscriberParticipant participant) {
		this.participant = participant;		
	}

	/**
	 * @return Returns the enabled.
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * @param enabled The enabled to set.
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * @return Returns the refreshInterval.
	 */
	public long getRefreshInterval() {
		return refreshInterval;
	}

	/**
	 * @param refreshInterval The refreshInterval to set.
	 */
	public void setRefreshInterval(long refreshInterval) {
		this.refreshInterval = refreshInterval;
	}
	
	public String lastRefreshEventAsString() {
		long stopMills = lastRefreshEvent.getStopTime();
		long startMills = lastRefreshEvent.getStartTime();
		StringBuffer text = new StringBuffer();
		if(stopMills <= 0) {
			text.append(Policy.bind("SyncViewPreferencePage.lastRefreshRunNever")); //$NON-NLS-1$
		} else {
			Date lastTimeRun = new Date(stopMills);
			text.append(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(lastTimeRun));
		}
		
		
		return text.toString();
	} 
	
	protected void startJob() {
		if(job == null) {
			job = new RefreshSubscriberJob(Policy.bind("ScheduledSyncViewRefresh.taskName"), participant.getInput());
		}
		job.setRestartOnCancel(true);
		job.setReschedule(true);
		job.schedule(getRefreshInterval());				
	}
	
	protected void stopJob() {
		job.setRestartOnCancel(false /* don't restart the job */);
		job.setReschedule(false);
		job.cancel();
		job = null;
	}
}
