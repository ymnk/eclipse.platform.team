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
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.ui.*;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.SubscriberParticipant;

/**
 * This is the class registered with the org.eclipse.team.ui.synchronizeWizard
 */
public abstract class SubscriberParticipantWizard extends Wizard {

	private GlobalRefreshResourceSelectionPage selectionPage;

	public SubscriberParticipantWizard() {
		setDefaultPageImageDescriptor(TeamImages.getImageDescriptor(ISharedImages.IMG_WIZBAN_SHARE));
		setNeedsProgressMonitor(false);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		selectionPage = new GlobalRefreshResourceSelectionPage(getRootResources());
		selectionPage.setTitle("Create");
		selectionPage.setDescription("Create a " +  getName() + " Synchronize Participant");
		selectionPage.setMessage("Select the resources that will be synchronized by the newly created synchronize participant.");
		addPage(selectionPage);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#performFinish()
	 */
	public boolean performFinish() {
		IResource[] resources = selectionPage.getRootResources();
		if(resources != null && resources.length > 0) {
			SubscriberParticipant participant = createParticipant(resources);
			TeamUI.getSynchronizeManager().addSynchronizeParticipants(new ISynchronizeParticipant[] {participant});
			// We don't know in which site to show progress because a participant could actually be shown in multiple sites.
			participant.run(null /* no site */);
		}
		return true;
	}

	protected abstract IResource[] getRootResources();
	
	protected abstract SubscriberParticipant createParticipant(IResource[] resources);

	protected abstract String getName();
}
