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
import org.eclipse.jface.action.Separator;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.ActionDelegateWrapper;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.IActionBars;

/**
 * Actions for the CVS workspace participant toolbar menu
 */
public class WorkspaceParticipantActionContributions extends CVSParticipantActionContribution {

	private ActionDelegateWrapper commitToolbar;
	private ActionDelegateWrapper updateToolbar;
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IContextMenuContribution#initialize(org.eclipse.jface.viewers.StructuredViewer)
	 */
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
		if (toolbar != null && toolbar.find(WorkspaceSynchronizeParticipant.ACTION_GROUP) != null) {
			toolbar.add(new Separator());
			toolbar.appendToGroup(WorkspaceSynchronizeParticipant.ACTION_GROUP, updateToolbar);
			toolbar.appendToGroup(WorkspaceSynchronizeParticipant.ACTION_GROUP, commitToolbar);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.subscriber.CVSParticipantActionContribution#modelChanged(org.eclipse.team.ui.synchronize.ISynchronizeModelElement)
	 */
	protected void modelChanged(ISynchronizeModelElement element) {
		commitToolbar.setSelection(element);
		updateToolbar.setSelection(element);
	}
}
