/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.subscriber;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.ActionDelegateWrapper;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.subscribers.ISubscriberPageConfiguration;
import org.eclipse.ui.IActionBars;

/**
 * Actions for the merge particpant's toolbar
 */
public class MergeParticipantActionContribution extends CVSParticipantActionContribution {
	
	private ActionDelegateWrapper updateAdapter;
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.subscriber.CVSParticipantActionContribution#initialize(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration)
	 */
	public void initialize(ISynchronizePageConfiguration configuration) {
		createRemoveAction(configuration);
		((ISubscriberPageConfiguration)configuration).setSupportedModes(ISubscriberPageConfiguration.INCOMING_MODE | ISubscriberPageConfiguration.CONFLICTING_MODE);
		MergeUpdateAction action = new MergeUpdateAction();
		action.setPromptBeforeUpdate(true);
		updateAdapter = new ActionDelegateWrapper(action, configuration.getSite().getPart());
		Utils.initAction(updateAdapter, "action.SynchronizeViewUpdate.", Policy.getBundle()); //$NON-NLS-1$
		((ISubscriberPageConfiguration)configuration).setMode(ISubscriberPageConfiguration.INCOMING_MODE);
		super.initialize(configuration);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.subscriber.CVSParticipantActionContribution#modelChanged(org.eclipse.team.ui.synchronize.ISynchronizeModelElement)
	 */
	protected void modelChanged(ISynchronizeModelElement input) {
		updateAdapter.setSelection(input);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IActionContribution#setActionBars(org.eclipse.ui.IActionBars)
	 */
	public void setActionBars(IActionBars actionBars) {
		if(actionBars != null) {
			IToolBarManager toolbar = actionBars.getToolBarManager();
			if(toolbar != null) {
				if(toolbar.find(MergeSynchronizeParticipant.ACTION_GROUP) != null) {
					toolbar.appendToGroup(MergeSynchronizeParticipant.ACTION_GROUP, updateAdapter);
				}
			}
		}	
		super.setActionBars(actionBars);
	}
}
