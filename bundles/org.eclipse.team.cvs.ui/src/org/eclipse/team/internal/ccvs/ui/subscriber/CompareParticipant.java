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
package org.eclipse.team.internal.ccvs.ui.subscriber;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoFilter;
import org.eclipse.team.internal.ccvs.core.CVSCompareSubscriber;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ui.synchronize.actions.*;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.team.ui.synchronize.subscribers.*;

public class CompareParticipant extends CVSParticipant {
	
	/**
	 * Toolbar actions for compare participants
	 */
	public class CompareParticipantActionContribution extends SubscriberParticipantActionContribution {
		public void initialize(ISynchronizePageConfiguration configuration) {
			createRemoveAction(configuration);
			super.initialize(configuration);
		}
		protected void modelChanged(ISynchronizeModelElement input) {
			// Nothing to do
		}
	}
	
	/**
	 * TODO: Need to add content filtering back
	 */
	private SyncInfoFilter contentComparison = new SyncInfoFilter() {
		private SyncInfoFilter contentCompare = new SyncInfoFilter.ContentComparisonSyncInfoFilter();
		public boolean select(SyncInfo info, IProgressMonitor monitor) {
			// Want to select infos whose contents do not match
			return !contentCompare.select(info, monitor);
		}
	};
	
	public CompareParticipant(CVSCompareSubscriber subscriber) {
		setSubscriber(subscriber);
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.subscriber.SubscriberParticipant#setSubscriber(org.eclipse.team.core.subscribers.Subscriber)
	 */
	protected void setSubscriber(Subscriber subscriber) {
		super.setSubscriber(subscriber);
		try {
			ISynchronizeParticipantDescriptor descriptor = TeamUI.getSynchronizeManager().getParticipantDescriptor(CVSCompareSubscriber.ID);
			setInitializationData(descriptor);
			CVSCompareSubscriber s = (CVSCompareSubscriber)getSubscriber();
			setSecondaryId(s.getId().getLocalName());
		} catch (CoreException e) {
			CVSUIPlugin.log(e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.AbstractSynchronizeParticipant#getName()
	 */
	public String getName() {
		return getSubscriber().getName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.subscriber.SubscriberParticipant#preCollectingChanges()
	 */
	protected void preCollectingChanges() {
		super.preCollectingChanges();
		// JEAN-MICHEL: how to handle filter configurability?
		//getSubscriberSyncInfoCollector().setFilter(contentComparison);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.AbstractSynchronizeParticipant#isPersistent()
	 */
	public boolean isPersistent() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.subscribers.SubscriberParticipant#initializeConfiguration(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration)
	 */
	protected void initializeConfiguration(ISynchronizePageConfiguration configuration) {
		super.initializeConfiguration(configuration);
		configuration.addActionContribution(new CompareParticipantActionContribution());
	}
}
