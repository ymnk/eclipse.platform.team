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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.widgets.FormSection;
import org.eclipse.team.internal.ui.widgets.FormWidgetFactory;
import org.eclipse.team.ui.ISharedImages;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ISynchronizeView;


public class ParticipantComposite extends Composite {

	private ISynchronizeParticipant participant;
	
	private Color background;
	private Image participantImage;
	private Image incomingImage = TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_DLG_SYNC_INCOMING).createImage();
	private Image outgoingImage = TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_DLG_SYNC_OUTGOING).createImage();
	private Image conflictingImage = TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_DLG_SYNC_CONFLICTING).createImage();

	private ISynchronizeView view;
	private FormSection dropDownSection;
	
	public ParticipantComposite(Composite parent, ISynchronizeParticipant participant, ISynchronizeView view, int style) {
		super(parent, style);
		this.participant = participant;		
		this.background = new Color(parent.getDisplay(), new RGB(255, 255, 255));
		this.participantImage = participant.getImageDescriptor().createImage();
		this.view = view;		
		this.dropDownSection = new FormSection() {
			/* (non-Javadoc)
			 * @see org.eclipse.team.internal.ui.widgets.FormSection#getDescription()
			 */
			public String getDescription() {
				return "This is the place where the resources are";
			}

			/* (non-Javadoc)
			 * @see org.eclipse.team.internal.ui.widgets.FormSection#getHeaderText()
			 */
			public String getHeaderText() {
				return "Resources are here";
			}
		
			/* (non-Javadoc)
			 * @see org.eclipse.team.internal.ui.widgets.FormSection#createClient(org.eclipse.swt.widgets.Composite, org.eclipse.team.internal.ui.widgets.FormWidgetFactory)
			 */
			public Composite createClient(Composite parent, FormWidgetFactory factory) {
				Composite top = factory.createComposite(parent);
				GridLayout layout = new GridLayout();
				layout.numColumns = 1;
				//layout.verticalSpacing = 9;
				//layout.horizontalSpacing = 6;
				top.setLayout(layout);
				top.setBackground(new Color(top.getDisplay(), new RGB(134,145,56)));
				//top.setBackground(getBackgroundColor());
				{Label label = new Label(top, SWT.NONE);
				label.setText("Hello!!!");}
				{Label label = new Label(top, SWT.NONE);
				label.setText("Hello!!!");}
				{Label label = new Label(top, SWT.NONE);
				label.setText("Hello!!!");}
				{Label label = new Label(top, SWT.NONE);
				label.setText("Hello!!!");}
				//factory.paintBordersFor(top);
				return top;				
			}
			protected void reflow() {
				super.reflow();
				setRedraw(false);
				getParent().setRedraw(false);
				layout(true);
				getParent().layout(true);
				setRedraw(true);
				getParent().setRedraw(true);
			}
		};
		dropDownSection.setCollapsable(true);
		dropDownSection.setCollapsed(false);
		createComposite(this);
		//dropDownSection.setDescription("asdkjfh aklhfd kahsdf kjgdaskjhsagf kjgsdaf j ");
		
//		Control control = dropDownSection.createControl(this, factory);
//		control.setBackground(new Color(parent.getDisplay(), new RGB(0,0,0)));
//		GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);
//		control.setLayoutData(gd);
		
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
			final Composite composite = new Composite(area, SWT.NONE);
			composite.setBackground(getBackgroundColor());
			composite.setLayoutData(new GridData(GridData.FILL_BOTH));
			final GridLayout gridLayout = new GridLayout();
			gridLayout.numColumns = 3;
			composite.setLayout(gridLayout);
			{
				final Label label = new Label(composite, SWT.NONE);
				final GridData gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
				gridData.verticalSpan = 5;
				label.setLayoutData(gridData);
				label.setImage(participantImage);
				label.setBackground(getBackgroundColor());
			}
			{
				final Composite composite_1 = new Composite(composite, SWT.NONE);
				composite_1.setBackground(getBackgroundColor());
				final GridData gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);
				gridData.verticalSpan = 5;
				gridData.horizontalSpan = 1;
				composite_1.setLayoutData(gridData);
				final GridLayout gridLayout_1 = new GridLayout();
				gridLayout_1.numColumns = 2;
				gridLayout_1.marginWidth = 0;
				gridLayout_1.marginHeight = 0;
				composite_1.setLayout(gridLayout_1);
				{
					final Label label = new Label(composite_1, SWT.NONE);
					final GridData gridData_1 = new GridData();
					gridData_1.horizontalSpan = 2;
					label.setLayoutData(gridData_1);
					label.setFont(JFaceResources.getHeaderFont());
					label.setText(participant.getName());
					label.setBackground(getBackgroundColor());
				}
				{
					final Label label = new Label(composite_1, SWT.NONE);
					final GridData gridData_1 = new GridData(GridData.FILL_BOTH);
					gridData_1.horizontalSpan = 2;
					gridData_1.verticalSpan = 3;
					label.setLayoutData(gridData_1);
					label.setText("This synchronizes all shared projects with their associated remote repositories.");
					label.setBackground(getBackgroundColor());
				}
				{
//					final Label label = new Label(composite_1, SWT.NONE);
//					label.setText("Resources:");
//					label.setBackground(getBackgroundColor());
					FormWidgetFactory factory = new FormWidgetFactory(getDisplay());
					factory.setBackgroundColor(getBackgroundColor());
					Control control = dropDownSection.createControl(composite_1, factory);
		control.setBackground(new Color(composite_1.getDisplay(), new RGB(0,0,0)));
		GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		control.setLayoutData(gd);
				}
				{
//					final Combo combo = new Combo(composite_1, SWT.READ_ONLY);
//					combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//					combo.setBackground(getBackgroundColor());
				}
			}
			{
				final Composite composite_1 = new Composite(composite, SWT.NONE);
				GridData gridData = new GridData(GridData.VERTICAL_ALIGN_FILL);
				gridData.verticalSpan = 5;
				gridData.horizontalSpan = 1;
				composite_1.setLayoutData(gridData);
				final GridLayout gridLayout_1 = new GridLayout();
				gridLayout_1.numColumns = 2;
				composite_1.setLayout(gridLayout_1);
				composite_1.setBackground(getBackgroundColor());
				{
					final Label label = new Label(composite_1, SWT.NONE);
					label.setText("Last Sync");
					label.setBackground(getBackgroundColor());
					gridData = new GridData();
					gridData.verticalAlignment = GridData.END;
					label.setLayoutData(gridData);
				}
				{
					final Label label = new Label(composite_1, SWT.NONE);
					label.setText("11/23/03 10:03:12");
					label.setBackground(getBackgroundColor());
					gridData = new GridData();
					gridData.verticalAlignment = GridData.END;
					label.setLayoutData(gridData);
				}
				{
					final Label label = new Label(composite_1, SWT.NONE);
					label.setText("Schedule");
					label.setBackground(getBackgroundColor());
					gridData = new GridData();
					gridData.verticalAlignment = GridData.END;
					label.setLayoutData(gridData);
				}
				{
					final Label label = new Label(composite_1, SWT.NONE);
					label.setText("Every hour");
					label.setBackground(getBackgroundColor());
					gridData = new GridData();
					gridData.verticalAlignment = GridData.END;
					label.setLayoutData(gridData);
				}
				{
					final Label label = new Label(composite_1, SWT.NONE);
					label.setText("Status");
					label.setBackground(getBackgroundColor());
					gridData = new GridData();
					gridData.verticalAlignment = GridData.END;
					label.setLayoutData(gridData);
				}
				{
					final Label label = new Label(composite_1, SWT.NONE);
					label.setText("Idle");
					label.setBackground(getBackgroundColor());
					gridData = new GridData();
					gridData.verticalAlignment = GridData.END;
					label.setLayoutData(gridData);
				}
			}
			{
				final Composite composite_1 = new Composite(composite, SWT.NONE);
				final GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
				gridData.horizontalSpan = 3;
				composite_1.setLayoutData(gridData);
				final GridLayout gridLayout_1 = new GridLayout();
				gridLayout_1.marginWidth = 0;
				gridLayout_1.marginHeight = 0;
				gridLayout_1.numColumns = 8;
				composite_1.setLayout(gridLayout_1);
				composite_1.setBackground(getBackgroundColor());
				{
					final Label label = new Label(composite_1, SWT.NONE);
					label.setImage(incomingImage);
					label.setBackground(getBackgroundColor());
				}

				{
					final Label label = new Label(composite_1, SWT.NONE);
					label.setText("12");
					label.setBackground(getBackgroundColor());
				}
				{
					final Label label = new Label(composite_1, SWT.NONE);
					label.setImage(outgoingImage);
					label.setBackground(getBackgroundColor());
				}
				{
					final Label label = new Label(composite_1, SWT.NONE);
					label.setText("0");
					label.setBackground(getBackgroundColor());
				}
				{
					final Label label = new Label(composite_1, SWT.NONE);
					label.setImage(conflictingImage);
					label.setBackground(getBackgroundColor());
				}
				{
					final Label label = new Label(composite_1, SWT.NONE);
					label.setBackground(getBackgroundColor());
					label.setText("55");
				}
				{
					final Button button = new Button(composite_1, SWT.FLAT);
					button.setText("Setup...");
				}
				{
					final Button button = new Button(composite_1, SWT.FLAT);
					button.setText("Synchronize");
					button.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							view.display(participant);
						}
					});
				}
			}
		}
		return area;
	}

	/**
	 * @return
	 */
	private Color getBackgroundColor() {
		return this.background;
	}
}
