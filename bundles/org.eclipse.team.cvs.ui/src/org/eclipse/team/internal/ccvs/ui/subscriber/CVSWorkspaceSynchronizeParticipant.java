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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.core.subscribers.TeamSubscriber;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.ui.Utilities;
import org.eclipse.team.ui.sync.actions.DirectionFilterActionGroup;
import org.eclipse.ui.IActionBars;

public class CVSWorkspaceSynchronizeParticipant extends CVSSynchronizeParticipant {
	
	private DirectionFilterActionGroup modes;
	private Action commitAdapter;
	private Action updateAdapter;
	
	public CVSWorkspaceSynchronizeParticipant(TeamSubscriber subscriber, String name, ImageDescriptor imageDescriptor, int num) {
		super(subscriber, name, imageDescriptor);
		modes = new DirectionFilterActionGroup(this, ALL_MODES);		
		commitAdapter = new CVSActionDelegate(new SubscriberCommitAction(), this);
		updateAdapter = new CVSActionDelegate(new WorkspaceUpdateAction(), this);
		Utilities.initAction(commitAdapter, "action.SynchronizeViewCommit.", Policy.getBundle());
		Utilities.initAction(updateAdapter, "action.SynchronizeViewUpdate.", Policy.getBundle());

	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.SubscriberPage#setActionsBars(org.eclipse.ui.IActionBars)
	 */
	public void setActionsBars(IActionBars actionBars, IToolBarManager detailsToolbar) {
		if(actionBars != null) {
			IToolBarManager toolbar = actionBars.getToolBarManager();
			toolbar.add(new Separator());		
			toolbar.add(new Separator());
			actionBars.getToolBarManager().add(updateAdapter);
			actionBars.getToolBarManager().add(commitAdapter);
		}
		if(detailsToolbar != null) {
			modes.fillToolBar(detailsToolbar);
		}
	}
}
