/*************.******************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.sync.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * Composite that displays statistics relating to Synchronization information.
 *
 * @since 3.0 
 */
public class StatisticsPanel extends Composite {
			
	private StatisticsCounterBar bar;		
			
	public StatisticsPanel(Composite parent) {
		super(parent, SWT.WRAP);
		GridLayout gridLayout= new GridLayout();
		gridLayout.numColumns= 1;
		gridLayout.makeColumnsEqualWidth= false;
		gridLayout.marginWidth= 3;
		setLayout(gridLayout);
		bar = new StatisticsCounterBar(this);
		bar.step(5);
		bar.setMaximum(10);
		new Label(this, SWT.WRAP).setText("1");
		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				disposeIcons();
			}
		});
		setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
	}

	protected void disposeIcons() {
	}
}
