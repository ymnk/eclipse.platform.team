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
package org.eclipse.team.internal.ccvs.ui.wizards;


import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.ccvs.ui.subscriber.WorkspaceSynchronizeParticipant;
import org.eclipse.team.internal.ui.synchronize.SyncChangesTreeViewer;
import org.eclipse.team.internal.ui.synchronize.compare.SyncInfoCompareInput;
import org.eclipse.team.internal.ui.synchronize.compare.SyncInfoDiffNode;
import org.eclipse.team.internal.ui.synchronize.views.TeamSubscriberParticipantLabelProvider;
import org.eclipse.team.internal.ui.widgets.*;
import org.eclipse.team.ui.synchronize.ITeamSubscriberParticipantNode;
import org.eclipse.team.ui.synchronize.SyncChangesViewer;
import org.eclipse.ui.part.PageBook;

public class SharingWizardFinishPage extends CVSWizardPage {
	private WorkspaceSynchronizeParticipant participant;

	public SharingWizardFinishPage(String pageName, String title, WorkspaceSynchronizeParticipant participant, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		this.participant = participant;
	}

	/*
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
//		Composite composite = createComposite(parent, 1);
//		// set F1 help
//		WorkbenchHelp.setHelp(composite, IHelpContextIds.SHARING_FINISH_PAGE);
//		Label label = new Label(composite, SWT.LEFT | SWT.WRAP);
//		label.setText(Policy.bind("SharingWizardFinishPage.message")); //$NON-NLS-1$
//		GridData data = new GridData();
//		data.widthHint = 350;
//		label.setLayoutData(data);
//		setControl(composite);
//        Dialog.applyDialogFont(parent);
		
		setControl(createCoolControl(parent));
		
		
	}
	
	private static class NullPreviewer implements IChangePreviewViewer {
		private Label fLabel;
		public void createControl(Composite parent) {
			fLabel= new Label(parent, SWT.CENTER | SWT.FLAT);
			fLabel.setText("No preview available"); //$NON-NLS-1$
		}
		public void refresh() {
		}
		public Control getControl() {
			return fLabel;
		}
		public void setInput(Object input) throws CoreException {
		}
	}
	
	private PageBook fPreviewContainer;
	private IChangePreviewViewer fNullPreviewer;
	private IChangePreviewViewer fChangePreviewViewer;
	private IChangePreviewViewer fCurrentPreviewViewer;
	private Viewer fTreeViewer;
	private ITeamSubscriberParticipantNode fCurrentSelection = null;
	
	public Composite createCoolControl(Composite parent) {
		Composite result= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0; layout.marginWidth= 0;
		result.setLayout(layout);
		
		SashForm sashForm= new SashForm(result, SWT.VERTICAL);
		
		ViewerPane pane= new ViewerPane(sashForm, SWT.BORDER | SWT.FLAT);
		pane.setText("Synchronize Changes"); //$NON-NLS-1$
		ToolBarManager tbm= pane.getToolBarManager();
		//tbm.add(new NextChange());
		//tbm.add(new PreviousChange());
		//tbm.update(true);
		
		fTreeViewer= createTreeViewer(pane);
		//fTreeViewer.setContentProvider(createTreeContentProvider());
		//fTreeViewer.setLabelProvider(createTreeLabelProvider());
		fTreeViewer.addSelectionChangedListener(createSelectionChangedListener());
		//fTreeViewer.addCheckStateListener(createCheckStateListener());
		pane.setContent(fTreeViewer.getControl());
		//setTreeViewerInput();
		
		fPreviewContainer= new PageBook(sashForm, SWT.NONE);
		fNullPreviewer= new NullPreviewer();
		fNullPreviewer.createControl(fPreviewContainer);
		fPreviewContainer.showPage(fNullPreviewer.getControl());
		fCurrentPreviewViewer= fNullPreviewer;
		
		sashForm.setWeights(new int[]{33, 67});
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= convertWidthInCharsToPixels(80);
		sashForm.setLayoutData(gd);
		Dialog.applyDialogFont(result);
		
		fChangePreviewViewer = new TextChangePreviewViewer();
		fChangePreviewViewer.createControl(fPreviewContainer);
		
		return result;
	}

	/**
	 * @param pane
	 * @return
	 */
	private Viewer createTreeViewer(ViewerPane pane) {
		SyncChangesViewer viewer =  new SyncChangesTreeViewer(pane, this.participant, participant.getInput().getFilteredSyncSet());
		viewer.getViewer().setLabelProvider(new TeamSubscriberParticipantLabelProvider());
		return viewer.getViewer(); 
	}
	
	private ISelectionChangedListener createSelectionChangedListener() {
		return new ISelectionChangedListener(){
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection sel= (IStructuredSelection) event.getSelection();
				if (sel.size() == 1) {
					ITeamSubscriberParticipantNode newSelection= (ITeamSubscriberParticipantNode)sel.getFirstElement();
					if (newSelection != fCurrentSelection) {
						fCurrentSelection= newSelection;
						SyncInfo info = fCurrentSelection.getSyncInfo();						
						if(info != null && info.getLocal().getType() == IResource.FILE) {
							SyncInfoDiffNode node = SyncInfoCompareInput.createSyncInfoDiffNode(participant, info);
							fetchContents(node);
							showPreview(node);
						} else {
							showPreview(null);
						}
					}
				} else {
					showPreview(null);
				}
			}
		};
	}

	private void fetchContents(final SyncInfoDiffNode node) {
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						node.cacheContents(monitor);
					} catch (TeamException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
		}
	}
	
	private void showPreview(Object element) {
		if (element == null) {
			showNullPreviewer();
		} else {
			try {
				fCurrentPreviewViewer = fChangePreviewViewer;
				fCurrentPreviewViewer.setInput(element);
			} catch (CoreException e) {
				showNullPreviewer();
			}
			fPreviewContainer.showPage(fCurrentPreviewViewer.getControl());
		}
	}
	
	private void showNullPreviewer() {
		fCurrentPreviewViewer= fNullPreviewer;
		fPreviewContainer.showPage(fCurrentPreviewViewer.getControl());
	}
}
