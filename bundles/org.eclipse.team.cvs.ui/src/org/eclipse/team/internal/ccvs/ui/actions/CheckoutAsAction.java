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
package org.eclipse.team.internal.ccvs.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.TagetLocationSelectionDialog;
import org.eclipse.team.internal.ccvs.ui.operations.CheckoutMultipleProjectsOperation;
import org.eclipse.team.internal.ccvs.ui.operations.CheckoutSingleProjectOperation;
import org.eclipse.team.internal.ccvs.ui.operations.HasProjectMetaFileOperation;
import org.eclipse.team.internal.ccvs.ui.wizards.CheckoutAsWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.NewProjectAction;

/**
 * Add a remote resource to the workspace. Current implementation:
 * -Works only for remote folders
 * -Does not prompt for project name; uses folder name instead
 */
public class CheckoutAsAction extends AddToWorkspaceAction {
	
	/*
	 * @see IActionDelegate#run(IAction)
	 */
	public void execute(IAction action) throws InvocationTargetException, InterruptedException {
		final ICVSRemoteFolder[] folders = getSelectedRemoteFolders();
//		try {
//			if (folders.length == 1){
//				checkoutSingleProject(folders[0]);
//			} else {
//				checkoutMultipleProjects(folders);
//			}
//		} catch (CVSException e) {
//			throw new InvocationTargetException(e);
//		}
		try {
			CheckoutAsWizard wizard = new CheckoutAsWizard(folders, allowProjectConfiguration(folders));
			WizardDialog dialog = new WizardDialog(shell, wizard);
			dialog.open();
		} catch (CVSException e) {
			throw new InvocationTargetException(e);
		}
	}
	
	protected boolean allowProjectConfiguration(ICVSRemoteFolder[] folders) throws CVSException, InterruptedException {
		if (folders.length != 1) return false;
		return HasProjectMetaFileOperation.hasMetaFile(getShell(), folders[0], new ProgressMonitorDialog(shell));	
	}
			
	private void checkoutMultipleProjects(final ICVSRemoteFolder[] folders) throws CVSException, InterruptedException {
		
		// create the target project handles
		IProject[] targetProjects = new IProject[folders.length];
		for (int i = 0; i < folders.length; i++) {
			ICVSRemoteFolder remoteFolder = folders[i];
			targetProjects[i] = ResourcesPlugin.getWorkspace().getRoot().getProject(remoteFolder.getName());
		}
		
		// prompt for the parent location
		TagetLocationSelectionDialog dialog = new TagetLocationSelectionDialog(
			getShell(), 
			Policy.bind("CheckoutAsAction.enterLocationTitle", new Integer(targetProjects.length).toString()), //$NON-NLS-1$
			targetProjects);
		int result = dialog.open();
		if (result != Dialog.OK) return;
		String targetParentLocation = dialog.getTargetLocation();
			
		new CheckoutMultipleProjectsOperation(getShell(), folders, targetParentLocation)
			.execute(new ProgressMonitorDialog(shell));
	}

	private void checkoutSingleProject(final ICVSRemoteFolder remoteFolder) throws CVSException, InterruptedException {
		
		// Fetch the members of the folder to see if they contain a .project file.
		String remoteFolderName = remoteFolder.getName();
		boolean hasProjectMetaFile = HasProjectMetaFileOperation.hasMetaFile(getShell(), remoteFolder, new ProgressMonitorDialog(shell));
		
		IProject newProject = null;
		String targetLocation = null;
		if (hasProjectMetaFile) {
			// prompt for the project name and location
			newProject = ResourcesPlugin.getWorkspace().getRoot().getProject(remoteFolderName);
			TagetLocationSelectionDialog dialog = new TagetLocationSelectionDialog(getShell(), Policy.bind("CheckoutAsAction.enterProjectTitle", remoteFolderName), newProject); //$NON-NLS-1$
			int result = dialog.open();
			if (result != Dialog.OK) return;
			// get the name and location from the dialog
			targetLocation = dialog.getTargetLocation();
			newProject = ResourcesPlugin.getWorkspace().getRoot().getProject(dialog.getNewProjectName());
		} else {
			// Create a new file using the platform wizard so the project 
			// can be configured by the user
			newProject = getNewProject(remoteFolderName);
			if (newProject == null) return;
		}
		
		new CheckoutSingleProjectOperation(shell, remoteFolder, newProject, targetLocation, !hasProjectMetaFile)
			.execute(new ProgressMonitorDialog(shell));
	}
	
	/*
	 * @see TeamAction#isEnabled()
	 */
	protected boolean isEnabled() throws TeamException {
		return getSelectedRemoteFolders().length > 0;
	}

	/**
	 * Get a new project.
	 * 
	 * The suggestedName is not currently used but is a desired capability.
	 */
	private IProject getNewProject(String suggestedName) {
		NewProjectListener listener = new NewProjectListener();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);
		(new NewProjectAction(PlatformUI.getWorkbench().getActiveWorkbenchWindow())).run();
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(listener);
		// Ensure that the project only has a single member which is the .project file
		return listener.getNewProject();
	}
	
	class NewProjectListener implements IResourceChangeListener {
		private IProject newProject = null;
		/**
		 * @see IResourceChangeListener#resourceChanged(IResourceChangeEvent)
		 */
		public void resourceChanged(IResourceChangeEvent event) {
			IResourceDelta root = event.getDelta();
			IResourceDelta[] projectDeltas = root.getAffectedChildren();
			for (int i = 0; i < projectDeltas.length; i++) {							
				IResourceDelta delta = projectDeltas[i];
				IResource resource = delta.getResource();
				if (delta.getKind() == IResourceDelta.ADDED) {
					newProject = (IProject)resource;
				}
			}
		}
		/**
		 * Gets the newProject.
		 * @return Returns a IProject
		 */
		public IProject getNewProject() {
			return newProject;
		}
	}
	
	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#getErrorTitle()
	 */
	protected String getErrorTitle() {
		return Policy.bind("CheckoutAsAction.checkoutFailed"); //$NON-NLS-1$
	}
}
