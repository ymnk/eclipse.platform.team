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
package org.eclipse.team.internal.ui.synchronize;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.dialogs.DetailsDialog;
import org.eclipse.team.internal.ui.synchronize.compare.SyncInfoSetCompareInput;
import org.eclipse.team.ui.synchronize.ITeamSubscriberSyncInfoSets;

public class RefreshCompleteDialog extends DetailsDialog {

	private SyncInfo[] changes;
	private ITeamSubscriberSyncInfoSets[] inputs;
	private Button openSyncButton;
	private Button promptWhenNoChanges;
	private Button promptWithChanges;
	private CompareEditorInput compareEditorInput;
	
	public RefreshCompleteDialog(Shell parentShell, SyncInfo[] changes, ITeamSubscriberSyncInfoSets[] inputs) {
		super(parentShell, "Synchronization Complete");
		setImageKey(DLG_IMG_INFO);
		this.changes = changes;
		this.inputs = inputs;
		this.compareEditorInput = new SyncInfoSetCompareInput(new CompareConfiguration(), getResources(), inputs[0]) {
			protected boolean allowParticipantMenuContributions() {
				return true;
			}
		}; 
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.dialogs.DetailsDialog#createMainDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected void createMainDialogArea(Composite parent) {
		String text;
		if(changes.length != 0) {
			text = Integer.toString(changes.length)  + " changes found.";
		} else {
			text = "No changes found.";
		}
		createLabel(parent, text, 2);
		createLabel(parent, "", 2);
		
		
		promptWhenNoChanges = new Button(parent, SWT.CHECK);
		promptWhenNoChanges.setText(Policy.bind("SyncViewerPreferencePage.16"));
		GridData data = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		data.horizontalSpan = 2;
		promptWhenNoChanges.setLayoutData(data);
		
		promptWithChanges = new Button(parent, SWT.CHECK);
		promptWithChanges.setText(Policy.bind("SyncViewerPreferencePage.17"));
		data = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		data.horizontalSpan = 2;
		promptWithChanges.setLayoutData(data);
		
		Dialog.applyDialogFont(parent);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.dialogs.DetailsDialog#createDropDownDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Composite createDropDownDialogArea(Composite parent) {
		try {
			compareEditorInput.run(new NullProgressMonitor());
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
		}
		
		Composite result= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		result.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		data.heightHint = 350;
		//data.widthHint = 700;
		result.setLayoutData(data);
		
		Control c = compareEditorInput.createContents(result);
		data = new GridData(GridData.FILL_BOTH);
		c.setLayoutData(data);
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.dialogs.DetailsDialog#updateEnablements()
	 */
	protected void updateEnablements() {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.dialogs.DetailsDialog#includeCancelButton()
	 */
	protected boolean includeCancelButton() {
		return false;
	}
	
	private Label createLabel(Composite parent, String text, int columns) {
		Label label = new Label(parent, SWT.WRAP);
		label.setText(text);
		GridData data = new GridData();
//				GridData.GRAB_HORIZONTAL |
//				GridData.GRAB_VERTICAL |
//				GridData.HORIZONTAL_ALIGN_FILL |
//				GridData.VERTICAL_ALIGN_CENTER);
//		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
		data.horizontalSpan = columns;		
		label.setLayoutData(data);
		return label;
	}
	
	protected Combo createCombo(Composite parent, int widthChars) {
		Combo combo = new Combo(parent, SWT.READ_ONLY);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		GC gc = new GC(combo);
		gc.setFont(combo.getFont());
		FontMetrics fontMetrics = gc.getFontMetrics();		
		data.widthHint = Dialog.convertWidthInCharsToPixels(fontMetrics, widthChars);
		gc.dispose();
		combo.setLayoutData(data);
		return combo;
	}
	
	private IResource[] getResources() {
		IResource[] resources = new IResource[changes.length];
		for (int i = 0; i < changes.length; i++) {
			SyncInfo info = changes[i];
			resources[i] = info.getLocal();
		}
		return resources;
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.dialogs.DetailsDialog#includeErrorMessage()
	 */
	protected boolean includeErrorMessage() {
		return false;
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.dialogs.DetailsDialog#includeDetailsButton()
	 */
	protected boolean includeDetailsButton() {
		return changes.length != 0;
	}
}