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
package org.eclipse.team.ui.synchronize.viewers;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.internal.ResizableDialog;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.util.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.*;

/**
 * A compare dialog that displays a comparison. 
 * 
 * @since 3.0
 */
public class CompareDialog extends ResizableDialog implements IPropertyChangeListener {
		
	private CompareEditorInput fCompareEditorInput;
	private ISynchronizeParticipant participant;
	private Button saveButton;
	private Button rememberParticipantButton;
	private String title;
	private boolean isDirty = false;

	public CompareDialog(Shell shell, String title, CompareEditorInput input) {
		super(shell, null);
		this.title = title;
		Assert.isNotNull(input);
		fCompareEditorInput= input;
		fCompareEditorInput.addPropertyChangeListener(this);
	}
	
	public void setSynchronizeParticipant(ISynchronizeParticipant participant) {
		this.participant = participant;
	}
		
	public void propertyChange(PropertyChangeEvent event) {
		if (fCompareEditorInput != null) {
			if(fCompareEditorInput.isSaveNeeded()) {
				// the dirty flag is required because there is a compare bug that causes the dirty bit to be reset sometimes
				// although the underlying compare editor input is still dirty.
				isDirty = true;
				getShell().setText(title + " *");
			} else {
				getShell().setText(title);
			}
		}
	}
	
	/* (non-Javadoc)
	 * Method declared on Dialog.
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}
	
	/* (non-Javadoc)
	 * Method declared on Dialog.
	 */
	protected Control createDialogArea(Composite parent2) {
		Composite parent = (Composite) super.createDialogArea(parent2);
		Control c = fCompareEditorInput.createContents(parent);
		c.setLayoutData(new GridData(GridData.FILL_BOTH));
		if (participant != null) {
			rememberParticipantButton = new Button(parent, SWT.CHECK);
			rememberParticipantButton.setText("Remember this result by placing it in the Synchronize View.");
		}
		Shell shell = c.getShell();
		shell.setText(title);
		shell.setImage(fCompareEditorInput.getTitleImage());
		Dialog.applyDialogFont(parent2);
		return parent;
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	protected void buttonPressed(int buttonId) {
		if (fCompareEditorInput.isSaveNeeded() && MessageDialog.openConfirm(getShell(), "Confirm Save", "Do you want to save changes?")) {						
			BusyIndicator.showWhile(null, new Runnable() {
				public void run() {
					try {
						fCompareEditorInput.saveChanges(new NullProgressMonitor());
					} catch (CoreException e) {
						Utils.handle(e);
					}
				}
			});		
		}
		if(buttonId == IDialogConstants.OK_ID && isRememberParticipant()) {
			rememberParticipant();
		} 
		super.buttonPressed(buttonId);
	}
	
	protected boolean isRememberParticipant() {
		return getParticipant() != null && rememberParticipantButton != null && rememberParticipantButton.getSelection();
	}
	
	protected void rememberParticipant() {
		if(getParticipant() != null) {
			ISynchronizeManager mgr = TeamUI.getSynchronizeManager();
			ISynchronizeView view = mgr.showSynchronizeViewInActivePage(null);
			mgr.addSynchronizeParticipants(new ISynchronizeParticipant[] {participant});
			view.display(participant);
		}
	}

	protected Object getParticipant() {
		return participant;
	}
	
	protected CompareEditorInput getCompareEditorInput() {
		return fCompareEditorInput;
	}
}