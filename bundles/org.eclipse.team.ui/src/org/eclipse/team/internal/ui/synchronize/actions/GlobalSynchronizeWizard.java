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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.wizard.*;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeManager;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.ui.IWorkbench;

/**
 * The wizard for synchronizing a synchronize participant.
 * 
 * @since 3.0
 */
public class GlobalSynchronizeWizard extends Wizard {

	protected IWorkbench workbench;
	protected IWizard wizard;
	protected GlobalRefreshParticipantSelectionPage mainPage;
	private String pluginId = TeamUIPlugin.PLUGIN_ID;

	public GlobalSynchronizeWizard() {
		setNeedsProgressMonitor(true);
		setWindowTitle("Synchronize"); //$NON-NLS-1$
	}

	/*
	 * @see Wizard#addPages
	 */
	public void addPages() {
		ISynchronizeParticipant[] participants = getParticipants();
		if (participants.length == 1) {
			// If there is only one wizard, skip the first page.
			// Only skip the first page if the one wizard has at least one
			// page.
			IWizard wizard = participants[0].createRefreshPage();
			wizard.addPages();
			if (wizard.getPageCount() > 0) {
				wizard.setContainer(getContainer());
				IWizardPage[] pages = wizard.getPages();
				for (int i = 0; i < pages.length; i++) {
					addPage(pages[i]);
				}
				return;
			}
		}
		mainPage = new GlobalRefreshParticipantSelectionPage();
		addPage(mainPage);
	}

	public IWizardPage getNextPage(IWizardPage page) {
		if (wizard != null) {
			return wizard.getNextPage(page);
		}
		return super.getNextPage(page);
	}

	public boolean canFinish() {
		// If we are on the first page, never allow finish unless the selected
		// wizard has no pages.
		if (getContainer().getCurrentPage() == mainPage) {
			if (mainPage.getSelectedWizard() != null && mainPage.getNextPage() == null) {
				return true;
			}
			return false;
		}
		if (wizard != null) {
			return wizard.canFinish();
		}
		return super.canFinish();
	}

	/*
	 * @see Wizard#performFinish
	 */
	public boolean performFinish() {
		// There is only one wizard with at least one page
		if (wizard != null) {
			return wizard.performFinish();
		}
		// If we are on the first page and the selected wizard has no pages
		// then
		// allow it to finish.
		if (getContainer().getCurrentPage() == mainPage) {
			IWizard noPageWizard = mainPage.getSelectedWizard();
			if (noPageWizard != null) {
				if (noPageWizard.canFinish()) {
					return noPageWizard.performFinish();
				}
			}
		}
		// If the wizard has pages and there are several
		// wizards registered then the registered wizard
		// will call it's own performFinish().
		return true;
	}

	protected ISynchronizeParticipant[] getParticipants() {
		List participants = new ArrayList();
		ISynchronizeManager manager = (ISynchronizeManager) TeamUI.getSynchronizeManager();
		ISynchronizeParticipant[] desciptors = manager.getSynchronizeParticipants();
		for (int i = 0; i < desciptors.length; i++) {
			ISynchronizeParticipant descriptor = desciptors[i];
			if (descriptor.doesSupportRefresh()) {
				participants.add(descriptor);
			}
		}
		return (ISynchronizeParticipant[]) participants.toArray(new ISynchronizeParticipant[participants.size()]);
	}
}
