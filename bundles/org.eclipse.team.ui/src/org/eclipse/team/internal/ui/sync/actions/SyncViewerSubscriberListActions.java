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
package org.eclipse.team.internal.ui.sync.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.team.internal.ui.sync.views.SubscriberInput;
import org.eclipse.team.internal.ui.sync.views.SyncViewer;
import org.eclipse.ui.actions.ActionContext;

/**
 * SyncViewerSubscriberListActions
 */
public class SyncViewerSubscriberListActions extends SyncViewerActionGroup {

	private static final String MEMENTO_KEY = "SelectedComparisonCriteria";

	private List actions = new ArrayList();
	private SubscriberInput activeInput = null;

	/**
	 * Action for filtering by change type.
	 */
	class SwitchSubscriberAction extends Action {
		private SubscriberInput input;
		public SwitchSubscriberAction(SubscriberInput input) {
			super(input.getSubscriber().getName());
			this.input = input;
		}
		public void run() {
			SyncViewerSubscriberListActions.this.activate(this);
		}
		public SubscriberInput getSubscriberInput() {
			return input;
		}
	}

	public SyncViewerSubscriberListActions(SyncViewer syncView) {
		super(syncView);
		setContext(null);
	}

	public void activate(SwitchSubscriberAction activatedAction) {
		if(activeInput == null || ! activatedAction.getSubscriberInput().getSubscriber().getId().equals(activeInput.getSubscriber().getId())) {
			for (int i = 0; i < actions.size(); i++) {
				SwitchSubscriberAction action =
					(SwitchSubscriberAction) actions.get(i);
				action.setChecked(activatedAction == action);
			}
			final SyncViewer view = getSyncView();
			view.initializeSubscriberInput(activatedAction.getSubscriberInput());
		}
	}

	/*
	 * Called when a context is enabled for the view.
	 *  (non-Javadoc)
	 * @see SyncViewerActionGroup#initializeActions()
	 */
	public void initializeActions() {
		SubscriberInput input = getSubscriberContext();
		if (input != null) {
			for (Iterator it = actions.iterator(); it.hasNext();) {
				SwitchSubscriberAction action =
					(SwitchSubscriberAction) it.next();
				boolean checked = action.getSubscriberInput().getSubscriber().getId().equals(
														input.getSubscriber().getId());											
				action.setChecked(checked);
				if(checked) {
					activeInput = input;
				}
			}
		}
	}

   /* 
    * Checking of the currently active subscriber input is done when the context is set
    * in the initializeActions method. 
	* (non-Javadoc)
	* @see fillContextMenu(org.eclipse.jface.action.IMenuManager)
	*/
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		if (! actions.isEmpty()) {
			for (Iterator it = actions.iterator(); it.hasNext();) {
				SwitchSubscriberAction action = (SwitchSubscriberAction) it.next();
				menu.add(action);				
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.sync.actions.SyncViewerActionGroup#addContext(org.eclipse.ui.actions.ActionContext)
	 */
	public void addContext(ActionContext context) {
		actions.add(new SwitchSubscriberAction((SubscriberInput)context.getInput()));		
	}
	
	/* 
	 * Method to add menu items to a toolbar drop down action
	 */
	public void fillMenu(SyncViewerToolbarDropDownAction dropDown) {
		super.fillMenu(dropDown);
		if (! actions.isEmpty()) {
			for (Iterator it = actions.iterator(); it.hasNext();) {
				SwitchSubscriberAction action = (SwitchSubscriberAction) it.next();
				dropDown.add(action);				
			}
		}
	 }
}