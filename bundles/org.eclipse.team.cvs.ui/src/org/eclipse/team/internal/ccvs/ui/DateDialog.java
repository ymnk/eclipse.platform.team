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
package org.eclipse.team.internal.ccvs.ui;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog for obtaining a date from the user
 */
public class DateDialog extends Dialog {

	private Combo fromDayCombo;
	private Combo fromMonthCombo;
	private Combo fromYearCombo;
	private Combo hourCombo;
	private Combo minuteCombo;
	private Combo secondCombo;
	private Button includeTime, localTime, utcTime;

	protected DateDialog(Shell parentShell) {
		super(parentShell);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite topLevel = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		topLevel.setLayout(layout);
		
		createDateArea(topLevel);
		createTimeArea(topLevel);
		initializeValues();
		updateWidgetEnablements();
		
		// set F1 help
		//WorkbenchHelp.setHelp(topLevel, IHelpContextIds.HISTORY_FILTER_DIALOG);
		Dialog.applyDialogFont(parent);
		return topLevel;
	}

	private void createDateArea(Composite topLevel) {
		// Create the date area
		Label label = new Label(topLevel, SWT.NONE);
		label.setText("Date (M/D/Y):");
		Composite dateComposite = new Composite(topLevel, SWT.NONE);
		GridLayout dateLayout = new GridLayout();
		dateLayout.numColumns = 3;
		dateComposite.setLayout(dateLayout);
		fromMonthCombo = new Combo(dateComposite, SWT.READ_ONLY);
		fromDayCombo = new Combo(dateComposite, SWT.READ_ONLY);
		fromYearCombo = new Combo(dateComposite, SWT.NONE);
		fromYearCombo.setTextLimit(4);
		
		//set day, month and year combos with numbers
		//years allows a selection from the past 5 years
		//or any year written in
		String days[] = new String[31];
		for (int i = 0; i < 31; i++) {
			days[i] = String.valueOf(i);
		}

		String months[] = new String[12];
		SimpleDateFormat format = new SimpleDateFormat("MMMM"); //$NON-NLS-1$
		Calendar calendar = Calendar.getInstance();
		for (int i = 0; i < 12; i++) {
			calendar.set(Calendar.MONTH, i);
			months[i] = format.format(calendar.getTime());
		}

		String years[] = new String[5];
		Calendar calender = Calendar.getInstance();
		for (int i = 0; i < 5; i++) {
			years[i] = String.valueOf(calender.get(1) - i);
		}
		fromDayCombo.setItems(days);
		fromMonthCombo.setItems(months);
		fromYearCombo.setItems(years);
	}

	private void createTimeArea(Composite topLevel) {
		includeTime = createCheckBox(topLevel, "Include time component in tag");
		Label label = new Label(topLevel, SWT.NONE);
		label.setText("Time (HH:MM:SS):");
		Composite dateComposite = new Composite(topLevel, SWT.NONE);
		GridLayout dateLayout = new GridLayout();
		dateLayout.numColumns = 3;
		dateComposite.setLayout(dateLayout);
		hourCombo = new Combo(dateComposite, SWT.READ_ONLY);
		minuteCombo = new Combo(dateComposite, SWT.READ_ONLY);
		secondCombo = new Combo(dateComposite, SWT.READ_ONLY);
		localTime = createRadioButton(topLevel, "Time is local");
		utcTime = createRadioButton(topLevel, "Time is in universal time coordinates (UTC)");
		
		String sixty[] = new String[6];
		for (int i = 0; i < 60; i++) {
			sixty[i] = String.valueOf(i);
		}
		String hours[] = new String[24];
		for (int i = 0; i < 24; i++) {
			hours[i] = String.valueOf(i);
		}
		hourCombo.setItems(hours);
		minuteCombo.setItems(sixty);
		secondCombo.setItems(sixty);
	}

	private void initializeValues() {
		Calendar calendar = Calendar.getInstance();
		fromDayCombo.select(calendar.get(Calendar.DATE) - 1);
		fromMonthCombo.select(calendar.get(Calendar.MONTH));
		String yearValue = String.valueOf(calendar.get(Calendar.YEAR));
		int index = fromYearCombo.indexOf(yearValue);
		if (index == -1) {
			fromYearCombo.add(yearValue);
			index = fromYearCombo.indexOf(yearValue);
		}
		fromYearCombo.select(index);
		hourCombo.select(calendar.get(Calendar.HOUR));
		minuteCombo.select(calendar.get(Calendar.MINUTE));
		secondCombo.select(calendar.get(Calendar.SECOND));
		
		includeTime.setSelection(false); // TODO: stroe in dialog properties
		localTime.setSelection(true);
		utcTime.setSelection(false);
	}
	
	private void updateWidgetEnablements() {
		if (includeTime.getSelection()) {
			// TODO: enable time widgets
		} else {
			
		}
		
	}
}
