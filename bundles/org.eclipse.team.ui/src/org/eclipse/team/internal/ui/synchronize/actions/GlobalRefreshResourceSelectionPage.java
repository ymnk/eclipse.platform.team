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
package org.eclipse.team.internal.ui.synchronize.actions;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.ui.synchronize.subscriber.SubscriberParticipant;
import org.eclipse.ui.internal.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceSorter;

/**
 * Page that allows the user to select a set of resources that are managed
 * by a synchronize participant.
 * 
 * Remembers last selection
 * Remembers last working set
 * 
 * @since 3.0
 */
public class GlobalRefreshResourceSelectionPage extends WizardPage {

	private SubscriberParticipant participant;
	private Button dontPrompt;
	private ContainerCheckedTreeViewer fViewer;

	class MyContentProvider extends BaseWorkbenchContentProvider {
		public Object[] getChildren(Object element) {
			if(element instanceof SubscriberParticipant) {
				return ((SubscriberParticipant)element).getResources();
			}
			return super.getChildren(element);
		}
	}
	
	class MyLabelProvider extends LabelProvider {
		private LabelProvider workbenchProvider = new WorkbenchLabelProvider();
		public String getText(Object element) {
			if(element instanceof IContainer) {
				IContainer c = (IContainer)element;
				List participantRoots = Arrays.asList(participant.getResources());
				if(participantRoots.contains(c)) {
					return c.getFullPath().toString();
				}
			}
			return workbenchProvider.getText(element);
		}	
		public Image getImage(Object element) {
			return workbenchProvider.getImage(element);
		}
	}
		
	public GlobalRefreshResourceSelectionPage(SubscriberParticipant participant) {
		super("Synchronize");
		this.participant = participant;
		setDescription("Select the resource to synchronize");
		setTitle("Synchronize");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent2) {
		Composite top = new Composite(parent2, SWT.NULL);
		top.setLayout(new GridLayout());
		setControl(top);
		
		Label l = new Label(parent2, SWT.NULL);
		l.setText("Available resources to Synchronize:");
		fViewer = new ContainerCheckedTreeViewer(top, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_BOTH);
		fViewer.getControl().setLayoutData(data);
		fViewer.setContentProvider(new MyContentProvider());
		fViewer.setLabelProvider(new MyLabelProvider());
		fViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				updateOKStatus();
			}
		});
		fViewer.setSorter(new ResourceSorter(ResourceSorter.NAME));

		fViewer.setInput(participant);
		
		Button selectAll = new Button(top, SWT.NULL);
		selectAll.setText("&Select All");
		selectAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fViewer.setCheckedElements(participant.getResources());
			}
		});
		
		Button deSelectAll = new Button(top, SWT.NULL);
		deSelectAll.setText("&Deselect All");
		deSelectAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fViewer.setCheckedElements(new Object[0]);
			}
		});
		
		//dontPrompt = new Button(top, SWT.CHECK);
		//dontPrompt.setText("Don't prompt for resources anymore. Refresh the selection or the current working set in the Synchronize View.");
		 Dialog.applyDialogFont(parent2);
	}

	protected void updateOKStatus() {	
	}
}
