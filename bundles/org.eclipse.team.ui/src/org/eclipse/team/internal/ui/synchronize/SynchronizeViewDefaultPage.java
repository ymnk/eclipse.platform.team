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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.HyperlinkGroup;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.part.Page;

public class SynchronizeViewDefaultPage extends Page {

	private FormToolkit forms;
	private Composite composite;

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		composite = new Composite(parent, SWT.NONE);
		composite.setBackground(parent.getShell().getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessVerticalSpace = true;
		composite.setLayoutData(data);

		forms = new FormToolkit(parent.getDisplay());
		forms.setBackground(parent.getShell().getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		HyperlinkGroup group = forms.getHyperlinkGroup();
		group.setBackground(parent.getShell().getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

		createDescriptionLabel(composite, "There are no synchronize participants created yet. Please create one:"); //$NON-NLS-1$
		Hyperlink link = forms.createHyperlink(composite, "Create", SWT.WRAP); //$NON-NLS-1$
		link.addHyperlinkListener(new HyperlinkAdapter() {

			public void linkActivated(HyperlinkEvent e) {

			}
		});
		forms.getHyperlinkGroup().add(link);
	}

	private Label createDescriptionLabel(Composite parent, String text) {
		Label description = new Label(parent, SWT.WRAP);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		data.widthHint = 100;
		description.setLayoutData(data);
		description.setText(text);
		description.setBackground(parent.getShell().getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		return description;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.MessagePage#setFocus()
	 */
	public void setFocus() {
		composite.setFocus();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.Page#dispose()
	 */
	public void dispose() {
		forms.dispose();
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.Page#getControl()
	 */
	public Control getControl() {
		return composite;
	}
}