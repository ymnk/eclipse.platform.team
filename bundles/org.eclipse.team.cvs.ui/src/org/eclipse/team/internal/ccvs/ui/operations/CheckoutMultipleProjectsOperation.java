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
package org.eclipse.team.internal.ccvs.ui.operations;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ui.PromptingDialog;

/**
 * This operation checks out a multiple remote folders into the workspace.
 * Each one will become a new project (overwritting any exsiting projects
 * with the same name).
 */
public class CheckoutMultipleProjectsOperation extends CheckoutOperation {

	Shell shell;
	ICVSRemoteFolder[] remoteFolders;
	IProjectDescription[] projectDescriptions;
	
	public CheckoutMultipleProjectsOperation(Shell shell, ICVSRemoteFolder[] remoteFolders, IProjectDescription[] projectDescriptions) {
		this.shell = shell;
		this.remoteFolders = remoteFolders;
		this.projectDescriptions = projectDescriptions;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.CVSOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void execute(IProgressMonitor monitor) throws CVSException, InterruptedException {
		IProject[] projects = getTargetProjects();
		if (projectDescriptions == null){
			checkoutToDefaultLocation(projects, monitor);
		} else {
			checkoutToCustomLocation(projects, monitor);
		}
	}
	
	/**
	 * @param projects
	 * @param monitor
	 */
	private void checkoutToDefaultLocation(IProject[] projects, IProgressMonitor monitor) throws InterruptedException, CVSException {
					
		PromptingDialog prompt = new PromptingDialog(getShell(), projects, 
													  getOverwriteLocalAndFileSystemPrompt(), 
													  Policy.bind("ReplaceWithAction.confirmOverwrite"));//$NON-NLS-1$
		IResource[] resources = prompt.promptForMultiple();
															
		monitor.beginTask(null, 100);
		if (resources.length != 0) {
			IProject[] localFolders = new IProject[resources.length];
			ICVSRemoteFolder[] remoteFolders = new ICVSRemoteFolder[resources.length];
			for (int i = 0; i < resources.length; i++) {
				localFolders[i] = (IProject)resources[i];
				remoteFolders[i] = getRemoteFolderNamed(resources[i].getName());
			}
						
			monitor.setTaskName(getTaskName(remoteFolders));						
			checkout(remoteFolders, localFolders, Policy.subMonitorFor(monitor, 100));
		}
		
	}

	/**
	 * @param string
	 * @return
	 */
	private ICVSRemoteFolder getRemoteFolderNamed(String string) {
		for (int i = 0; i < remoteFolders.length; i++) {
			ICVSRemoteFolder folder = remoteFolders[i];
			if (folder.getName().equals(string)) 
				return folder;
		}
		return null;
	}

	private void checkoutToCustomLocation(IProject[] projects, IProgressMonitor monitor) throws CVSException {
		String taskName = Policy.bind("CheckoutAsAction.multiCheckout", new Integer(projects.length).toString()); //$NON-NLS-1$
		monitor.beginTask(taskName, 100);
		monitor.setTaskName(taskName);
		// create the projects
		createAndOpenProjects(projects, projectDescriptions, Policy.subMonitorFor(monitor, 5));
		checkout(remoteFolders, projects, Policy.subMonitorFor(monitor, 95));
	}

	/**
	 * @return
	 */
	private IProject[] getTargetProjects() {
		IProject[] projects = new IProject[remoteFolders.length];
		for (int i = 0; i < projects.length; i++) {
			projects[i] = ResourcesPlugin.getWorkspace().getRoot().getProject(remoteFolders[i].getName());
		}
		return projects;
	}

	/**
	 * @return
	 */
	public Shell getShell() {
		return shell;
	}

}
