package org.eclipse.team.internal.ccvs.ui.actions;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.CVSTeamProvider;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.ui.CVSCompareEditorInput;
import org.eclipse.team.internal.ccvs.ui.CVSLocalCompareEditorInput;
import org.eclipse.team.internal.ccvs.ui.CVSResourceNode;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.ResourceEditionNode;
import org.eclipse.team.ui.actions.TeamAction;

public class CompareWithRemoteAction extends CompareWithTagAction {

	public void run(IAction action) {
		IResource[] resources;
		resources = getSelectedResources();
		CVSTag[] tags = new CVSTag[resources.length];
		try {
			for (int i = 0; i < resources.length; i++) {
				tags[i] = getTag(resources[i]);
			}
			CompareUI.openCompareEditor(new CVSLocalCompareEditorInput(resources, tags));
		} catch(CVSException e) {
			ErrorDialog.openError(getShell(), Policy.bind("CompareWithRemoteAction.compare"), 
								  Policy.bind("CompareWithRemoteAction.noRemoteLong"), e.getStatus());
		}			
	}
	
	protected CVSTag getTag(IResource resource) throws CVSException {
		ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
		CVSTag tag = null;
		if (cvsResource.isFolder()) {
			FolderSyncInfo folderInfo = ((ICVSFolder)cvsResource).getFolderSyncInfo();
			if (folderInfo!=null) {
				tag = folderInfo.getTag();
			}
		} else {
			ResourceSyncInfo info = cvsResource.getSyncInfo();
			if (info!=null) {					
				tag = info.getTag();
			}
		}
		if (tag==null) {
			if (cvsResource.getParent().isCVSFolder()) {
				tag = cvsResource.getParent().getFolderSyncInfo().getTag();
			}
		}
		return tag;
	}
	
	protected boolean isEnabled() {
		IResource[] resources = getSelectedResources();
		if(resources.length>0) {
			for (int i = 0; i < resources.length; i++) {
				IResource resource = resources[i];
				if(RepositoryProvider.getProvider(resource.getProject(), CVSProviderPlugin.getTypeId()) == null) {
					return false;
				}
				try {
					if(getTag(resource) == null) {
						return false;
					}
				} finally {
					return false;
				}
			}
			return true;
		}
		return false;
	}
}