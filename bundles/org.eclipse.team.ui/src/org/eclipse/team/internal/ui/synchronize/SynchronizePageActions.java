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
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.actions.*;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.team.ui.synchronize.IActionContribution;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.*;

/**
 * General synchronize page actions
 */
public class SynchronizePageActions implements IActionContribution {
	
	// Actions
	private OpenWithActionGroup openWithActions;
	private RefactorActionGroup refactorActions;
	private ExpandAllAction expandAllAction;
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IActionContribution#initialize(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration)
	 */
	public void initialize(ISynchronizePageConfiguration configuration) {
		final StructuredViewer viewer = configuration.getAdviser().getViewer();
		if (viewer instanceof AbstractTreeViewer) {
			expandAllAction = new ExpandAllAction((AbstractTreeViewer) viewer);
			Utils.initAction(expandAllAction, "action.expandAll."); //$NON-NLS-1$
		}
		ISynchronizePageSite site = configuration.getSite();
		IWorkbenchSite ws = site.getWorkbenchSite();
		if (ws instanceof IViewSite) {
			openWithActions = new OpenWithActionGroup(site, configuration.getParticipant().getName());
			refactorActions = new RefactorActionGroup(site);
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IActionContribution#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	public void fillContextMenu(IMenuManager manager) {
		if (openWithActions != null && manager.find(FILE_MENU) != null) {
			openWithActions.fillContextMenu(manager, FILE_MENU);
		}
		if (refactorActions != null && manager.find(EDIT_MENU) != null) {
			refactorActions.fillContextMenu(manager, EDIT_MENU);
		}
		if (expandAllAction != null  && manager.find(NAVIGATE_MENU) != null) {
			manager.insertAfter(NAVIGATE_MENU, expandAllAction);
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IActionContribution#setActionBars(org.eclipse.ui.IActionBars)
	 */
	public void setActionBars(IActionBars actionBars) {
		// TODO Auto-generated method stub
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IActionContribution#dispose()
	 */
	public void dispose() {
		// TODO Auto-generated method stub
	}
}
