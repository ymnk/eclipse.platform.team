package org.eclipse.team.ui.synchronize;

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.jobs.IRefreshEvent;
import org.eclipse.team.internal.ui.jobs.RefreshSubscriberJob;
import org.eclipse.ui.IMemento;

public class RefreshSchedule {
	private long refreshInterval = 3600; // 1 hour default
	
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
		boolean wasEnabled = isEnabled();
		this.enabled = enabled;
		if(enabled && ! wasEnabled) { 
			startJob();
		} else {
			stopJob();
		}
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
		stopJob();
		this.refreshInterval = refreshInterval;
		if(isEnabled()) {
			startJob();
		}
	}
	
	protected void startJob() {
		if(job == null) {
			job = new RefreshSubscriberJob("Refreshing '" + participant.getName() + "'. " + getRefreshIntervalAsString(), participant.getInput()); //$NON-NLS-1$
		}
		job.setRestartOnCancel(true);
		job.setReschedule(true);
		job.schedule(getRefreshInterval());				
	}
	
	protected void stopJob() {
		if(job != null) {
			job.setRestartOnCancel(false /* don't restart the job */);
			job.setReschedule(false);
			job.cancel();
			job = null;
		}
	}

	public void dispose() {
		stopJob();
	}
	
	public void saveState(IMemento memento) {
		memento.putString(CTX_REFRESHSCHEDULE_ENABLED, Boolean.toString(enabled));
		memento.putInteger(CTX_REFRESHSCHEDULE_INTERVAL, (int)refreshInterval);
	}

	public static RefreshSchedule init(IMemento memento, TeamSubscriberParticipant participant) {
		RefreshSchedule schedule = new RefreshSchedule(participant);
		if(memento != null) {
			String enabled = memento.getString(CTX_REFRESHSCHEDULE_ENABLED);
			int interval = memento.getInteger(CTX_REFRESHSCHEDULE_INTERVAL).intValue();
			schedule.setRefreshInterval(interval);
			schedule.setEnabled("true".equals(enabled) ? true : false);
		}
		// Use the defaults if a schedule hasn't been saved or can't be found.
		return schedule;
	}

	public static String refreshEventAsString(IRefreshEvent event) {
		long stopMills = event.getStopTime();
		long startMills = event.getStartTime();
		SyncInfo[] changes = event.getChanges();
		StringBuffer text = new StringBuffer();
		if(stopMills <= 0) {
			text.append(Policy.bind("SyncViewPreferencePage.lastRefreshRunNever")); //$NON-NLS-1$
		} else {
			Date lastTimeRun = new Date(stopMills);
			text.append(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(lastTimeRun));
		}				
		return text.toString();
	} 
	
	public String getScheduleAsString() {
		if(! isEnabled()) {
			return "Not Scheduled";
		}		
		return getRefreshIntervalAsString();
	}
	
	private String getRefreshIntervalAsString() {
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