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
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.dialogs.DetailsDialog;
import org.eclipse.team.internal.ui.jobs.IRefreshEvent;
import org.eclipse.team.internal.ui.synchronize.compare.SyncInfoSetCompareInput;

public class RefreshCompleteDialog extends DetailsDialog {

	private Button promptWhenNoChanges;
	private Button promptWithChanges;
	private CompareEditorInput compareEditorInput;
	private IRefreshEvent event;
	private final static int RESOURCE_LIST_SIZE = 10;
	private IDialogSettings settings;
	private static final String HEIGHT_KEY = "width-key";
	private static final String WIDTH_KEY = "height-key";
	
	public RefreshCompleteDialog(Shell parentShell, IRefreshEvent event) {
		super(parentShell, "Synchronization Complete - " + event.getParticipant().getName());
		int shellStyle = getShellStyle();
		setShellStyle(shellStyle | SWT.RESIZE | SWT.MAX);
		this.event = event;
		setImageKey(DLG_IMG_INFO);
		
		// create the compare input for the details area
		this.compareEditorInput = new SyncInfoSetCompareInput(new CompareConfiguration(), getResources(), null /* no filter */,  event.getParticipant().getInput()) {
			protected boolean allowParticipantMenuContributions() {
				return true;
			}
		}; 
		
		IDialogSettings workbenchSettings = TeamUIPlugin.getPlugin().getDialogSettings();
		this.settings = workbenchSettings.getSection("RefreshCompleteDialog");//$NON-NLS-1$
		if (settings == null) {
			this.settings = workbenchSettings.addNewSection("RefreshCompleteDialog");//$NON-NLS-1$
		}	
	}

	/**
	 * @see Window#getInitialSize()
	 */
	protected Point getInitialSize() {
		int width, height;
		try {
			height = settings.getInt(HEIGHT_KEY);
			width = settings.getInt(WIDTH_KEY);
		} catch(NumberFormatException e) {
			return super.getInitialSize();
		}
		Point p = super.getInitialSize();
		return new Point(width, p.y);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#close()
	 */
	public boolean close() {
		Rectangle bounds = getShell().getBounds();
		settings.put(HEIGHT_KEY, bounds.height);
		settings.put(WIDTH_KEY, bounds.width);
		return super.close();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.dialogs.DetailsDialog#createMainDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected void createMainDialogArea(Composite parent) {
		StringBuffer text = new StringBuffer();
		SyncInfo[] changes = event.getChanges();
		IResource[] resources = event.getResources();
		
		if(changes.length != 0) {
			text.append(Integer.toString(changes.length)  + " changes found ");
		} else {
			text.append("No changes found ");
		}
		text.append("refreshing " + Integer.toString(resources.length) + " resource(s).");		
		createLabel(parent, text.toString(), 2);
		
		StringBuffer resourcesLabel = new StringBuffer();
		StringBuffer resourcesFullListLabel = new StringBuffer();
		for (int i = 0; i < resources.length; i++) {
			if(i < RESOURCE_LIST_SIZE) {
				String EOL = i < RESOURCE_LIST_SIZE - 1 ? "\n" : "...";
				resourcesLabel.append(resources[i].getFullPath() + EOL);
			}
			resourcesFullListLabel.append(resources[i].getFullPath() + "\n");
		}
		Label l = createLabel(parent, resourcesLabel.toString(), 2);
		l.setToolTipText(resourcesFullListLabel.toString());
		
		createLabel(parent, "", 2);
		promptWhenNoChanges = new Button(parent, SWT.CHECK);
		GridData data = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		data.horizontalSpan = 2;
		promptWhenNoChanges.setLayoutData(data);
		
		promptWithChanges = new Button(parent, SWT.CHECK);

		data = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		data.horizontalSpan = 2;
		promptWithChanges.setLayoutData(data);

		if(event.getRefreshType() == IRefreshEvent.USER_REFRESH) {
			promptWhenNoChanges.setText(Policy.bind("SyncViewerPreferencePage.16"));
			promptWhenNoChanges.setSelection(TeamUIPlugin.getPlugin().getPreferenceStore().getBoolean(IPreferenceIds.SYNCVIEW_VIEW_PROMPT_WHEN_NO_CHANGES));
			promptWithChanges.setText(Policy.bind("SyncViewerPreferencePage.17"));
			promptWithChanges.setSelection(TeamUIPlugin.getPlugin().getPreferenceStore().getBoolean(IPreferenceIds.SYNCVIEW_VIEW_PROMPT_WITH_CHANGES));
			
		} else {
			promptWhenNoChanges.setText(Policy.bind("SyncViewerPreferencePage.31"));
			promptWhenNoChanges.setSelection(TeamUIPlugin.getPlugin().getPreferenceStore().getBoolean(IPreferenceIds.SYNCVIEW_VIEW_BKG_PROMPT_WHEN_NO_CHANGES));
			promptWithChanges.setText(Policy.bind("SyncViewerPreferencePage.32"));
			promptWithChanges.setSelection(TeamUIPlugin.getPlugin().getPreferenceStore().getBoolean(IPreferenceIds.SYNCVIEW_VIEW_BKG_PROMPT_WITH_CHANGES));
		}
				
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
		SyncInfo[] changes = event.getChanges();
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
		return event.getChanges().length != 0;
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {		
		if(event.getRefreshType() == IRefreshEvent.USER_REFRESH) {
			TeamUIPlugin.getPlugin().getPreferenceStore().setValue(IPreferenceIds.SYNCVIEW_VIEW_PROMPT_WHEN_NO_CHANGES, promptWhenNoChanges.getSelection());
			TeamUIPlugin.getPlugin().getPreferenceStore().setValue(IPreferenceIds.SYNCVIEW_VIEW_PROMPT_WITH_CHANGES, promptWithChanges.getSelection());			
		} else {
			TeamUIPlugin.getPlugin().getPreferenceStore().setValue(IPreferenceIds.SYNCVIEW_VIEW_BKG_PROMPT_WHEN_NO_CHANGES, promptWhenNoChanges.getSelection());
			TeamUIPlugin.getPlugin().getPreferenceStore().setValue(IPreferenceIds.SYNCVIEW_VIEW_BKG_PROMPT_WITH_CHANGES, promptWithChanges.getSelection());
		}
		TeamUIPlugin.getPlugin().savePluginPreferences();
		super.okPressed();		
	}
}