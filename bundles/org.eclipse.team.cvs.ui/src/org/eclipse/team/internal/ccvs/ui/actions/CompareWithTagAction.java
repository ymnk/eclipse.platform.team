package org.eclipse.team.internal.ccvs.ui.actions;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
 
import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.ui.CVSLocalCompareEditorInput;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.TagSelectionDialog;
import org.eclipse.team.ui.actions.TeamAction;

public class CompareWithTagAction extends TeamAction {

	public void run(IAction action) {
		final CVSTag tag;
		final ICVSRemoteResource[] remoteResource = new ICVSRemoteResource[] { null };
		final IResource[] resources = getSelectedResources();
		
		IProject[] projects = new IProject[resources.length];
		for (int i = 0; i < resources.length; i++) {
			projects[i] = resources[i].getProject();
		}
		TagSelectionDialog dialog = new TagSelectionDialog(getShell(), projects, Policy.bind("CompareWithTagAction.message")); //$NON-NLS-1$
		dialog.setBlockOnOpen(true);
		int result = dialog.open();
		if (result == Dialog.CANCEL || dialog.getResult() == null) {
			return;
		}
		tag = dialog.getResult();
		if (tag == null) return;
		CompareUI.openCompareEditor(new CVSLocalCompareEditorInput(resources, tag));
	}
	
	protected boolean isEnabled() {
		IResource[] resources = getSelectedResources();
		// allow operation for homegeneous multiple selections
		if(resources.length>0) {
			for (int i = 0; i < resources.length; i++) {
				IResource resource = resources[i];
				if(RepositoryProvider.getProvider(resource.getProject(), CVSProviderPlugin.getTypeId()) == null) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
}
