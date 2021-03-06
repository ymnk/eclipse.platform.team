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
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.operations.CheckoutMultipleProjectsOperation;
import org.eclipse.team.internal.ccvs.ui.operations.ProjectMetaFileOperation;

/**
 * Checkout a remote module into the workspace ensuring that the user is prompted for
 * any overwrites that may occur.
 */
public class CheckoutAction extends CVSAction {
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#execute(org.eclipse.jface.action.IAction)
	 */
	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		new CheckoutMultipleProjectsOperation(getTargetPart(), getSelectedRemoteFoldersWithProjectName(), null)
			.run();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.actions.TeamAction#isEnabled()
	 */
   public boolean isEnabled() {
	   ICVSRemoteFolder[] folders = getSelectedRemoteFolders();
	   if (folders.length == 0) return false;
	   // only enabled when all folders are in the same repository
	   ICVSRepositoryLocation location = folders[0].getRepository();
	   for (int i = 1; i < folders.length; i++) {
		   ICVSRemoteFolder folder = folders[i];
		   if (!folder.getRepository().equals(location)) {
			   return false;
		   }
	   }
	   return true;
   }

	/**
	 * Get selected CVS remote folders, and add Project Description 
	 * from metafile if available based on preferences settings
	 * @throws InterruptedException 
	 * @throws InvocationTargetException 
	 */
	private ICVSRemoteFolder[] getSelectedRemoteFoldersWithProjectName() throws InvocationTargetException, InterruptedException {
		ICVSRemoteFolder[] folders = getSelectedRemoteFolders();
		if (CVSUIPlugin.getPlugin().isUseProjectNameOnCheckout()){
			folders = ProjectMetaFileOperation.updateFoldersWithProjectName(getTargetPart(), folders);
		}
		return folders;
	}
}
