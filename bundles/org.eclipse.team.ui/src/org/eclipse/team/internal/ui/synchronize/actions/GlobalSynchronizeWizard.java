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
package org.eclipse.team.internal.ui.synchronize.actions;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipantDescriptor;


public class GlobalSynchronizeWizard extends Wizard {

	private ISynchronizeParticipantDescriptor participantDescriptor;
	private GlobalRefreshResourceSelectionPage selectionPage;
	
	public GlobalSynchronizeWizard(ISynchronizeParticipantDescriptor participantDescriptor) {
		this.participantDescriptor = participantDescriptor;
		setWindowTitle("Team Synchronize");
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		if(participantDescriptor == null) {
			// addPage();
		}
		selectionPage = new GlobalRefreshResourceSelectionPage();
		addPage(selectionPage);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#performFinish()
	 */
	public boolean performFinish() {
		// TODO Auto-generated method stub
		return false;
	}
}
