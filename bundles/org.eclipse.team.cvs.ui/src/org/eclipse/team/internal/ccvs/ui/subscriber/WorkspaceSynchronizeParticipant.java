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
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.ActionDelegateWrapper;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.subscribers.ISubscriberPageConfiguration;
import org.eclipse.ui.*;

public class WorkspaceSynchronizeParticipant extends CVSParticipant {

	public static final String ID = "org.eclipse.team.cvs.ui.cvsworkspace-participant"; //$NON-NLS-1$

	/**
	 * The id of a workspace action group to which additions actions can 
	 * be added.
	 */
	public static final String ACTION_GROUP = "cvs_workspace_actions"; //$NON-NLS-1$
	
	/**
	 * CVS workspace action contribution
	 */
	public class WorkspaceActionContribution extends CVSParticipantActionContribution {
		private ActionDelegateWrapper commitToolbar;
		private ActionDelegateWrapper updateToolbar;
		public void initialize(ISynchronizePageConfiguration configuration) {
			commitToolbar = new ActionDelegateWrapper(new SubscriberCommitAction(), configuration.getSite().getPart());
			WorkspaceUpdateAction action = new WorkspaceUpdateAction();
			action.setPromptBeforeUpdate(true);
			updateToolbar = new ActionDelegateWrapper(action, configuration.getSite().getPart());
			Utils.initAction(commitToolbar, "action.SynchronizeViewCommit.", Policy.getBundle()); //$NON-NLS-1$
			Utils.initAction(updateToolbar, "action.SynchronizeViewUpdate.", Policy.getBundle()); //$NON-NLS-1$
			super.initialize(configuration);
		}
		public void setActionBars(IActionBars actionBars) {
			IToolBarManager toolbar = actionBars.getToolBarManager();
			if (toolbar != null && toolbar.find(ACTION_GROUP) != null) {
				toolbar.add(new Separator());
				toolbar.appendToGroup(ACTION_GROUP, updateToolbar);
				toolbar.appendToGroup(ACTION_GROUP, commitToolbar);
			}
			super.setActionBars(actionBars);
		}
		protected void modelChanged(ISynchronizeModelElement element) {
			commitToolbar.setSelection(element);
			updateToolbar.setSelection(element);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#init(org.eclipse.ui.IMemento)
	 */
	public void init(String secondaryId, IMemento memento) throws PartInitException {
		super.init(secondaryId, memento);
		Subscriber subscriber = CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber(); 
		setSubscriber(subscriber);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.subscribers.SubscriberParticipant#initializeConfiguration(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration)
	 */
	protected void initializeConfiguration(ISynchronizePageConfiguration configuration) {
		super.initializeConfiguration(configuration);
		configuration.setProperty(ISynchronizePageConfiguration.P_TOOLBAR_MENU, 
			new String[] { 
				ISynchronizePageConfiguration.SYNCHRONIZE_GROUP,  
				ISynchronizePageConfiguration.NAVIGATE_GROUP, 
				ISynchronizePageConfiguration.MODE_GROUP, 
				ACTION_GROUP
			});
		configuration.addActionContribution(new WorkspaceActionContribution());
		((ISubscriberPageConfiguration)configuration).setSupportedModes(ISubscriberPageConfiguration.ALL_MODES);
	}
}