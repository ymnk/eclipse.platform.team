package org.eclipse.team.internal.ccvs.ui.actions;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.ui.wizards.BranchWizard;
import org.eclipse.team.ui.actions.TeamAction;

/**
 * BranchAction tags the selected resources with a branch tag specified by the user,
 * and optionally updates the local resources to point to the new branch.
 */
public class BranchAction extends TeamAction {
	/*
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		final Shell shell = getShell();
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				BranchWizard wizard = new BranchWizard();
				wizard.setResources(getSelectedResources());
				WizardDialog dialog = new WizardDialog(shell, wizard);
				dialog.open();
			}
		});
	}
	
	/*
	 * @see TeamAction#isEnabled()
	 */
	protected boolean isEnabled() throws TeamException {
		IResource[] resources = getSelectedResources();
		// allow operation for homegeneous multiple selections
		if(resources.length>0) {
			int type = -1;
			for (int i = 0; i < resources.length; i++) {
				IResource resource = resources[i];
				if(type!=-1) {
					if(type!=resource.getType()) return false;
				}
				if(RepositoryProvider.getProvider(resource.getProject(), CVSProviderPlugin.getTypeId()) == null) {
					return false;
				}
				type = resource.getType();
				ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
				if(cvsResource.isFolder()) {
					if( ! ((ICVSFolder)cvsResource).isCVSFolder()) return false;
				} else {
					if( ! cvsResource.isManaged()) return false;
				}
			}
			return true;
		}
		return false;
	}
}

