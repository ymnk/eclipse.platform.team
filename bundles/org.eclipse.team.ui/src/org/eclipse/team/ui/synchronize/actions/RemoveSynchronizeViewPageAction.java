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
package org.eclipse.team.ui.synchronize.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;

public class RemoveSynchronizeViewPageAction extends Action {
	private ISynchronizeParticipant page;

	public RemoveSynchronizeViewPageAction(ISynchronizeParticipant page) {
		this.page = page;
		Utils.initAction(this, "action.removePage."); //$NON-NLS-1$
	}
	
	public void run() {
		TeamUI.getSynchronizeManager().removeSynchronizeParticipants(new ISynchronizeParticipant[] {page});
	}
}
