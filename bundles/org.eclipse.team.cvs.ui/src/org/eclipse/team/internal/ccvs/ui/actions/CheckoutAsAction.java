package org.eclipse.team.internal.ccvs.ui.actions;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.sync.IRemoteResource;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.PromptingDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.ProjectLocationSelectionDialog;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.internal.IHelpContextIds;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.dialogs.NewWizard;

/**
 * Add a remote resource to the workspace. Current implementation:
 * -Works only for remote folders
 * -Does not prompt for project name; uses folder name instead
 */
public class CheckoutAsAction extends AddToWorkspaceAction {
	/*
	 * @see IActionDelegate#run(IAction)
	 */
	public void execute(IAction action) {
		
		final ICVSRemoteFolder[] folders = getSelectedRemoteFolders();
		if (folders.length != 1) return;
		final String name = folders[0].getName();
		
		// Fetch the members of the folder to see if they contain a .project file.
		final boolean[] hasProjectMetaFile = new boolean[] { false };
		run(new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) throws InterruptedException, InvocationTargetException {
				try {
					folders[0].members(monitor);
				} catch (TeamException e) {
					throw new InvocationTargetException(e);
				}
				try {
					folders[0].getFile(".project");
					hasProjectMetaFile[0] = true;
				} catch (TeamException e) {
					// We couldn't retrieve the meta file so assume it doesn't exist
					hasProjectMetaFile[0] = false;
				}
			}
		}, Policy.bind("CheckoutAsAction.checkoutFailed"), this.PROGRESS_DIALOG); //$NON-NLS-1$
		
		// Prompt outside a workspace runnable so that the project creation delta can be heard
		IProject newProject = null;
		if ( ! hasProjectMetaFile[0]) {
			newProject = getNewProject(name);
		}
		
		final IProject createdProject = newProject;
		run(new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) throws InterruptedException, InvocationTargetException {
				try {
					IProject project;
					if (hasProjectMetaFile[0]) {
						// Prompt for name
						final Shell shell = getShell();
						final int[] result = new int[] { Dialog.OK };
						project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
						final ProjectLocationSelectionDialog dialog = new ProjectLocationSelectionDialog(shell, project);
						dialog.setTitle(Policy.bind("CheckoutAsAction.enterProjectTitle", name)); //$NON-NLS-1$
	
						shell.getDisplay().syncExec(new Runnable() {
							public void run() {
								result[0] = dialog.open();
							}
						});
						if (result[0] != Dialog.OK) return;
	
						Object[] destinationPaths = dialog.getResult();
						if (destinationPaths == null) return;
						String newName = (String) destinationPaths[0];
						IPath newLocation = new Path((String) destinationPaths[1]);
	
						// prompt if the project exists locally
						project = ResourcesPlugin.getWorkspace().getRoot().getProject(newName);
						PromptingDialog prompt = new PromptingDialog(getShell(), new IResource[] { project },
							getOverwriteLocalAndFileSystemPrompt(), Policy.bind("ReplaceWithAction.confirmOverwrite"));//$NON-NLS-1$
						if (prompt.promptForMultiple().length == 0) return;
	
						monitor.beginTask(null, 100);
						monitor.setTaskName(Policy.bind("CheckoutAsAction.taskname", name, newName)); //$NON-NLS-1$
	
						// create the project
						try {
							if (newLocation.equals(Platform.getLocation())) {
								// create in default location
								project.create(Policy.subMonitorFor(monitor, 3));
							} else {
								// create in some other location
								IProjectDescription desc = ResourcesPlugin.getWorkspace().newProjectDescription(project.getName());
								desc.setLocation(newLocation);
								project.create(desc, Policy.subMonitorFor(monitor, 3));
							}
							project.open(Policy.subMonitorFor(monitor, 2));
						} catch (CoreException e) {
							throw CVSException.wrapException(e);
						}
					} else {
						project = createdProject;
						monitor.beginTask(null, 95);
						monitor.setTaskName(Policy.bind("CheckoutAsAction.taskname", name, createdProject.getName())); //$NON-NLS-1$
					}

					CVSProviderPlugin.getProvider().checkout(folders, new IProject[] { project }, Policy.subMonitorFor(monitor, 95));

				} catch (TeamException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		}, Policy.bind("CheckoutAsAction.checkoutFailed"), this.PROGRESS_DIALOG); //$NON-NLS-1$
	}
	
	/*
	 * @see TeamAction#isEnabled()
	 */
	protected boolean isEnabled() throws TeamException {
		return getSelectedRemoteFolders().length == 1;
	}

	/**
	 * Get a new project.
	 * 
	 * The suggestedName is not currently used but is a desired capability.
	 */
	private IProject getNewProject(String suggestedName) {
		NewProjectListener listener = new NewProjectListener();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);
		createProject(suggestedName);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(listener);
		return listener.getNewProject();
	}
	
	/**
	 * This code is was taken from org.eclipse.ui.actions.NewProjectAction.
	 * It opens the NewWizard for creating projects in the same manner
	 * as the new project wizard available from the Eclipse File menu.
	 * 
	 * The suggestedName is not currently used but is a desired capability.
	 */
	private void createProject(String suggestedName) {
		// Create wizard selection wizard.
		IWorkbench workbench = PlatformUI.getWorkbench();
		NewWizard wizard = new NewWizard();
		wizard.setProjectsOnly(true);
		//ISelection selection = window.getSelectionService().getSelection();
		IStructuredSelection selectionToPass = StructuredSelection.EMPTY;
		//if (selection instanceof IStructuredSelection)
		//	selectionToPass = (IStructuredSelection) selection;
		wizard.init(workbench, selectionToPass);
		IDialogSettings workbenchSettings = WorkbenchPlugin.getDefault().getDialogSettings();
		IDialogSettings wizardSettings = workbenchSettings.getSection("NewWizardAction");//$NON-NLS-1$
		if(wizardSettings==null)
			wizardSettings = workbenchSettings.addNewSection("NewWizardAction");//$NON-NLS-1$
		wizard.setDialogSettings(wizardSettings);
		wizard.setForcePreviousAndNextButtons(true);
	
		// Create wizard dialog.
		Shell parent = getShell();
		WizardDialog dialog = new WizardDialog(parent, wizard);
		dialog.create();
		dialog.getShell().setSize( Math.max(500, dialog.getShell().getSize().x), 500 );
		WorkbenchHelp.setHelp(dialog.getShell(), IHelpContextIds.NEW_PROJECT_WIZARD);
	
		// Open wizard.
		dialog.open();
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
}