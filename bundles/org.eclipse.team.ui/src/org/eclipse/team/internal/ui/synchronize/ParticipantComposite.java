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

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ui.widgets.HyperlinkAdapter;
import org.eclipse.team.ui.controls.IControlFactory;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ISynchronizeView;


public class ParticipantComposite extends Composite {

	private ISynchronizeParticipant participant;	
	private Image participantImage;
	private ISynchronizeView view;
	private IControlFactory factory;
	private Composite participantComposite;
	
	public ParticipantComposite(Composite parent, IControlFactory factory, ISynchronizeParticipant participant, ISynchronizeView view, int style) {
		super(parent, style);
		this.factory = factory;
		this.participant = participant;		
		this.participantImage = participant.getImageDescriptor().createImage();
		this.view = view;		
		createComposite(this);
	}
	
	protected Composite createComposite(Composite area) {
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		area.setLayout(layout);
		{
			final GridData gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);
			area.setLayoutData(gridData);
		}
		{
			final Composite composite = factory.createComposite(area);
			composite.setLayoutData(new GridData(GridData.FILL_BOTH));
			final GridLayout gridLayout = new GridLayout();
			gridLayout.numColumns = 3;
			composite.setLayout(gridLayout);
			{
				final Label label = factory.createLabel(composite, "", SWT.NONE);
				final GridData gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
				label.setLayoutData(gridData);
				label.setImage(participantImage);
			}
			{
				final Label label = factory.createLabel(composite, participant.getName(), SWT.WRAP);
				label.setLayoutData(new GridData());
				label.setFont(JFaceResources.getHeaderFont());
				label.setText(participant.getName());
			}
			{
				final Label label = factory.createLabel(composite, "Goto Page", SWT.NONE);
				final GridData gridData_1 = new GridData();				
				gridData_1.horizontalAlignment = GridData.END;
				label.setLayoutData(gridData_1);
				factory.turnIntoHyperlink(label, new HyperlinkAdapter() {
					public void linkActivated(Control linkLabel) {
						view.display(participant);
					}
				});
			}			
			{
				final Composite composite_1 = factory.createComposite(composite, SWT.NONE);
				final GridData gridData = new GridData(GridData.FILL_BOTH);
				gridData.horizontalSpan = 3;
				composite_1.setLayoutData(gridData);
				final GridLayout gridLayout_1 = new GridLayout();
				gridLayout_1.marginWidth = 0;
				gridLayout_1.marginHeight = 0;
				composite_1.setLayout(gridLayout_1);
				participantComposite = participant.createOverviewComposite(composite_1, factory, view);
			}			
			{
				final Composite composite_1 = factory.createComposite(composite, SWT.NONE);
				final GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
				gridData.horizontalSpan = 3;
				composite_1.setLayoutData(gridData);
				final GridLayout gridLayout_1 = new GridLayout();
				gridLayout_1.marginWidth = 0;
				gridLayout_1.marginHeight = 0;
				composite_1.setLayout(gridLayout_1);
				{
					final Button button = factory.createButton(composite_1, "Setup...", SWT.FLAT);
					GridData gd = new GridData();
					gd.horizontalAlignment = GridData.END;
					button.setLayoutData(gd);
				}
			}
		}
		return area;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	public void dispose() {
		super.dispose();
		participantComposite.dispose();
		participantImage.dispose();
	}
}
