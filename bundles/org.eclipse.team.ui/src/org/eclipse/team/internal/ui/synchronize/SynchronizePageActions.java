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
import org.eclipse.team.internal.ui.synchronize.actions.OpenWithActionGroup;
import org.eclipse.team.internal.ui.synchronize.actions.RefactorActionGroup;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.*;

/**
 * General synchronize page actions
 */
public class SynchronizePageActions implements IActionContribution {
	
	// Actions
	private OpenWithActionGroup openWithActions;
	private RefactorActionGroup refactorActions;
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IActionContribution#initialize(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration)
	 */
	public void initialize(ISynchronizePageConfiguration configuration) {
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
		if (openWithActions != null && manager.find(ISynchronizePageConfiguration.FILE_GROUP) != null) {
			openWithActions.fillContextMenu(manager, ISynchronizePageConfiguration.FILE_GROUP);
		}
		if (refactorActions != null && manager.find(ISynchronizePageConfiguration.EDIT_GROUP) != null) {
			refactorActions.fillContextMenu(manager, ISynchronizePageConfiguration.EDIT_GROUP);
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
