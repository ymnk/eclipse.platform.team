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
package org.eclipse.team.ui.synchronize.subscribers;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberSyncInfoCollector;
import org.eclipse.team.core.synchronize.SyncInfoTree;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.*;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.*;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * A synchronize participant that displays synchronization information for local
 * resources that are managed via a {@link Subscriber}.
 * 
 * Participant:
 * 1. manages subscriber collector
 * 2. synchronize schedule
 * 3. APIs for creating specific: sync page, sync wizard, sync advisor (control ui pieces)
 * 4. allows refreshing the participant synchronization state
 * 
 * Push Down:
 * 1. working set
 * 2. modes
 * 3. working set/filtered sync sets
 *
 * @since 3.0
 */
public abstract class SubscriberParticipant extends AbstractSynchronizeParticipant implements IPropertyChangeListener {
	
	private SubscriberSyncInfoCollector collector;
	
	private SubscriberRefreshSchedule refreshSchedule;
	
	/**
	 * Key for settings in memento
	 */
	private static final String CTX_SUBSCRIBER_PARTICIPANT_SETTINGS = TeamUIPlugin.ID + ".TEAMSUBSRCIBERSETTINGS"; //$NON-NLS-1$
	
	/**
	 * Key for schedule in memento
	 */
	private static final String CTX_SUBSCRIBER_SCHEDULE_SETTINGS = TeamUIPlugin.ID + ".TEAMSUBSRCIBER_REFRESHSCHEDULE"; //$NON-NLS-1$
	
	/**
	 * Property constant indicating the schedule of a page has changed. 
	 */
	public static final String P_SYNCVIEWPAGE_SCHEDULE = TeamUIPlugin.ID  + ".P_SYNCVIEWPAGE_SCHEDULE";	 //$NON-NLS-1$

	private IRefreshSubscriberListenerFactory refreshListenerFactory;
	
	public SubscriberParticipant() {
		super();
		refreshSchedule = new SubscriberRefreshSchedule(this);
		refreshListenerFactory = new IRefreshSubscriberListenerFactory() {
			public IRefreshSubscriberListener createModalDialogListener(String targetId, SubscriberParticipant participant, SyncInfoTree syncInfoSet) {
				return new RefreshUserNotificationPolicyInModalDialog(targetId, participant, syncInfoSet);
			}
			public IRefreshSubscriberListener createSynchronizeViewListener(SubscriberParticipant participant) {
				return new RefreshUserNotificationPolicy(participant);
			}
		};
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.ISynchronizeViewPage#createPage(org.eclipse.team.ui.sync.ISynchronizeView)
	 */
	public final IPageBookViewPage createPage(SubscriberConfiguration configuration) {
		return doCreatePage(configuration);
	}
	
	protected SubscriberParticipantPage doCreatePage(SubscriberConfiguration configuration) {
		return new SubscriberParticipantPage(configuration);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#createSynchronizeConfiguration(org.eclipse.ui.IWorkbenchPart)
	 */
	public ISynchronizeConfiguration createSynchronizeConfiguration(IWorkbenchPart part) {
		return new SubscriberConfiguration(this, part);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#createRefreshPage()
	 */
	public IWizard createSynchronizeWizard() {
		return new SubscriberRefreshWizard(this);
	}
	
	public void setRefreshSchedule(SubscriberRefreshSchedule schedule) {
		this.refreshSchedule = schedule;
		firePropertyChange(this, P_SYNCVIEWPAGE_SCHEDULE, null, schedule);
	}
	
	public SubscriberRefreshSchedule getRefreshSchedule() {
		return refreshSchedule;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#getResources()
	 */
	public IResource[] getResources() {
		return collector.getSubscriber().roots();
	}
	
	/**
	 * Will refresh a participant in the background.
	 * 
	 * @param resources the resources to be refreshed.
	 */
	public void refresh(IResource[] resources, final IRefreshSubscriberListener listener, String taskName, IWorkbenchSite site) {
		RefreshSubscriberJob job = new RefreshSubscriberJob(taskName, resources, collector.getSubscriber());
		job.setSubscriberCollector(collector);
		IRefreshSubscriberListener autoListener = new IRefreshSubscriberListener() {
			public void refreshStarted(IRefreshEvent event) {
				if(listener != null) {
					listener.refreshStarted(event);
				}
			}
			public void refreshDone(IRefreshEvent event) {
				if(listener != null) {
					listener.refreshDone(event);
					RefreshSubscriberJob.removeRefreshListener(this);
				}
			}
		};
		
		if (listener != null) {
			RefreshSubscriberJob.addRefreshListener(autoListener);
		}	
		Utils.schedule(job, site);
	}
	
	public IRefreshSubscriberListenerFactory getRefreshListeners() {
		return getRefreshListenerFactory();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.AbstractSynchronizeViewPage#dispose()
	 */
	public void dispose() {
		refreshSchedule.dispose();				
		TeamUI.removePropertyChangeListener(this);
		collector.dispose();
	}
	
	/**
	 * Return the <code>SubscriberSyncInfoCollector</code> for the participant.
	 * This collector maintains the set of all out-of-sync resources for the subscriber.
	 * @return the <code>SubscriberSyncInfoCollector</code> for this participant
	 */
	public final SubscriberSyncInfoCollector getSubscriberSyncInfoCollector() {
		return collector;
	}
	
	protected void setSubscriber(Subscriber subscriber) {
		collector = new SubscriberSyncInfoCollector(subscriber);
		
		// listen for global ignore changes
		TeamUI.addPropertyChangeListener(this);
		
		preCollectingChanges();
		
		collector.start();
		
		// start the refresh now that a subscriber has been added
		SubscriberRefreshSchedule schedule = getRefreshSchedule();
		if(schedule.isEnabled()) {
			getRefreshSchedule().startJob();
		}
	}
	
	protected IRefreshSubscriberListenerFactory getRefreshListenerFactory() {
		return refreshListenerFactory;
	}
	
	/**
	 * This method is invoked just before the collector is started. 
	 * This gives an oportunity to configure the collector parameters
	 * before collection starts. The default implementation sets the working
	 * set as returned by <code>getWorkingSet()</code> and sets the mode 
	 * as returned by <code>getMode()</code>.
	 */
	protected void preCollectingChanges() {
	}
	
	/**
	 * Returns the viewer advisor which will be used to configure the display of the participant.
	 * @return
	 */
	protected StructuredViewerAdvisor createSynchronizeViewerAdvisor(SubscriberConfiguration configuration, SyncInfoTree syncInfoTree) {
		return new SynchronizeViewerAdvisor(configuration, syncInfoTree);
	}
	
	/**
	 * Get the <code>Subscriber</code> for this participant
	 * @return a <code>TamSubscriber</code>
	 */
	public Subscriber getSubscriber() {
		return collector.getSubscriber();
	}
		
	/* (non-Javadoc)
	 * @see IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(TeamUI.GLOBAL_IGNORES_CHANGED)) {
			collector.reset();
		}	
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#init(org.eclipse.ui.IMemento)
	 */
	public void init(String secondaryId, IMemento memento) throws PartInitException {
		if(memento != null) {
			IMemento settings = memento.getChild(CTX_SUBSCRIBER_PARTICIPANT_SETTINGS);
			if(settings != null) {
				SubscriberRefreshSchedule schedule = SubscriberRefreshSchedule.init(settings.getChild(CTX_SUBSCRIBER_SCHEDULE_SETTINGS), this);
				setRefreshSchedule(schedule);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#saveState(org.eclipse.ui.IMemento)
	 */
	public void saveState(IMemento memento) {
		IMemento settings = memento.createChild(CTX_SUBSCRIBER_PARTICIPANT_SETTINGS);
		refreshSchedule.saveState(settings.createChild(CTX_SUBSCRIBER_SCHEDULE_SETTINGS));
	}
}