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
import org.eclipse.team.internal.ccvs.core.CVSMergeSubscriber;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.ui.Utilities;
import org.eclipse.team.ui.sync.actions.DirectionFilterActionGroup;
import org.eclipse.team.ui.sync.actions.RemoveSynchronizeViewPageAction;
import org.eclipse.ui.IActionBars;

public class CVSMergeSynchronizeParticipant extends CVSSynchronizeParticipant {

	private RemoveSynchronizeViewPageAction removeAction;
	private DirectionFilterActionGroup modes;
	private Action updateAdapter;
	
	public CVSMergeSynchronizeParticipant(CVSMergeSubscriber subscriber, String name, ImageDescriptor imageDescriptor) {
		super(subscriber, name, imageDescriptor);
		makeActions();
	}
		
	private void makeActions() {
		removeAction = new RemoveSynchronizeViewPageAction(this);
		modes = new DirectionFilterActionGroup(this, INCOMING_MODE | CONFLICTING_MODE | BOTH_MODE);
		updateAdapter = new CVSActionDelegate(new WorkspaceUpdateAction(), this);
		Utilities.initAction(updateAdapter, "action.SynchronizeViewUpdate.", Policy.getBundle());
		setMode(INCOMING_MODE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.SubscriberPage#setActionsBars(org.eclipse.ui.IActionBars)
	 */
	public void setActionsBars(IActionBars actionBars) {		
		super.setActionsBars(actionBars);
		IToolBarManager toolbar = actionBars.getToolBarManager();
		toolbar.add(new Separator());
		modes.fillActionBars(actionBars, null);
		toolbar.add(new Separator());
		actionBars.getToolBarManager().add(updateAdapter);
		actionBars.getToolBarManager().add(removeAction);		
	}
}
