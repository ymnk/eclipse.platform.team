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

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSStatus;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.CVSTeamProvider;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.ICVSRunnable;
import org.eclipse.team.internal.ccvs.core.client.Checkout;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Request;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.client.Update;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.connection.CVSServerException;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ui.IPromptCondition;

/**
 * This class acts as an abstract class for checkout operations.
 * It provides a few common methods.
 */
public abstract class CheckoutOperation extends CVSOperation {

	protected void createAndOpenProjects(IProject[] projects, IProjectDescription[] descriptions, IProgressMonitor monitor) throws CVSException {
		monitor.beginTask(null, projects.length* 100);
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			IProjectDescription desc = findDescription(descriptions, project);
			createAndOpenProject(project, desc, Policy.subMonitorFor(monitor, 100));
		}
		monitor.done();
	}

	/**
	 * This should be done in the checkout (scrubProjects?). 
	 * However, there are some cases where it is not needed 
	 * (ie. no custom location or pre-configured).
	 * @deprecated
	 * @param project
	 * @param desc
	 * @param monitor
	 * @throws CVSException
	 */
	protected void createAndOpenProject(IProject project, IProjectDescription desc, IProgressMonitor monitor) throws CVSException {
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
	
	private IProjectDescription findDescription(IProjectDescription[] descriptions, IResource resource) {
		IProject project = resource.getProject();
		for (int i = 0; i < descriptions.length; i++) {
			IProjectDescription description = descriptions[i];
			if (description.getName().equals(project.getName()))
				return description;
		}
		return null;
	}
	
	protected void checkoutProjects(ICVSRemoteFolder[] folders, IProject[] projects, IProgressMonitor monitor) throws CVSException {
		try {
			CVSWorkspaceRoot.checkout(folders, projects, monitor);
		} catch (TeamException e) {
			throw CVSException.wrapException(e);
		}
	}
	
	protected IPromptCondition getOverwriteLocalAndFileSystemPrompt() {
		return new IPromptCondition() {
			// prompt if resource in workspace exists or exists in local file system
			public boolean needsPrompt(IResource resource) {
				File localLocation  = getFileLocation(resource);
				if(resource.exists() || localLocation.exists()) {
					return true;
				}
				return false;
			}
			public String promptMessage(IResource resource) {
				File localLocation  = getFileLocation(resource);
				if(resource.exists()) {
					return Policy.bind("AddToWorkspaceAction.thisResourceExists", resource.getName());//$NON-NLS-1$
				} else {
					return Policy.bind("AddToWorkspaceAction.thisExternalFileExists", resource.getName());//$NON-NLS-1$
				}
			}
			private File getFileLocation(IResource resource) {
				return new File(resource.getParent().getLocation().toFile(), resource.getName());
			}
		};
	}
	
	protected static String getTaskName(ICVSRemoteFolder[] remoteFolders) {
		if (remoteFolders.length == 1) {
			ICVSRemoteFolder folder = remoteFolders[0];
			String label = folder.getRepositoryRelativePath();
			if (label.equals(FolderSyncInfo.VIRTUAL_DIRECTORY)) {
				label = folder.getName();
			}
			return Policy.bind("AddToWorkspace.taskName1", label);  //$NON-NLS-1$
		}
		else {
			return Policy.bind("AddToWorkspace.taskNameN", new Integer(remoteFolders.length).toString());  //$NON-NLS-1$
		}
	}
	
	/**
	 * Checkout the remote resources into the local workspace. Each resource will 
	 * be checked out into the corresponding project. If the corresponding project is
	 * null or if projects is null, the name of the remote resource is used as the name of the project.
	 * 
	 * Resources existing in the local file system at the target project location but now 
	 * known to the workbench will be overwritten.
	 */
	public void checkout(ICVSRemoteFolder[] resources, IProject[] projects, IProgressMonitor pm) throws CVSException {
		try {
			pm.beginTask(null, 1000 * resources.length);
			for (int i=0;i<resources.length;i++) {
				ICVSRemoteFolder resource = resources[i];
				
				// Determine the provided target project if there is one
				IProject project = null;
				if (projects != null) 
					project = projects[i];
				
				// Determine the remote module to be checked out
				String moduleName;
				if (resource.isDefinedModule()) {
					moduleName = resource.getName();
				} else {
					moduleName = resource.getRepositoryRelativePath();
				}
				
				checkout(resource, project, moduleName, Policy.subMonitorFor(pm, 1000));
			}
		} finally {
			pm.done();
		}
	}

	protected void checkout(ICVSRemoteFolder resource, IProject project, String moduleName, final IProgressMonitor pm) throws CVSException {
		// Get the location of the workspace root
		ICVSFolder root = CVSWorkspaceRoot.getCVSFolderFor(ResourcesPlugin.getWorkspace().getRoot());
		// Open a connection session to the repository
		ICVSRepositoryLocation repository = resource.getRepository();
		Session session = new Session(repository, root);
		try {
			pm.beginTask(null, 1000);
			session.open(Policy.subMonitorFor(pm, 50));
			
			// Determine the local target projects (either the project provider or the module expansions) 
			IProject[] targetProjects = prepareProjects(session, project,moduleName, Policy.subMonitorFor(pm, 50));
		
			// Build the local options
			List localOptions = new ArrayList();
			// Add the option to load into the target project if one was supplied
			if (project != null) {
				localOptions.add(Checkout.makeDirectoryNameOption(project.getName()));
			}
			// Prune empty directories if pruning enabled
			if (CVSProviderPlugin.getPlugin().getPruneEmptyDirectories()) 
				localOptions.add(Checkout.PRUNE_EMPTY_DIRECTORIES);
			// Add the options related to the CVSTag
			CVSTag tag = resource.getTag();
			if (tag == null) {
				// A null tag in a remote resource indicates HEAD
				tag = CVSTag.DEFAULT;
			}
			localOptions.add(Update.makeTagOption(tag));
		
			// Perform the checkout
			IStatus status = Command.CHECKOUT.execute(session,
				Command.NO_GLOBAL_OPTIONS,
				(LocalOption[])localOptions.toArray(new LocalOption[localOptions.size()]),
				new String[]{moduleName},
				null,
				Policy.subMonitorFor(pm, 800));
			if (status.getCode() == CVSStatus.SERVER_ERROR) {
				// TODO: Should we cleanup any partially checked out projects?
				// TODO: Should we return this status
				throw new CVSServerException(status);
			}
			
			// Bring the project into the workspace
			refreshProjects(targetProjects, Policy.subMonitorFor(pm, 100));
		
		} finally {
			session.close();
			pm.done();
		}
	}

	/*
	 * Prepare the workspace to receive the project(s). If project is not null, then
	 * if will be the only target project of the checkout. Otherwise, the remote folder
	 * could expand to multiple projects.
	 */
	private IProject[] prepareProjects(Session session, IProject project, String moduleName, IProgressMonitor pm) throws CVSException {
			
		pm.beginTask(null, 100);
		Set targetProjectSet = new HashSet();
		if (project == null) {
			
			// Fetch the module expansions
			IStatus status = Request.EXPAND_MODULES.execute(session, new String[] {moduleName}, Policy.subMonitorFor(pm, 50));
			if (status.getCode() == CVSStatus.SERVER_ERROR) {
				// TODO: Should we return this status
				throw new CVSServerException(status);
			}
			
			// Convert the module expansions to local projects
			String[] expansions = session.getModuleExpansions();
			for (int j = 0; j < expansions.length; j++) {
				targetProjectSet.add(ResourcesPlugin.getWorkspace().getRoot().getProject(new Path(expansions[j]).segment(0)));
			}
			
		} else {
			targetProjectSet.add(project);
		}
		
		final IProject[] targetProjects = (IProject[]) targetProjectSet.toArray(new IProject[targetProjectSet.size()]);
		// Prepare the target projects to receive resources
		// TODO: Does this really need to be wrapped or is it done higher up?
		session.getLocalRoot().run(new ICVSRunnable() {
			public void run(IProgressMonitor monitor) throws CVSException {
				scrubProjects(targetProjects, monitor);
			}
		}, Policy.subMonitorFor(pm, 50));
		pm.done();
		return targetProjects;
	}
	
	/*
	 * Delete the target projects before checking out
	 */
	private static void scrubProjects(IProject[] projects, IProgressMonitor monitor) throws CVSException {
		if (projects == null) {
			monitor.done();
			return;
		}
		monitor.beginTask(Policy.bind("CVSProvider.Scrubbing_projects_1"), projects.length * 100); //$NON-NLS-1$
		try {	
			for (int i=0;i<projects.length;i++) {
				IProject project = projects[i];
				if (project.exists()) {
					// TODO: Prompt to confirm overwrite
					if(!project.isOpen()) {
						project.open(Policy.subMonitorFor(monitor, 10));
					}
					// We do not want to delete the project to avoid a project deletion delta
					// We do not want to delete the .project to avoid core exceptions
					monitor.subTask(Policy.bind("CVSProvider.Scrubbing_local_project_1")); //$NON-NLS-1$
					// unmap the project from any previous repository provider
					if (RepositoryProvider.getProvider(project) != null)
						RepositoryProvider.unmap(project);
					IResource[] children = project.members(IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
					IProgressMonitor subMonitor = Policy.subMonitorFor(monitor, 80);
					subMonitor.beginTask(null, children.length * 100);
					try {
						for (int j = 0; j < children.length; j++) {
							if ( ! children[j].getName().equals(".project")) {//$NON-NLS-1$
								children[j].delete(true /*force*/, Policy.subMonitorFor(subMonitor, 100));
							}
						}
					} finally {
						subMonitor.done();
					}
				} else {
					// Make sure there is no directory in the local file system.
					File location = new File(project.getParent().getLocation().toFile(), project.getName());
					if (location.exists()) {
						// TODO: Prompt to confirm overwrite
						deepDelete(location);
					}
				}
			}
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		} catch (TeamException e) {
			throw CVSException.wrapException(e);
		} finally {
			monitor.done();
		}
	}
	
	/*
	 * Bring the provied projects into the workspace
	 */
	private static void refreshProjects(IProject[] projects, IProgressMonitor monitor) throws CVSException {
		monitor.beginTask(Policy.bind("CVSProvider.Creating_projects_2"), projects.length * 100); //$NON-NLS-1$
		try {
			for (int i = 0; i < projects.length; i++) {
				IProject project = projects[i];
				// Register the project with Team
				try {
					RepositoryProvider.map(project, CVSProviderPlugin.getTypeId());
				} catch (TeamException e) {
					throw CVSException.wrapException(e);
				}
				CVSTeamProvider provider = (CVSTeamProvider)RepositoryProvider.getProvider(project, CVSProviderPlugin.getTypeId());
				provider.setWatchEditEnabled(CVSProviderPlugin.getPlugin().isWatchEditEnabled());
			}
			
		} finally {
			monitor.done();
		}
	}
	
	private static void deepDelete(File resource) {
		if (resource.isDirectory()) {
			File[] fileList = resource.listFiles();
			for (int i = 0; i < fileList.length; i++) {
				deepDelete(fileList[i]);
			}
		}
		resource.delete();
	}
}
