package org.eclipse.team.internal.ui.jobs;

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.synchronize.TeamSubscriberParticipant;
import org.eclipse.ui.IMemento;

public class RefreshSchedule {
	private long refreshInterval = 3600; // 1 hour default
	
	private IRefreshEvent lastRefreshEvent;
	
	private boolean enabled = false;
	
	private RefreshSubscriberJob job;
	
	private TeamSubscriberParticipant participant;
	
	/**
	 * Key for settings in memento
	 */
	private static final String CTX_REFRESHSCHEDULE_INTERVAL = TeamUIPlugin.ID + ".CTX_REFRESHSCHEDULE_INTERVAL"; //$NON-NLS-1$
	
	/**
	 * Key for schedule in memento
	 */
	private static final String CTX_REFRESHSCHEDULE_ENABLED = TeamUIPlugin.ID + ".CTX_REFRESHSCHEDULE_ENABLED"; //$NON-NLS-1$
		
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

	public TeamSubscriberParticipant getParticipant() {
		return participant;
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
			job = new RefreshSubscriberJob(Policy.bind("ScheduledSyncViewRefresh.taskName"), participant.getInput()); //$NON-NLS-1$
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

	public void saveState(IMemento memento) {
		memento.putString(CTX_REFRESHSCHEDULE_ENABLED, Boolean.toString(enabled));
		memento.putInteger(CTX_REFRESHSCHEDULE_INTERVAL, (int)refreshInterval);
	}

	public static RefreshSchedule init(IMemento memento, TeamSubscriberParticipant participant) {
		RefreshSchedule schedule = new RefreshSchedule(participant);
		String enabled = memento.getString(CTX_REFRESHSCHEDULE_ENABLED);
		int interval = memento.getInteger(CTX_REFRESHSCHEDULE_INTERVAL).intValue();
		schedule.setEnabled(Boolean.getBoolean(enabled));
		schedule.setRefreshInterval(interval);
		return schedule;
	}

	public String getScheduleAsString() {
		if(! isEnabled()) {
			return "Not Scheduled";
		}
		StringBuffer text = new StringBuffer();				
		text.append("Every ");
		boolean hours = false;
		long seconds = getRefreshInterval();
		if(seconds <= 60) {
			seconds = 60;
		}
		long minutes = seconds / 60;		
		if(minutes >= 60) {
			minutes = minutes / 60;
			hours = true;
		}		
		text.append(Long.toString(minutes) + " ");
		if(minutes >= 1) {
			text.append(hours ? "hours" : "minutes");
		} else {
			text.append(hours ? "hour" : "minute");
		}
		return text.toString();
	}
}