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
package org.eclipse.team.internal.ccvs.ui.wizards;

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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ui.IPromptCondition;
import org.eclipse.team.internal.ui.PromptingDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.NewProjectAction;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

/**
 * @author Administrator
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class CheckoutAsWizard extends Wizard {
	
	private ICVSRemoteFolder[] remoteFolders;
	private boolean allowProjectConfiguration;

	private CheckoutAsMainPage mainPage;
	private CheckoutAsProjectSelectionPage projectSelectionPage;
	private CheckoutAsLocationSelectionPage locationSelectionPage;

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
	
	public CheckoutAsWizard(ICVSRemoteFolder[] remoteFolders, boolean allowProjectConfiguration) {
		super();
		this.remoteFolders = remoteFolders;
		setWindowTitle(Policy.bind("CheckoutAsWizard.title")); //$NON-NLS-1$
		this.allowProjectConfiguration = allowProjectConfiguration;
	}
	
	/**
	 * @see org.eclipse.jface.wizard.IWizard#addPages()
	 */
	public void addPages() {
		setNeedsProgressMonitor(true);
		ImageDescriptor substImage = CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_WIZBAN_CHECKOUT);
		
		mainPage = new CheckoutAsMainPage(substImage, remoteFolders, allowProjectConfiguration);
		addPage(mainPage);
		
		projectSelectionPage = new CheckoutAsProjectSelectionPage(substImage, remoteFolders);
		addPage(projectSelectionPage);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#performFinish()
	 */
	public boolean performFinish() {
		if (mainPage.isPerformConfigure()) {
			return performConfigureAndCheckout();
		} else if (mainPage.isPerformCheckoutAs()) {
			if (isSingleFolder()) {
				return performSingleCheckoutAs();
			} else {
				return performMultipleCheckoutAs();
			}
		} else if (mainPage.isPerformCheckoutInto()) {
			return performCheckoutInto();
		}
		return false;
	}

	/**
	 * @return
	 */
	private boolean isSingleFolder() {
		return remoteFolders.length == 1;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#canFinish()
	 */
	public boolean canFinish() {
		return (mainPage.isPageComplete() 
		&& (mainPage.isPerformConfigure()
			|| (mainPage.isPerformCheckoutInto() && projectSelectionPage.isPageComplete()) 
			|| (mainPage.isPerformCheckoutAs() && locationSelectionPage.isPageComplete())));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#getNextPage(org.eclipse.jface.wizard.IWizardPage)
	 */
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == mainPage) {
			if (mainPage.isPerformConfigure()) return null;
			if (mainPage.isPerformCheckoutInto()) return projectSelectionPage;
			if (mainPage.isPerformCheckoutAs()) {
				if (isSingleFolder()) {
					locationSelectionPage.setProjectName(mainPage.getProjectName());
				} else {
					locationSelectionPage.setProject(null);
				}
				return locationSelectionPage; 
			} 
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#getPreviousPage(org.eclipse.jface.wizard.IWizardPage)
	 */
	public IWizardPage getPreviousPage(IWizardPage page) {
		if (page == mainPage) return null;
		return mainPage;
	}

	private boolean run(WorkspaceModifyOperation operation) {
		try {
			getContainer().run(true, true, operation);
			return true;
		} catch (InvocationTargetException e) {
			handle(e);
			return false;
		} catch (InterruptedException e) {
			return false;
		}
	}
	
	private void handle(Throwable e) {
		CVSUIPlugin.openError(getShell(), Policy.bind("CheckoutAsWizard.error"), null, e); //$NON-NLS-1$
	}
	
	/*
	 * Configure a local project and checkout the selected remote folder into the project.
	 * This only occurs for single folders.
	 */
	private boolean performConfigureAndCheckout() {
		final IProject newProject = getNewProject();
		if (newProject == null) return false;
		return run(new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					checkoutProject(newProject, null, monitor);
				} catch (TeamException e) {
					throw new InvocationTargetException(e);
				}
			}
		});
	}
	
	/**
	 * 
	 */
	private boolean performSingleCheckoutAs() {
		final IProject newProject = ResourcesPlugin.getWorkspace().getRoot().getProject(mainPage.getProjectName());
		final IProjectDescription desc = locationSelectionPage.getProjectDescription(newProject);
		if (!promptToOverwrite(new IProject[] {newProject}, new IProjectDescription[] {desc})) return false;
		return run(new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					checkoutProject(newProject, desc, monitor);
				} catch (TeamException e) {
					throw new InvocationTargetException(e);
				}
			}
		});
	}

	/**
	 * Check out multiple folders to the workspace using a custom location if one is
	 * specified.
	 */
	private boolean performMultipleCheckoutAs() {
		final IProject[] newProjects = getTargetProjects();
		final IProjectDescription[] descs = locationSelectionPage.getProjectDescriptions();
		if (!promptToOverwrite(newProjects, descs)) return false;
		return run(new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					checkoutProjects(newProjects, descs, monitor);
				} catch (TeamException e) {
					throw new InvocationTargetException(e);
				}
			}
		});
	}
	
	/**
	 * Answer the list of target projects for the remote folders
	 */
	private IProject[] getTargetProjects() {
		// create the target project handles
		IProject[] targetProjects = new IProject[remoteFolders.length];
		for (int i = 0; i < remoteFolders.length; i++) {
			ICVSRemoteFolder remoteFolder = remoteFolders[i];
			targetProjects[i] = ResourcesPlugin.getWorkspace().getRoot().getProject(remoteFolder.getName());
		}
		return targetProjects;
	}

	/**
	 * @return
	 */
	private boolean performCheckoutInto() {
		// TODO Auto-generated method stub
		return false;
	}
	
	/**
	 * @param projects
	 * @param descs
	 * @return
	 */
	private boolean promptToOverwrite(IProject[] projects, IProjectDescription[] descs) {
		PromptingDialog prompt = new PromptingDialog(getShell(), projects,
			getOverwriteLocalAndFileSystemPrompt(descs), 
			Policy.bind("ReplaceWithAction.confirmOverwrite"),
			true /* all or nothing */);//$NON-NLS-1$
		try {
			return (prompt.promptForMultiple().length == projects.length);
		} catch (InterruptedException e) {
			return false;
		}
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
	/**
	 * @param newProject
	 * @param monitor
	 */
	protected void checkoutProject(IProject newProject, IProjectDescription desc, IProgressMonitor monitor) throws TeamException {
		monitor.beginTask(null, 100);
		createAndOpenProject(newProject, desc, Policy.subMonitorFor(monitor, 10));
		checkoutProjects(remoteFolders, new IProject[] {newProject}, Policy.subMonitorFor(monitor, 90));
		monitor.done();
	}

	protected void checkoutProjects(IProject[] newProjects, IProjectDescription[] descs, IProgressMonitor monitor) throws TeamException {
		monitor.beginTask(null, 100);
		createAndOpenProjects(newProjects, descs, Policy.subMonitorFor(monitor, 10));
		checkoutProjects(remoteFolders, newProjects, Policy.subMonitorFor(monitor, 90));
		monitor.done();
				
	}
	/**
	 * Get a new project that is configured by the new project wizard.
	 * This is currently the only way to do this.
	 */
	private IProject getNewProject() {
		NewProjectListener listener = new NewProjectListener();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);
		(new NewProjectAction(PlatformUI.getWorkbench().getActiveWorkbenchWindow())).run();
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(listener);
		IProject project = listener.getNewProject();
		return project;
	}
	
	private void createAndOpenProject(IProject project, IProjectDescription desc, IProgressMonitor monitor) throws CVSException {
		try {
			monitor.beginTask(null, 5);
			if (project.exists()) {
				if (desc != null) {
					project.move(desc, true, Policy.subMonitorFor(monitor, 3));
				}
			} else {
				if (desc == null) {
					// create in default location
					project.create(Policy.subMonitorFor(monitor, 3));
				} else {
					// create in some other location
					project.create(desc, Policy.subMonitorFor(monitor, 3));
				}
			}
			if (!project.isOpen()) {
				project.open(Policy.subMonitorFor(monitor, 2));
			}
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		} finally {
			monitor.done();
		}
	}
	
	private void createAndOpenProjects(IProject[] projects, IProjectDescription[] descriptions, IProgressMonitor monitor) throws CVSException {
		monitor.beginTask(null, projects.length* 100);
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			IProjectDescription desc = findDescription(descriptions, project);
			createAndOpenProject(project, desc, Policy.subMonitorFor(monitor, 100));
		}
		monitor.done();
	}
	
	private IProjectDescription findDescription(IProjectDescription[] descriptions, IResource resource) {
		IProject project = resource.getProject();
		for (int i = 0; i < descriptions.length; i++) {
			IProjectDescription description = descriptions[i];
			if (description != null && description.getName().equals(project.getName()))
				return description;
		}
		return null;
	}
	
	private void checkoutProjects(ICVSRemoteFolder[] folders, IProject[] projects, IProgressMonitor monitor) throws TeamException {
		CVSWorkspaceRoot.checkout(folders, projects, monitor);
	}

}
