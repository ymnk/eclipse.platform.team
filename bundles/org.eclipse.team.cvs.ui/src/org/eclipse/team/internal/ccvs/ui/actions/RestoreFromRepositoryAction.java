/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.ui.ResizableWizardDialog;
import org.eclipse.team.internal.ccvs.ui.wizards.RestoreFromRepositoryWizard;

/**
 * @author Administrator
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class RestoreFromRepositoryAction extends CVSAction {

	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#execute(org.eclipse.jface.action.IAction)
	 */
	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		IResource resource = getSelectedResources()[0];
		RestoreFromRepositoryWizard wizard = new RestoreFromRepositoryWizard((IContainer)resource);
		WizardDialog dialog = new ResizableWizardDialog(shell, wizard);
		dialog.setMinimumPageSize(350, 250);
		dialog.open();
	}

	/**
	 * @see org.eclipse.team.internal.ui.actions.TeamAction#isEnabled()
	 */
	protected boolean isEnabled() throws TeamException {
		IResource[] resources = getSelectedResources();
		if (resources.length != 1) return false;
		if (resources[0].getType() == IResource.FILE) return false;
		ICVSFolder folder = CVSWorkspaceRoot.getCVSFolderFor((IContainer)resources[0]);
		if (!folder.isCVSFolder()) return false;
		return true;
	}

}
