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

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.team.core.synchronize.SyncInfoTree;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.ActionDelegateWrapper;
import org.eclipse.team.internal.ui.synchronize.actions.RemoveSynchronizeParticipantAction;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.team.ui.synchronize.subscribers.*;
import org.eclipse.team.ui.synchronize.subscribers.DirectionFilterActionGroup;
import org.eclipse.ui.IActionBars;


public class MergeSynchronizeAdvisor extends CVSSynchronizeViewerAdvisor {

	private RemoveSynchronizeParticipantAction removeAction;
	private DirectionFilterActionGroup modes;
	private ActionDelegateWrapper updateAdapter;
	
	public MergeSynchronizeAdvisor(SubscriberPageConfiguration configuration, SyncInfoTree syncInfoTree) {
		super(configuration, syncInfoTree);		
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.subscriber.SynchronizeViewerAdvisor#initializeActions(org.eclipse.jface.viewers.StructuredViewer)
	 */
	protected void initializeActions(StructuredViewer treeViewer) {
		super.initializeActions(treeViewer);
		
		SubscriberParticipant p = getParticipant();
		removeAction = new RemoveSynchronizeParticipantAction(p);
		SubscriberPageConfiguration configuration = getConfiguration();
		configuration.setSupportedModes(SubscriberPageConfiguration.INCOMING_MODE | SubscriberPageConfiguration.CONFLICTING_MODE);
		modes = new DirectionFilterActionGroup(configuration);
		MergeUpdateAction action = new MergeUpdateAction();
		action.setPromptBeforeUpdate(true);
		updateAdapter = new ActionDelegateWrapper(action, getSynchronizeView());
		Utils.initAction(updateAdapter, "action.SynchronizeViewUpdate.", Policy.getBundle()); //$NON-NLS-1$
		configuration.setMode(SubscriberPageConfiguration.INCOMING_MODE);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IPage#setActionBars(org.eclipse.ui.IActionBars)
	 */
	public void setActionBars(IActionBars actionBars) {		
		super.setActionBars(actionBars);
		if(actionBars != null) {
			IToolBarManager toolbar = actionBars.getToolBarManager();
			if(toolbar != null) {
				toolbar.add(new Separator());
				modes.fillToolBar(toolbar);
				toolbar.add(new Separator());
				toolbar.add(updateAdapter);
				toolbar.add(removeAction);
			}
		}		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.subscriber.CVSSynchronizeViewerAdvisor#getActionDelegates()
	 */
	protected ActionDelegateWrapper[] getActionDelegates() {
		// Returned so that the superclass will forward model changes
		return new ActionDelegateWrapper[] { updateAdapter };
	}
}