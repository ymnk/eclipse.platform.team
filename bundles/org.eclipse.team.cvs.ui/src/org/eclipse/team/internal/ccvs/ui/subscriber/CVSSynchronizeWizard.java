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
package org.eclipse.team.internal.ccvs.ui.subscriber;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.synchronize.GlobalRefreshResourceSelectionPage;
import org.eclipse.team.ui.ISharedImages;
import org.eclipse.team.ui.TeamImages;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;

/**
 * This is the class registered with the org.eclipse.team.ui.synchronizeWizard
 */
public class CVSSynchronizeWizard extends Wizard {

	private GlobalRefreshResourceSelectionPage selectionPage;

	public CVSSynchronizeWizard() {
		setDefaultPageImageDescriptor(TeamImages.getImageDescriptor(ISharedImages.IMG_WIZBAN_SHARE));
		setNeedsProgressMonitor(false);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		selectionPage = new GlobalRefreshResourceSelectionPage(getRootResources());
		addPage(selectionPage);
	}
	
	private IResource[] getRootResources() {
		return CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().roots();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#performFinish()
	 */
	public boolean performFinish() {
		IResource[] resources = selectionPage.getRootResources();
		if(resources != null && resources.length > 0) {
			WorkspaceSynchronizeParticipant participant = new WorkspaceSynchronizeParticipant(resources);
			TeamUI.getSynchronizeManager().addSynchronizeParticipants(new ISynchronizeParticipant[] {participant});
			// We don't know in which site to show progress because a participant could actually be shown in multiple sites.
			participant.refresh(resources, Policy.bind("Participant.synchronizing"), Policy.bind("Participant.synchronizingDetails", participant.getName()), null); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return true;
	}

}
