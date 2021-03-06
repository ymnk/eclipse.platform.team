/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Philippe Ombredanne - bug 84808
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.operations.ProjectMetaFileOperation;
import org.eclipse.team.internal.ccvs.ui.wizards.CheckoutAsWizard;

public class CheckoutAsAction extends CVSAction {
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#execute(org.eclipse.jface.action.IAction)
	 */
	public void execute(IAction action) throws InvocationTargetException, InterruptedException {
		ICVSRemoteFolder[] folders = getSelectedRemoteFolders();
		boolean withName = CVSUIPlugin.getPlugin().isUseProjectNameOnCheckout();
		ProjectMetaFileOperation op = new ProjectMetaFileOperation(getTargetPart(), folders, withName);
		op.run();
		
		// project configuration allowed only if single folder without metafile
		boolean allowProjectConfig = (folders.length == 1 && !op.metaFileExists());
		
		if (withName) {
			folders = op.getUpdatedFolders();
		}
		
		CheckoutAsWizard wizard = new CheckoutAsWizard(getTargetPart(), folders, allowProjectConfig);
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		dialog.open();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.actions.TeamAction#isEnabled()
	 */
	public boolean isEnabled() {
		return getSelectedRemoteFolders().length > 0;
	}
}
