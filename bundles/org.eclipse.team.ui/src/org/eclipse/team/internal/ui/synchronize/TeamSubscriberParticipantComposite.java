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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.ui.controls.IControlFactory;
import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.eclipse.team.ui.synchronize.TeamSubscriberParticipant;

public class TeamSubscriberParticipantComposite extends Composite {
	private TeamSubscriberParticipant participant;	
	private Color background;
	private ISynchronizeView view;
	private IControlFactory factory;
	
	public TeamSubscriberParticipantComposite(Composite parent, IControlFactory factory, TeamSubscriberParticipant participant, ISynchronizeView view) {
		super(parent, SWT.NONE);
		this.factory = factory;
		this.participant = participant;		
		this.view = view;
		createComposite(this);
	}
	
	protected Composite createComposite(Composite area) {
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		area.setLayout(layout);
		setBackground(factory.getBackgroundColor());
		{
			final Composite composite_1 = factory.createComposite(this, SWT.NONE);
			GridData gridData = new GridData(GridData.VERTICAL_ALIGN_FILL);
			final GridLayout gridLayout_1 = new GridLayout();
			gridLayout_1.numColumns = 2;
			composite_1.setLayout(gridLayout_1);
			{
				final Label label = factory.createLabel(composite_1, "Last Sync");
				gridData = new GridData();
				gridData.verticalAlignment = GridData.END;
				label.setLayoutData(gridData);
			}
			{
				final Label label = factory.createLabel(composite_1, "11/23/03 10:03:12");
				gridData = new GridData();
				gridData.verticalAlignment = GridData.END;
				label.setLayoutData(gridData);
			}
			{
				final Label label = factory.createLabel(composite_1, "Schedule");
				gridData = new GridData();
				gridData.verticalAlignment = GridData.END;
				label.setLayoutData(gridData);
			}
			{
				final Label label = factory.createLabel(composite_1, "Every Hour");
				gridData = new GridData();
				gridData.verticalAlignment = GridData.END;
				label.setLayoutData(gridData);
			}
			{
				final Label label = factory.createLabel(composite_1, "Status");
				gridData = new GridData();
				gridData.verticalAlignment = GridData.END;
				label.setLayoutData(gridData);
			}
			{
				final Label label = factory.createLabel(composite_1, "Idle");
				gridData = new GridData();
				gridData.verticalAlignment = GridData.END;
				label.setLayoutData(gridData);
			}
		}		
		return area;
	}	
}
