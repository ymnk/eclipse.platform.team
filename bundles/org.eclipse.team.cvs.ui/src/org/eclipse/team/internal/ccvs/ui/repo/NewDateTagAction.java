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
package org.eclipse.team.internal.ccvs.ui.repo;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.window.Window;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.util.CVSDateFormatter;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.DateTagDialog;

/**
 * Action for creating a CVS Date tag.
 */
public class NewDateTagAction extends CVSRepoViewAction {

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#execute(org.eclipse.jface.action.IAction)
	 */
	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		ICVSRepositoryLocation[] locations = getSelectedRepositoryLocations();
		if (locations.length != 1) return;
		DateTagDialog dialog = new DateTagDialog(getShell());
		if (dialog.open() == Window.OK) {
			Date date = dialog.getDate();
			String dateString = CVSDateFormatter.dateToServerStamp(date);
			CVSTag tag = new CVSTag(dateString, CVSTag.DATE);
			CVSUIPlugin.getPlugin().getRepositoryManager().addDateTag(locations[0], tag);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.actions.TeamAction#isEnabled()
	 */
	protected boolean isEnabled() {
		ICVSRepositoryLocation[] locations = getSelectedRepositoryLocations();
		if (locations.length != 1) return false;
		return true;
	}

}
