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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.ui.dialogs.DetailsDialog;
import org.eclipse.team.ui.synchronize.ITeamSubscriberSyncInfoSets;

public class RefreshCompleteDialog extends DetailsDialog {

	private SyncInfo[] changes;
	private ITeamSubscriberSyncInfoSets[] inputs;
	private Button openSyncButton;

	public RefreshCompleteDialog(Shell parentShell, SyncInfo[] changes, ITeamSubscriberSyncInfoSets[] inputs) {
		super(parentShell, "Synchronization Complete");
		setImageKey(DLG_IMG_INFO);
		this.changes = changes;
		this.inputs = inputs;
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
		
		Group group = new Group(parent, SWT.SHADOW_ETCHED_IN);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		group.setLayout(layout);
		GridData data = new GridData(GridData.GRAB_HORIZONTAL |
				GridData.GRAB_VERTICAL |
				GridData.HORIZONTAL_ALIGN_FILL |
				GridData.VERTICAL_ALIGN_CENTER);
		data.horizontalSpan = 2;
		group.setLayoutData(data);
		group.setText("Prompt Options");
		createLabel(group, "This settings controls what to do when a Synchronize operation has completed.", 2);
		createLabel(group, "No Changes:", 1);
		Combo combo = createCombo(group, 10);
		combo.add("Prompt");
		combo.add("Open Synchronize View");
		combo.add("Nothing");
		
		createLabel(group, "With Changes:", 1);
		combo = createCombo(group, 10);
		combo.add("Prompt");
		combo.add("Open Synchronize View");
		combo.add("Nothing");
		parent.layout();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.dialogs.DetailsDialog#createDropDownDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Composite createDropDownDialogArea(Composite parent) {
		return null;
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
		if(changes.length != 0) {
			openSyncButton = createButton(parent, 1234, "Synchronize...", true);
		}
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
}