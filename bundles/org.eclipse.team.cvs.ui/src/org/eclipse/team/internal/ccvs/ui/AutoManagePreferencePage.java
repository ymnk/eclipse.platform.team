/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 * IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.team.internal.ccvs.ui;
 
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.team.core.IIgnoreInfo;
import org.eclipse.team.core.TeamPlugin;
import org.eclipse.team.ui.TeamUIPlugin;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class AutoManagePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	private Table autoManageTable;
	private Button addButton;
	private Button removeButton;
	public void init(IWorkbench workbench) {
		setDescription(Policy.bind("AutoManagePreferencePage.description"));
	}
	
	/**
	 * Creates preference page controls on demand.
	 *
	 * @param parent  the parent for the preference page
	 */
	protected Control createContents(Composite ancestor) {
		noDefaultAndApplyButton();
		
		Composite parent = new Composite(ancestor, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		parent.setLayout(layout);
	
		// set F1 help
		//WorkbenchHelp.setHelp(parent, new DialogPageContextComputer (this, IVCMHelpContextIds.IGNORE_PREFERENCE_PAGE));
		
		Label l1 = new Label(parent, SWT.NULL);
		l1.setText(Policy.bind("AutoManagePreferencePage.autoManagePatterns"));
		GridData data = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		data.horizontalSpan = 2;
		l1.setLayoutData(data);
		
		autoManageTable = new Table(parent, SWT.CHECK | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint= convertWidthInCharsToPixels(30);
		autoManageTable.setLayoutData(gd);
		autoManageTable.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				handleSelection();
			}
		});
		
		Composite buttons = new Composite(parent, SWT.NULL);
		buttons.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		buttons.setLayout(new GridLayout());
		
		addButton = new Button(buttons, SWT.PUSH);
		addButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		addButton.setText(Policy.bind("AutoManagePreferencePage.add"));
		addButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				addItem();
			}
		});
		
		
		removeButton = new Button(buttons, SWT.PUSH);
		removeButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		removeButton.setText(Policy.bind("AutoManagePreferencePage.remove"));
		removeButton.setEnabled(false);
		removeButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				removeItem();
			}
		});
		
		fillTable();
		
		return parent;
	}
	/**
	 * Do anything necessary because the OK button has been pressed.
	 *
	 * @return whether it is okay to close the preference page
	 */
	public boolean performOk() {
		int count = autoManageTable.getItemCount();
		String[] patterns = new String[count];
		boolean[] enabled = new boolean[count];
		TableItem[] items = autoManageTable.getItems();
		for (int i = 0; i < count; i++) {
			patterns[i] = items[i].getText();
			enabled[i] = items[i].getChecked();
		}
		CVSUIPlugin.getPlugin().setAutoManagePatterns(patterns);
		// XXX Should we search for unmanaged resources that match the provided patterns?
		return true;
	}
	
	private void fillTable() {
		String[] patterns = CVSUIPlugin.getPlugin().getAutoManagePatterns();
		for (int i = 0; i < patterns.length; i++) {
			TableItem item = new TableItem(autoManageTable, SWT.NONE);
			item.setText(patterns[i]);
			item.setChecked(true);
		}
	}
	
	private void addItem() {
		InputDialog dialog = new InputDialog(getShell(), Policy.bind("AutoManagePreferencePage.enterPatternShort"), Policy.bind("AutoManagePreferencePage.enterPatternLong"), null, null);
		dialog.open();
		if (dialog.getReturnCode() != InputDialog.OK) return;
		String pattern = dialog.getValue();
		if (pattern.equals("")) return;
		// Check if the item already exists
		TableItem[] items = autoManageTable.getItems();
		for (int i = 0; i < items.length; i++) {
			if (items[i].getText().equals(pattern)) {
				MessageDialog.openWarning(getShell(), Policy.bind("AutoManagePreferencePage.patternExistsShort"), Policy.bind("AutoManagePreferencePage.patternExistsLong"));
				return;
			}
		}
		TableItem item = new TableItem(autoManageTable, SWT.NONE);
		item.setText(pattern);
		item.setChecked(true);
	}
	
	private void removeItem() {
		int[] selection = autoManageTable.getSelectionIndices();
		autoManageTable.remove(selection);
	}
	private void handleSelection() {
		if (autoManageTable.getSelectionCount() > 0) {
			removeButton.setEnabled(true);
		} else {
			removeButton.setEnabled(false);
		}
	}
}
