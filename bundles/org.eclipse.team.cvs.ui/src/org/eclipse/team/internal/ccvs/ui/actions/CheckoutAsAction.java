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

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.TagetLocationSelectionDialog;
import org.eclipse.team.internal.ccvs.ui.operations.CheckoutMultipleProjectsOperation;
import org.eclipse.team.internal.ccvs.ui.operations.CheckoutSingleProjectOperation;
import org.eclipse.team.internal.ccvs.ui.operations.HasProjectMetaFileOperation;
import org.eclipse.team.internal.ui.IPromptCondition;
import org.eclipse.team.internal.ui.PromptingDialog;
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
		ICVSRemoteFolder[] folders = getSelectedRemoteFolders();
		try {
			if (folders.length == 1){
				checkoutSingleProject(folders[0]);
			} else {
				checkoutMultipleProjects(folders);
			}
		} catch (CVSException e) {
			throw new InvocationTargetException(e);
		}
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
			
		// if the location is null, just checkout the projects into the workspace
		if (targetParentLocation == null) {
			new CheckoutMultipleProjectsOperation(getShell(), folders, null)
				.execute(new ProgressMonitorDialog(shell));
			return;
		}
		
		// create the project descriptions for each project
		IProjectDescription[] descriptions = new IProjectDescription[targetProjects.length];
		for (int i = 0; i < targetProjects.length; i++) {
			String projectName = targetProjects[i].getName();
			descriptions[i] = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
			descriptions[i].setLocation(new Path(targetParentLocation).append(projectName));
		}
			
		// prompt if the projects or locations exist locally
		PromptingDialog prompt = new PromptingDialog(getShell(), targetProjects,
			getOverwriteLocalAndFileSystemPrompt(descriptions), Policy.bind("ReplaceWithAction.confirmOverwrite"));//$NON-NLS-1$
		IResource[] projectsToCheckout = prompt.promptForMultiple();
		if (projectsToCheckout.length== 0) return;
		
		// copy the selected projects to a new array
		final IProject[] projects = new IProject[projectsToCheckout.length];
		for (int i = 0; i < projects.length; i++) {
			projects[i] = projectsToCheckout[i].getProject();
		}
		
		// perform the checkout
		// TODO: The selected projects neew to be used to determine which folders are still 
		// to be checked out
		new CheckoutMultipleProjectsOperation(getShell(), folders, descriptions)
			.execute(new ProgressMonitorDialog(shell));
	}

	private void checkoutSingleProject(final ICVSRemoteFolder remoteFolder) throws CVSException, InterruptedException {
		// Fetch the members of the folder to see if they contain a .project file.
		final String remoteFolderName = remoteFolder.getName();
		
		boolean hasProjectMetaFile = HasProjectMetaFileOperation.hasMetaFile(remoteFolder, new ProgressMonitorDialog(shell));
		
		// Prompt outside a workspace runnable so that the project creation delta can be heard
		IProject newProject = null;
		IProjectDescription newDesc = null;
		if (hasProjectMetaFile) {
			
			// prompt for the project name and location
			newProject = ResourcesPlugin.getWorkspace().getRoot().getProject(remoteFolderName);
			TagetLocationSelectionDialog dialog = new TagetLocationSelectionDialog(getShell(), Policy.bind("CheckoutAsAction.enterProjectTitle", remoteFolderName), newProject); //$NON-NLS-1$
			int result = dialog.open();
			if (result != Dialog.OK) return;
			// get the name and location from the dialog
			String targetLocation = dialog.getTargetLocation();
			String targetName = dialog.getNewProjectName();
			
			// create the project description for a custom location
			if (targetLocation != null) {
				newDesc = ResourcesPlugin.getWorkspace().newProjectDescription(newProject.getName());
				newDesc.setLocation(new Path(targetLocation));
			}
			
			// prompt if the project or location exists locally
			newProject = ResourcesPlugin.getWorkspace().getRoot().getProject(targetName);
			PromptingDialog prompt = new PromptingDialog(getShell(), new IResource[] { newProject },
				getOverwriteLocalAndFileSystemPrompt(
					newDesc == null ? new IProjectDescription[0] : new IProjectDescription[] {newDesc}), 
					Policy.bind("ReplaceWithAction.confirmOverwrite"));//$NON-NLS-1$
			if (prompt.promptForMultiple().length == 0) return;
			
		} else {
			newProject = getNewProject(remoteFolderName);
			if (newProject == null) return;
		}
		
		new CheckoutSingleProjectOperation(remoteFolder, newProject, newDesc, !hasProjectMetaFile)
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
		IProject project = listener.getNewProject();
		if (project == null) return null;
		try {
			IResource[] members = project.members();
			if ((members.length == 0) 
				||(members.length == 1 && members[0].getName().equals(".project"))) { //$NON-NLS-1$
				return project;
			} else {
				// prompt to overwrite
				PromptingDialog prompt = new PromptingDialog(getShell(), new IProject[] { project }, 
						getOverwriteLocalAndFileSystemPrompt(), 
						Policy.bind("ReplaceWithAction.confirmOverwrite"));//$NON-NLS-1$
				try {
					if (prompt.promptForMultiple().length == 1) return project;
				} catch (InterruptedException e) {
				}
			}
		} catch (CoreException e) {
			handle(e);
		}
		return null;
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
	
	protected IPromptCondition getOverwriteLocalAndFileSystemPrompt(final IProjectDescription[] descriptions) {
		return new IPromptCondition() {
			// prompt if resource in workspace exists or exists in local file system
			public boolean needsPrompt(IResource resource) {
				
				// First, check the description location
				IProjectDescription desc = findDescription(descriptions, resource);
				if (desc != null) {
					File localLocation = desc.getLocation().toFile();
					return localLocation.exists();
				}
				
				// Next, check if the resource itself exists
				if (resource.exists()) return true;
				
				// Finally, check if the location in the workspace exists;
				File localLocation  = getFileLocation(resource);
				if (localLocation.exists()) return true;
				
				// The target doesn't exist
				return false;
			}
			public String promptMessage(IResource resource) {
				IProjectDescription desc = findDescription(descriptions, resource);
				if (desc != null) {
					return Policy.bind("AddToWorkspaceAction.thisExternalFileExists", desc.getLocation().toString());//$NON-NLS-1$
				} else if(resource.exists()) {
					return Policy.bind("AddToWorkspaceAction.thisResourceExists", resource.getName());//$NON-NLS-1$
				} else {
					File localLocation  = getFileLocation(resource);
					return Policy.bind("AddToWorkspaceAction.thisExternalFileExists", localLocation.toString());//$NON-NLS-1$
				}
			}
			private File getFileLocation(IResource resource) {
				return new File(resource.getParent().getLocation().toFile(), resource.getName());
			}
		};
	}
	
	private IProjectDescription findDescription(IProjectDescription[] descriptions, IResource resource) {
		IProject project = resource.getProject();
		for (int i = 0; i < descriptions.length; i++) {
			IProjectDescription description = descriptions[i];
			if (description.getName().equals(project.getName()))
				return description;
		}
		return null;
	}
}
