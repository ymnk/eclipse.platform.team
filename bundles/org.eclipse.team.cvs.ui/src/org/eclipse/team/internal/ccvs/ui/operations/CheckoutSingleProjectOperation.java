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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.ui.Policy;

/**
 * This operation checks out a single remote folder into the workspace as
 * a project.
 */
public class CheckoutSingleProjectOperation extends CheckoutOperation {

	private boolean preconfigured;
	private ICVSRemoteFolder remoteFolder;
	private IProject targetProject;
	private IProjectDescription projectDescription;
	
	public CheckoutSingleProjectOperation(ICVSRemoteFolder remoteFolder, IProject targetProject, IProjectDescription projectDescription, boolean preconfigured) {
		this.remoteFolder = remoteFolder;
		this.projectDescription = projectDescription;
		this.targetProject = targetProject;
		this.preconfigured = preconfigured;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.CVSOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void execute(IProgressMonitor monitor) throws CVSException, InterruptedException {
		try {
			String taskName = Policy.bind("CheckoutAsAction.taskname", getRemoteFolderName(), targetProject.getName()); //$NON-NLS-1$
			monitor.beginTask(taskName, 100);
			monitor.setTaskName(taskName);
			int used = 0;
			if (!isPreconfigured()) {
				used = 5;
				createAndOpenProject(targetProject, getProjectDescription(), Policy.subMonitorFor(monitor, used));
			}
			checkout(new ICVSRemoteFolder[] { remoteFolder }, new IProject[] { targetProject }, Policy.subMonitorFor(monitor, 100 - used));
		} catch (TeamException e) {
			throw CVSException.wrapException(e);
		} finally {
			monitor.done();
		}

	}
	
	/**
	 * 
	 */
	private IProjectDescription getProjectDescription() {
		return projectDescription;
	}
	
	/**
	 * @return
	 */
	private String getRemoteFolderName() {
		// TODO Auto-generated method stub
		return remoteFolder.getName();
	}

	/**
	 * @return
	 */
	private boolean isPreconfigured() {
		return preconfigured;
	}

}
