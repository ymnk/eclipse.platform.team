/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.operations;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.CVSTeamProvider;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.ICVSResourceVisitor;
import org.eclipse.team.internal.ccvs.core.ICVSRunnable;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.connection.CVSServerException;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.actions.CVSAction;
import org.eclipse.team.internal.ccvs.ui.repo.RepositoryManager;
import org.eclipse.team.internal.ccvs.ui.tags.BranchPromptDialog;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Perform a CVS branch operaiton
 */
public class BranchOperation extends RepositoryProviderOperation {
	
	private boolean update;
	private CVSTag rootVersionTag;
	private CVSTag branchTag;
	
	public BranchOperation(IWorkbenchPart part, IResource[] resources) {
		super(part, resources);
	}
	
	public void setTags(CVSTag rootVersionTag, CVSTag branchTag, boolean updateToBranch) {
		this.rootVersionTag = rootVersionTag;
		this.branchTag = branchTag;
		this.update = updateToBranch;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.TeamOperation#shouldRun()
	 */
	protected boolean shouldRun() {
		IResource[] resources = getResources();
		boolean allSticky = areAllResourcesSticky(resources);
		ICVSFolder folder = CVSWorkspaceRoot.getCVSFolderFor(resources[0].getProject());
		final BranchPromptDialog dialog = new BranchPromptDialog(getShell(),
											Policy.bind("BranchWizard.title"), //$NON-NLS-1$
											getCVSResources(), 
											allSticky, 
											calculateInitialVersionName(resources,allSticky));
		if (dialog.open() != InputDialog.OK) return false;		
		
		// Capture the dialog info in local variables
		final String tagString = dialog.getBranchTagName();
		update = dialog.getUpdate();
		final String versionString = dialog.getVersionTagName();
		rootVersionTag = (versionString == null) ? null : new CVSTag(versionString, CVSTag.VERSION);
		branchTag = new CVSTag(tagString, CVSTag.BRANCH);
								
		// For non-projects determine if the tag being loaded is the same as the resource's parent
		// If it's not, warn the user that they will be mixing tags
		if (update) {
			try {
				if(!CVSAction.checkForMixingTags(getShell(), resources, branchTag)) {
					return false;
				}
			} catch (CVSException e) {
				CVSUIPlugin.log(e);
			}
		}
		return super.shouldRun();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.RepositoryProviderOperation#execute(org.eclipse.team.internal.ccvs.core.CVSTeamProvider, org.eclipse.core.resources.IResource[], org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void execute(CVSTeamProvider provider, IResource[] providerResources, IProgressMonitor monitor) throws CVSException, InterruptedException {
		try {
			monitor.beginTask(null, 100);
			makeBranch(provider, providerResources, rootVersionTag, branchTag, update, Policy.subMonitorFor(monitor, 90));										
			updateRememberedTags(providerResources);
			if (update) {
				updateWorkspaceSubscriber(provider, getCVSArguments(providerResources), Policy.subMonitorFor(monitor, 10));
			}
			collectStatus(Status.OK_STATUS);
		} catch (TeamException e) {
			// Accumulate the status which will be displayed by CVSAction#endOperation(IAction)
			collectStatus(e.getStatus());
		} finally {
			monitor.done();
		}
	}

	private void makeBranch(CVSTeamProvider provider, IResource[] resources, final CVSTag versionTag, final CVSTag branchTag, boolean moveToBranch, IProgressMonitor monitor) throws TeamException {
		
		// Determine the total amount of work
		int totalWork = (versionTag!= null ? 60 : 40) + (moveToBranch ? 20 : 0);
		monitor.beginTask(Policy.bind("CVSTeamProvider.makeBranch"), totalWork);  //$NON-NLS-1$
		try {
			// Build the arguments list
			final ICVSResource[] arguments = getCVSArguments(resources);
			
			// Tag the remote resources
			IStatus status = null;
			if (versionTag != null) {
				// Version using a custom tag command that skips added but not commited reesources
				Session session = new Session(getRemoteLocation(provider), getLocalRoot(provider), true /* output to console */);
				session.open(Policy.subMonitorFor(monitor, 5), true /* open for modification */);
				try {
					status = Command.CUSTOM_TAG.execute(
						session,
						Command.NO_GLOBAL_OPTIONS,
						Command.NO_LOCAL_OPTIONS,
						versionTag,
						arguments,
						null,
						Policy.subMonitorFor(monitor, 35));
				} finally {
					session.close();
				}
				if (status.isOK()) {
					// Branch using the tag
					session = new Session(getRemoteLocation(provider), getLocalRoot(provider), true /* output to console */);
					session.open(Policy.subMonitorFor(monitor, 5), true /* open for modification */);
					try {
						status = Command.CUSTOM_TAG.execute(
							session,
							Command.NO_GLOBAL_OPTIONS,
							Command.NO_LOCAL_OPTIONS,
							branchTag,
							arguments,
							null,
						Policy.subMonitorFor(monitor, 15));
					} finally {
						session.close();
					}
				}
			} else {
				// Just branch using tag
				Session session = new Session(getRemoteLocation(provider), getLocalRoot(provider), true /* output to console */);
				session.open(Policy.subMonitorFor(monitor, 5), true /* open for modification */);
				try {
					status = Command.CUSTOM_TAG.execute(
						session,
						Command.NO_GLOBAL_OPTIONS,
						Command.NO_LOCAL_OPTIONS,
						branchTag,
						arguments,
						null,
						Policy.subMonitorFor(monitor, 35));
				} finally {
					session.close();
				}

			}
			if ( ! status.isOK()) {
				throw new CVSServerException(status);
			}
			
			// Set the tag of the local resources to the branch tag (The update command will not
			// properly update "cvs added" and "cvs removed" resources so a custom visitor is used
			if (moveToBranch) {
				setTag(provider, resources, branchTag, Policy.subMonitorFor(monitor, 20));
			}
		} finally {
			monitor.done();
		}
	}
	
	/*
	 * This method sets the tag for a project.
	 * It expects to be passed an InfiniteSubProgressMonitor
	 */
	private void setTag(final CVSTeamProvider provider, final IResource[] resources, final CVSTag tag, IProgressMonitor monitor) throws TeamException {
	
		getLocalRoot(provider).run(new ICVSRunnable() {
			public void run(IProgressMonitor progress) throws CVSException {
				try {
					// 512 ticks gives us a maximum of 2048 which seems reasonable for folders and files in a project
					progress.beginTask(null, 100);
					final IProgressMonitor monitor = Policy.infiniteSubMonitorFor(progress, 100);
					monitor.beginTask(Policy.bind("CVSTeamProvider.folderInfo", provider.getProject().getName()), 512); //$NON-NLS-1$
					
					// Visit all the children folders in order to set the root in the folder sync info
					for (int i = 0; i < resources.length; i++) {
						CVSWorkspaceRoot.getCVSResourceFor(resources[i]).accept(new ICVSResourceVisitor() {
							public void visitFile(ICVSFile file) throws CVSException {
								monitor.worked(1);
								//ResourceSyncInfo info = file.getSyncInfo();
								byte[] syncBytes = file.getSyncBytes();
								if (syncBytes != null) {
									monitor.subTask(Policy.bind("CVSTeamProvider.updatingFile", file.getName())); //$NON-NLS-1$
									file.setSyncBytes(ResourceSyncInfo.setTag(syncBytes, tag), ICVSFile.UNKNOWN);
								}
							}
							public void visitFolder(ICVSFolder folder) throws CVSException {
								monitor.worked(1);
								FolderSyncInfo info = folder.getFolderSyncInfo();
								if (info != null) {
									monitor.subTask(Policy.bind("CVSTeamProvider.updatingFolder", info.getRepository())); //$NON-NLS-1$
									folder.setFolderSyncInfo(new FolderSyncInfo(info.getRepository(), info.getRoot(), tag, info.getIsStatic()));
									folder.acceptChildren(this);
								}
							}
						});
					}
				} finally {
					progress.done();
				}
			}
		}, monitor);
	}
	
	private void updateRememberedTags(IResource[] providerResources) throws CVSException {
		if (rootVersionTag != null || update) {
			for (int i = 0; i < providerResources.length; i++) {
				ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(providerResources[i]);
				RepositoryManager manager = CVSUIPlugin.getPlugin().getRepositoryManager();

				if (rootVersionTag != null) {
					manager.addTags(cvsResource, new CVSTag[] { rootVersionTag });
				}
				if (update) {
					manager.addTags(cvsResource, new CVSTag[] { branchTag });
				}
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.CVSOperation#getTaskName()
	 */
	protected String getTaskName() {
		return Policy.bind("BranchOperation.0"); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.RepositoryProviderOperation#getTaskName(org.eclipse.team.internal.ccvs.core.CVSTeamProvider)
	 */
	protected String getTaskName(CVSTeamProvider provider) {
		return Policy.bind("BranchOperation.1", provider.getProject().getName()); //$NON-NLS-1$
	}
	
	/**
	 * Answers <code>true</code> if all resources in the array have a sticky tag
	 */
	private boolean areAllResourcesSticky(IResource[] resources) {
		for (int i = 0; i < resources.length; i++) {
			if(!hasStickyTag(resources[i])) return false;
		}
		return true;
	}
	
	/**
	 * Answers <code>true</code> if the resource has a sticky tag
	 */
	private boolean hasStickyTag(IResource resource) {
		try {
			ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);			
			CVSTag tag;
			if(cvsResource.isFolder()) {
				FolderSyncInfo folderInfo = ((ICVSFolder)cvsResource).getFolderSyncInfo();
				tag = folderInfo.getTag();
			} else {
				ResourceSyncInfo info = cvsResource.getSyncInfo();
				tag = info.getTag();
			}
			if(tag!=null) {
				int tagType = tag.getType();
				if(tagType==CVSTag.VERSION) {
					return true;
				}
			}
		} catch(CVSException e) {
			CVSUIPlugin.log(e);
			return false;
		}
		return false;
	}
	
	private String calculateInitialVersionName(IResource[] resources, boolean allSticky) {
		String versionName = "";		 //$NON-NLS-1$
		try {
			if(allSticky) {
				IResource stickyResource = resources[0];									
				if(stickyResource.getType()==IResource.FILE) {
					ICVSFile cvsFile = CVSWorkspaceRoot.getCVSFileFor((IFile)stickyResource);
					versionName = cvsFile.getSyncInfo().getTag().getName();
				} else {
					ICVSFolder cvsFolder = CVSWorkspaceRoot.getCVSFolderFor((IContainer)stickyResource);
					versionName = cvsFolder.getFolderSyncInfo().getTag().getName();
				}
			}
		} catch(CVSException e) {
			CVSUIPlugin.log(e);
			versionName = ""; //$NON-NLS-1$
		}
		return versionName;
	}
}
