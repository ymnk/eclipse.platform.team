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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.internal.ResizableDialog;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.*;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;


public class CompareDialog extends ResizableDialog implements IPropertyChangeListener {
		
	private CompareEditorInput fCompareEditorInput;
	private ISynchronizeParticipant participant;
	private Button saveButton;

	public CompareDialog(Shell shell, CompareEditorInput input) {
		super(shell, null);
		Assert.isNotNull(input);
		fCompareEditorInput= input;
		fCompareEditorInput.addPropertyChangeListener(this);
	}
	
	public void setSynchronizeParticipant(ISynchronizeParticipant participant) {
		this.participant = participant;
	}
		
	public void propertyChange(PropertyChangeEvent event) {
		if (saveButton != null && fCompareEditorInput != null)
			saveButton.setEnabled(fCompareEditorInput.isSaveNeeded());
	}
	
	/* (non-Javadoc)
	 * Method declared on Dialog.
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		if(participant != null) {
			Button b = createButton(parent, IDialogConstants.OPEN_ID, "Add to Synchronize View", false);
			b.setToolTipText("Remembering this comparison will add it to the Synchronize View.");
		}
		saveButton = createButton(parent, IDialogConstants.OK_ID, "Save", true);
		saveButton.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	/* (non-Javadoc)
	 * Method declared on Dialog.
	 */
	protected Control createDialogArea(Composite parent2) {
						
		Composite parent= (Composite) super.createDialogArea(parent2);

		Control c= fCompareEditorInput.createContents(parent);
		c.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Shell shell= c.getShell();
		shell.setText(fCompareEditorInput.getTitle());
		shell.setImage(fCompareEditorInput.getTitleImage());
		Dialog.applyDialogFont(parent2);
		return parent;
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	protected void buttonPressed(int buttonId) {
		if(buttonId == IDialogConstants.OPEN_ID) {
			ISynchronizeManager mgr = TeamUI.getSynchronizeManager();
			ISynchronizeView view = mgr.showSynchronizeViewInActivePage(null);
			mgr.addSynchronizeParticipants(new ISynchronizeParticipant[] {participant});
			view.display(participant);
			okPressed();
			return;
		} else	if (buttonId == IDialogConstants.OK_ID && fCompareEditorInput.isSaveNeeded()) {				
			final WorkspaceModifyOperation operation= new WorkspaceModifyOperation() {
				public void execute(IProgressMonitor pm) throws CoreException {
					fCompareEditorInput.saveChanges(pm);
				}
			};								
				TeamUIPlugin.getStandardDisplay().asyncExec(new Runnable() {
					public void run() {
						BusyIndicator.showWhile(null, new Runnable() {
							public void run() {
								try {
								PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
									public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
										operation.run(monitor);
									}
								});		
							} catch (InterruptedException x) {
								// NeedWork
							} catch (OperationCanceledException x) {
								// NeedWork
							} catch (InvocationTargetException x) {
								Utils.handle(x);
							}
							}
						});
					}
				});
		}
		super.buttonPressed(buttonId);
	}
}
