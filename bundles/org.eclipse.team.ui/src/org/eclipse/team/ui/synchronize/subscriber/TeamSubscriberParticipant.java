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
package org.eclipse.team.ui.synchronize.subscriber;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.synchronize.actions.TeamParticipantRefreshAction;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.AbstractSynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.eclipse.ui.*;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * A synchronize participant that displays synchronization information for local
 * resources that are managed via a {@link Subscriber}.
 *
 * @since 3.0
 */
public abstract class TeamSubscriberParticipant extends AbstractSynchronizeParticipant implements IPropertyChangeListener {
	
	private SubscriberSyncInfoCollector collector;
	
	private FilteredSyncInfoCollector filteredSyncSet;
	
	private TeamSubscriberRefreshSchedule refreshSchedule;
	
	private int currentMode;
	
	private IWorkingSet workingSet;
	
	/**
	 * Key for settings in memento
	 */
	private static final String CTX_SUBSCRIBER_PARTICIPANT_SETTINGS = TeamUIPlugin.ID + ".TEAMSUBSRCIBERSETTINGS"; //$NON-NLS-1$
	
	/**
	 * Key for schedule in memento
	 */
	private static final String CTX_SUBSCRIBER_SCHEDULE_SETTINGS = TeamUIPlugin.ID + ".TEAMSUBSRCIBER_REFRESHSCHEDULE"; //$NON-NLS-1$
	
	/**
	 * Property constant indicating the mode of a page has changed. 
	 */
	public static final String P_SYNCVIEWPAGE_WORKINGSET = TeamUIPlugin.ID  + ".P_SYNCVIEWPAGE_WORKINGSET";	 //$NON-NLS-1$
	
	/**
	 * Property constant indicating the schedule of a page has changed. 
	 */
	public static final String P_SYNCVIEWPAGE_SCHEDULE = TeamUIPlugin.ID  + ".P_SYNCVIEWPAGE_SCHEDULE";	 //$NON-NLS-1$
	
	/**
	 * Property constant indicating the mode of a page has changed. 
	 */
	public static final String P_SYNCVIEWPAGE_MODE = TeamUIPlugin.ID  + ".P_SYNCVIEWPAGE_MODE";	 //$NON-NLS-1$
		
	/**
	 * Modes are direction filters for the view
	 */
	public final static int INCOMING_MODE = 0x1;
	public final static int OUTGOING_MODE = 0x2;
	public final static int BOTH_MODE = 0x4;
	public final static int CONFLICTING_MODE = 0x8;
	public final static int ALL_MODES = INCOMING_MODE | OUTGOING_MODE | CONFLICTING_MODE | BOTH_MODE;
	
	public final static int[] INCOMING_MODE_FILTER = new int[] {SyncInfo.CONFLICTING, SyncInfo.INCOMING};
	public final static int[] OUTGOING_MODE_FILTER = new int[] {SyncInfo.CONFLICTING, SyncInfo.OUTGOING};
	public final static int[] BOTH_MODE_FILTER = new int[] {SyncInfo.CONFLICTING, SyncInfo.INCOMING, SyncInfo.OUTGOING};
	public final static int[] CONFLICTING_MODE_FILTER = new int[] {SyncInfo.CONFLICTING};
	
	public TeamSubscriberParticipant() {
		super();
		refreshSchedule = new TeamSubscriberRefreshSchedule(this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.ISynchronizeViewPage#createPage(org.eclipse.team.ui.sync.ISynchronizeView)
	 */
	public IPageBookViewPage createPage(ISynchronizeView view) {
		return new TeamSubscriberParticipantPage(this, view);
	}
	
	public void setMode(int mode) {
		int oldMode = getMode();
		if(oldMode == mode) return;
		currentMode = mode;
		TeamUIPlugin.getPlugin().getPreferenceStore().setValue(IPreferenceIds.SYNCVIEW_SELECTED_MODE, mode);
		updateMode(mode);
		firePropertyChange(this, P_SYNCVIEWPAGE_MODE, new Integer(oldMode), new Integer(mode));
	}
	
	public int getMode() {
		return currentMode;
	}
	
	public void setRefreshSchedule(TeamSubscriberRefreshSchedule schedule) {
		this.refreshSchedule = schedule;
		firePropertyChange(this, P_SYNCVIEWPAGE_SCHEDULE, null, schedule);
	}
	
	public TeamSubscriberRefreshSchedule getRefreshSchedule() {
		return refreshSchedule;
	}
	
	public void setWorkingSet(IWorkingSet set) {
		IWorkingSet oldSet = workingSet;
		if(filteredSyncSet != null) {
			IResource[] resources = set != null ? Utils.getResources(set.getElements()) : new IResource[0];
			filteredSyncSet.setWorkingSet(resources);
			workingSet = null;
		} else {
			workingSet = set;
		}
		firePropertyChange(this, P_SYNCVIEWPAGE_WORKINGSET, oldSet, set);
	}
	
	public IWorkingSet getWorkingSet() {
		return workingSet;
	}
	
	public void refreshWithRemote(IResource[] resources) {
		if((resources == null || resources.length == 0)) {
			TeamParticipantRefreshAction.run(filteredSyncSet.getWorkingSet(), this);
		} else {
			TeamParticipantRefreshAction.run(resources, this);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.AbstractSynchronizeViewPage#dispose()
	 */
	public void dispose() {
		refreshSchedule.dispose();				
		TeamUI.removePropertyChangeListener(this);
		filteredSyncSet.dispose();
		collector.dispose();
	}
	
	/**
	 * Return the <code>FilteredSyncInfoCollector</code> for this participant.
	 * Thsi collector maintains the set of all out-of-sync resources that
	 * are being displayed on the participant's synchronize view page.
	 */
	public final FilteredSyncInfoCollector getFilteredSyncInfoCollector() {
		return filteredSyncSet; 
	}
	
	/**
	 * Return the <code>SubscriberSyncInfoCollector</code> for the participant.
	 * This collector maintains the set of all out-of-sync resources for the subscriber.
	 * @return the <code>SubscriberSyncInfoCollector</code> for this participant
	 */
	public final SubscriberSyncInfoCollector getTeamSubscriberSyncInfoCollector() {
		return collector;
	}
	
	protected void setSubscriber(Subscriber subscriber) {
		collector = new SubscriberSyncInfoCollector(subscriber);
		filteredSyncSet = new FilteredSyncInfoCollector(collector.getSyncInfoSet(), null /* no initial roots */, null /* no initial filter */);

		// listen for global ignore changes
		TeamUI.addPropertyChangeListener(this);
		
		if(workingSet != null) {
			setWorkingSet(workingSet);
		}
		updateMode(getMode());
		// start the refresh how that a subscriber has been added
		TeamSubscriberRefreshSchedule schedule = getRefreshSchedule();
		if(schedule.isEnabled()) {
			getRefreshSchedule().startJob();
		}
	}
	
	/**
	 * Get the <code>Subscriber</code> for this participant
	 * @return a <code>TamSubscriber</code>
	 */
	public Subscriber getSubscriber() {
		return collector.getTeamSubscriber();
	}
		
	/* (non-Javadoc)
	 * @see IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(TeamUI.GLOBAL_IGNORES_CHANGED)) {
			try {
				collector.reset(null);
			} catch (TeamException e) {
				TeamUIPlugin.log(e);
			}
		}	
	}
	
	private void updateMode(int mode) {
		if(filteredSyncSet != null) {	
		
		int[] modeFilter = BOTH_MODE_FILTER;
		switch(mode) {
		case TeamSubscriberParticipant.INCOMING_MODE:
			modeFilter = INCOMING_MODE_FILTER; break;
		case TeamSubscriberParticipant.OUTGOING_MODE:
			modeFilter = OUTGOING_MODE_FILTER; break;
		case TeamSubscriberParticipant.BOTH_MODE:
			modeFilter = BOTH_MODE_FILTER; break;
		case TeamSubscriberParticipant.CONFLICTING_MODE:
			modeFilter = CONFLICTING_MODE_FILTER; break;
		}

			getFilteredSyncInfoCollector().setFilter(
					new FastSyncInfoFilter.AndSyncInfoFilter(
							new FastSyncInfoFilter[] {
									new FastSyncInfoFilter.SyncInfoDirectionFilter(modeFilter)
							}), new NullProgressMonitor());
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#init(org.eclipse.ui.IMemento)
	 */
	public void init(IMemento memento) throws PartInitException {
		if(memento != null) {
			IMemento settings = memento.getChild(CTX_SUBSCRIBER_PARTICIPANT_SETTINGS);
			if(settings != null) {
				String set = settings.getString(P_SYNCVIEWPAGE_WORKINGSET);
				String mode = settings.getString(P_SYNCVIEWPAGE_MODE);
				TeamSubscriberRefreshSchedule schedule = TeamSubscriberRefreshSchedule.init(settings.getChild(CTX_SUBSCRIBER_SCHEDULE_SETTINGS), this);
				setRefreshSchedule(schedule);
				
				if(set != null) {
					IWorkingSet workingSet = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(set);
					if(workingSet != null) {
						setWorkingSet(workingSet);
					}
				}
				setMode(Integer.parseInt(mode));
			}
		} else {
			setMode(BOTH_MODE);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#saveState(org.eclipse.ui.IMemento)
	 */
	public void saveState(IMemento memento) {
		IMemento settings = memento.createChild(CTX_SUBSCRIBER_PARTICIPANT_SETTINGS);
		IWorkingSet set = getWorkingSet();
		if(set != null) {
			settings.putString(P_SYNCVIEWPAGE_WORKINGSET, getWorkingSet().getName());
		}
		settings.putString(P_SYNCVIEWPAGE_MODE, Integer.toString(getMode()));
		refreshSchedule.saveState(settings.createChild(CTX_SUBSCRIBER_SCHEDULE_SETTINGS));
	}
}