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
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.team.core.subscribers.SubscriberSyncInfoCollector;
import org.eclipse.team.core.subscribers.WorkingSetFilteredSyncInfoCollector;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.actions.SubscriberActionContribution;
import org.eclipse.team.ui.synchronize.SyncInfoSetSynchronizePage;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.subscribers.SubscriberParticipant;
import org.eclipse.ui.IWorkingSet;

/**
 * A synchronize view page that works with participants that are subclasses of 
 * {@link SubscriberParticipant}. It shows changes in the tree or table view
 * and supports navigation, opening, and filtering changes.
 * <p>
 * Clients can subclass to extend the label decoration or add action bar 
 * contributions. For more extensive modifications, clients should create
 * their own custom page.
 * </p> 
 * @since 3.0
 */
public final class SubscriberParticipantPage extends SyncInfoSetSynchronizePage implements IAdaptable {
	
	/** 
	 * Settings constant for section name (value <code>SubscriberParticipantPage</code>).
	 */
	private static final String STORE_SECTION_POSTFIX = "SubscriberParticipantPage"; //$NON-NLS-1$
	/** 
	 * Settings constant for working set (value <code>SubscriberParticipantPage</code>).
	 */
	private static final String STORE_WORKING_SET = "SubscriberParticipantPage.STORE_WORKING_SET"; //$NON-NLS-1$
	/** 
	 * Settings constant for working set (value <code>SubscriberParticipantPage</code>).
	 */
	private static final String STORE_MODE = "SubscriberParticipantPage.STORE_MODE"; //$NON-NLS-1$
	
	private IDialogSettings settings;
	
	private SubscriberParticipant participant;
	
	private final static int[] INCOMING_MODE_FILTER = new int[] {SyncInfo.CONFLICTING, SyncInfo.INCOMING};
	private final static int[] OUTGOING_MODE_FILTER = new int[] {SyncInfo.CONFLICTING, SyncInfo.OUTGOING};
	private final static int[] BOTH_MODE_FILTER = new int[] {SyncInfo.CONFLICTING, SyncInfo.INCOMING, SyncInfo.OUTGOING};
	private final static int[] CONFLICTING_MODE_FILTER = new int[] {SyncInfo.CONFLICTING};

	/**
	 * Filters out-of-sync resources by working set and mode
	 */
	private WorkingSetFilteredSyncInfoCollector collector;
		
	/**
	 * Constructs a new SynchronizeView.
	 */
	public SubscriberParticipantPage(ISynchronizePageConfiguration configuration, SubscriberSyncInfoCollector subscriberCollector) {
		super(configuration);
		this.participant = (SubscriberParticipant)configuration.getParticipant();
		IDialogSettings viewsSettings = TeamUIPlugin.getPlugin().getDialogSettings();
		
		String key = Utils.getKey(participant.getId(), participant.getSecondaryId());
		settings = viewsSettings.getSection(key + STORE_SECTION_POSTFIX);
		if (settings == null) {
			settings = viewsSettings.addNewSection(key + STORE_SECTION_POSTFIX);
		}
		
		configuration.addActionContribution(new SubscriberActionContribution());
		initializeCollector(configuration, subscriberCollector);
	}
	
	/**
	 * @return Returns the participant.
	 */
	public SubscriberParticipant getParticipant() {
		return participant;
	}
	
	protected void updateWorkingSet(IWorkingSet workingSet) {
		if(collector != null) {
			IResource[] resources = workingSet != null ? Utils.getResources(workingSet.getElements()) : new IResource[0];
			collector.setWorkingSet(resources);
		}
	}

	/*
	 * This method is invoked from <code>setMode</code> when the mode has changed.
	 * It sets the filter on the collector to show the <code>SyncInfo</code>
	 * appropriate for the mode.
	 * @param mode the new mode (one of <code>INCOMING_MODE_FILTER</code>,
	 * <code>OUTGOING_MODE_FILTER</code>, <code>CONFLICTING_MODE_FILTER</code>
	 * or <code>BOTH_MODE_FILTER</code>)
	 */
	protected void updateMode(int mode) {
		if(collector != null) {	
		
			int[] modeFilter = BOTH_MODE_FILTER;
			switch(mode) {
			case ISynchronizePageConfiguration.INCOMING_MODE:
				modeFilter = INCOMING_MODE_FILTER; break;
			case ISynchronizePageConfiguration.OUTGOING_MODE:
				modeFilter = OUTGOING_MODE_FILTER; break;
			case ISynchronizePageConfiguration.BOTH_MODE:
				modeFilter = BOTH_MODE_FILTER; break;
			case ISynchronizePageConfiguration.CONFLICTING_MODE:
				modeFilter = CONFLICTING_MODE_FILTER; break;
			}

			collector.setFilter(
					new FastSyncInfoFilter.AndSyncInfoFilter(
							new FastSyncInfoFilter[] {
									new FastSyncInfoFilter.SyncInfoDirectionFilter(modeFilter)
							}));
		}
	}
	
	private void initializeCollector(ISynchronizePageConfiguration configuration, SubscriberSyncInfoCollector subscriberCollector) {
		SubscriberParticipant participant = getParticipant();
		collector = new WorkingSetFilteredSyncInfoCollector(subscriberCollector, participant.getSubscriber().roots());
		collector.reset();
		configuration.setProperty(ISynchronizePageConfiguration.P_SYNC_INFO_SET, collector.getSyncInfoTree());
		configuration.setProperty(ISynchronizePageConfiguration.P_WORKING_SET_SYNC_INFO_SET, collector.getWorkingSetSyncInfoSet());
	}
	
	public boolean isThreeWay() {
		return getParticipant().getSubscriber().getResourceComparator().isThreeWay();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.AbstractSynchronizePage#reset()
	 */
	public void reset() {
		getParticipant().reset();
	}
}