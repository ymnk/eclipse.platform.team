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
import org.eclipse.team.core.subscribers.WorkingSetFilteredSyncInfoCollector;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.actions.*;
import org.eclipse.team.ui.synchronize.subscribers.*;
import org.eclipse.ui.IWorkingSet;


/**
 * A <code>SynchronizeConfiguration</code> object controls various UI aspects of 
 * synchronization viewers.
 * <p>
 * Clients may use this class as is, or subclass to add new state and behavior.
 * </p>
 * @since 3.0
 */
public class SubscriberPageConfiguration extends SynchronizePageConfiguration implements ISubscriberPageConfiguration {

	/**
	 * Private property that gives access to a set the
	 * contains all out-of-sync resources in the selected working
	 * set.
	 */
	public static final String P_WORKING_SET_SYNC_INFO_SET = TeamUIPlugin.ID + ".P_WORKING_SET_SYNC_INFO_SET"; //$NON-NLS-1$
	
	private final static int[] INCOMING_MODE_FILTER = new int[] {SyncInfo.CONFLICTING, SyncInfo.INCOMING};
	private final static int[] OUTGOING_MODE_FILTER = new int[] {SyncInfo.CONFLICTING, SyncInfo.OUTGOING};
	private final static int[] BOTH_MODE_FILTER = new int[] {SyncInfo.CONFLICTING, SyncInfo.INCOMING, SyncInfo.OUTGOING};
	private final static int[] CONFLICTING_MODE_FILTER = new int[] {SyncInfo.CONFLICTING};

	/**
	 * Filters out-of-sync resources by working set and mode
	 */
	private WorkingSetFilteredSyncInfoCollector collector;
	
	public SubscriberPageConfiguration(SubscriberParticipant participant) {
		super(participant);
		addActionContribution(new SubscriberActionContribution());
		initializeCollector();
	}
	
	private void initializeCollector() {
		SubscriberParticipant participant = (SubscriberParticipant)getParticipant();
		collector = new WorkingSetFilteredSyncInfoCollector(participant.getSubscriberSyncInfoCollector(), participant.getSubscriber().roots());
		collector.reset();
		setProperty(P_SYNC_INFO_SET, collector.getSyncInfoTree());
		setProperty(P_WORKING_SET_SYNC_INFO_SET, collector.getWorkingSetSyncInfoSet());
	}

	public void dispose() {
		collector.dispose();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.subscribers.ISubscriberPageConfiguration#getWorkingSet()
	 */
	public IWorkingSet getWorkingSet() {
		Object o = getProperty(P_WORKING_SET);
		if (o instanceof IWorkingSet) {
			return (IWorkingSet)o;
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.subscribers.ISubscriberPageConfiguration#getMode()
	 */
	public int getMode() {
		Object o = getProperty(P_MODE);
		if (o instanceof Integer) {
			return ((Integer)o).intValue();
		}
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration#setProperty(java.lang.String, java.lang.Object)
	 */
	public void setProperty(String key, Object newValue) {
		if (key.equals(P_MODE)) {
			if (internalSetMode(((Integer)newValue).intValue())) {
				super.setProperty(key, newValue);
			} else {
				return;
			}
		}
		if (key.equals(P_WORKING_SET)) {
			if (internalSetWorkingSet((IWorkingSet)newValue)) {
				super.setProperty(key, newValue);
			} else {
				return;
			}
		}
		super.setProperty(key, newValue);
	}
	
	private boolean internalSetMode(int mode) {
		int oldMode = getMode();
		if(oldMode == mode) return false;
		updateMode(mode);
		return true;
	}
	
	private boolean internalSetWorkingSet(IWorkingSet workingSet) {
		IWorkingSet oldSet = getWorkingSet();
		if (workingSet == null || !workingSet.equals(oldSet)) {
			updateWorkingSet(workingSet);
			return true;
		}
		return false;
	}
	
	private void updateWorkingSet(IWorkingSet workingSet) {
		if(collector != null) {
			IResource[] resources = workingSet != null ? Utils.getResources(workingSet.getElements()) : new IResource[0];
			collector.setWorkingSet(resources);
		}
	}

	public int getSupportedModes() {
		Object o = getProperty(P_SUPPORTED_MODES);
		if (o instanceof Integer) {
			return ((Integer)o).intValue();
		}
		return 0;
	}
	
	/**
	 * This method is invoked from <code>setMode</code> when the mode has changed.
	 * It sets the filter on the collector to show the <code>SyncInfo</code>
	 * appropriate for the mode.
	 * @param mode the new mode (one of <code>INCOMING_MODE_FILTER</code>,
	 * <code>OUTGOING_MODE_FILTER</code>, <code>CONFLICTING_MODE_FILTER</code>
	 * or <code>BOTH_MODE_FILTER</code>)
	 */
	private void updateMode(int mode) {
		if(collector != null) {	
		
			int[] modeFilter = BOTH_MODE_FILTER;
			switch(mode) {
			case INCOMING_MODE:
				modeFilter = INCOMING_MODE_FILTER; break;
			case OUTGOING_MODE:
				modeFilter = OUTGOING_MODE_FILTER; break;
			case BOTH_MODE:
				modeFilter = BOTH_MODE_FILTER; break;
			case CONFLICTING_MODE:
				modeFilter = CONFLICTING_MODE_FILTER; break;
			}

			collector.setFilter(
					new FastSyncInfoFilter.AndSyncInfoFilter(
							new FastSyncInfoFilter[] {
									new FastSyncInfoFilter.SyncInfoDirectionFilter(modeFilter)
							}));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.subscribers.ISubscriberPageConfiguration#setWorkingSet(org.eclipse.ui.IWorkingSet)
	 */
	public void setWorkingSet(IWorkingSet set) {
		setProperty(P_WORKING_SET, set);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.subscribers.ISubscriberPageConfiguration#setMode(int)
	 */
	public void setMode(int mode) {
		setProperty(P_MODE, new Integer(mode));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.subscribers.ISubscriberPageConfiguration#setSupportedModes(int)
	 */
	public void setSupportedModes(int modes) {
		setProperty(P_SUPPORTED_MODES, new Integer(modes));
	}
}
