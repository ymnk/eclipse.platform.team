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

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipantDescriptor;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate;
import org.eclipse.ui.texteditor.IUpdate;

public class GlobalRefreshAction extends Action implements IMenuCreator, IWorkbenchWindowPulldownDelegate, IUpdate {

	private Menu fMenu;
	private IWorkbenchWindow window;

	static class SynchronizeWizardDialog extends WizardDialog {
		SynchronizeWizardDialog(Shell parent, IWizard wizard) {
			super(parent, wizard);
			setShellStyle(getShellStyle());
			setMinimumPageSize(500, 300);
		}
	}
	
	class RefreshParticipantAction extends Action {
		private ISynchronizeParticipantDescriptor participantDescriptor;

		public void run() {
			GlobalSynchronizeWizard wizard = new GlobalSynchronizeWizard(participantDescriptor);
			SynchronizeWizardDialog dialog = new SynchronizeWizardDialog(window.getShell(), wizard);
			dialog.open();
		}

		public RefreshParticipantAction(int prefix, ISynchronizeParticipantDescriptor participantDescriptor) {
			super("&" + prefix + " " + participantDescriptor.getName()); //$NON-NLS-1$ //$NON-NLS-2$
			this.participantDescriptor = participantDescriptor;
			setImageDescriptor(participantDescriptor.getImageDescriptor());
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.IUpdate#update()
	 */
	public void update() {
		ISynchronizeParticipant[] pages = TeamUI.getSynchronizeManager().getSynchronizeParticipants();
		setEnabled(pages.length >= 1);
	}

	public GlobalRefreshAction() {
		Utils.initAction(this, "action.refreshSubscriber."); //$NON-NLS-1$
		setMenuCreator(this);
		update();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.action.IMenuCreator#dispose()
	 */
	public void dispose() {
		if (fMenu != null) {
			fMenu.dispose();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.action.IMenuCreator#getMenu(org.eclipse.swt.widgets.Menu)
	 */
	public Menu getMenu(Menu parent) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.action.IMenuCreator#getMenu(org.eclipse.swt.widgets.Control)
	 */
	public Menu getMenu(Control parent) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = new Menu(parent);
		ISynchronizeParticipantDescriptor[] descriptions = TeamUI.getSynchronizeManager().getParticipantDescriptors();
		for (int i = 0; i < descriptions.length; i++) {
			ISynchronizeParticipantDescriptor description = descriptions[i];
			if (description.doesSupportRefresh()) {
				Action action = new RefreshParticipantAction(i + 1, description);
				addActionToMenu(fMenu, action);
			}
		}
		return fMenu;
	}

	protected void addActionToMenu(Menu parent, Action action) {
		ActionContributionItem item = new ActionContributionItem(action);
		item.fill(parent, -1);
	}

	protected void addMenuSeparator() {
		new MenuItem(fMenu, SWT.SEPARATOR);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
	 */
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
	 *           org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}
}