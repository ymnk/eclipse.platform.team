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

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.core.ITeamStatus;
import org.eclipse.team.core.subscribers.FilteredSyncInfoCollector;
import org.eclipse.team.core.synchronize.ISyncInfoSetChangeEvent;
import org.eclipse.team.core.synchronize.ISyncInfoSetChangeListener;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.core.synchronize.SyncInfoTree;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.subscriber.WorkspaceSynchronizeParticipant;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.SynchronizeCompareInput;
import org.eclipse.team.ui.synchronize.TreeViewerAdvisor;
import org.eclipse.ui.part.PageBook;

/**
 * Page that displays the compare input for sharing
 */
public class SharingWizardSyncPage extends CVSWizardPage implements ISyncInfoSetChangeListener {
	
	private SynchronizeCompareInput input;
	private FilteredSyncInfoCollector collector;
	private SyncInfoTree infos;
	private IProject project;
	
	PageBook pageBook;
	private Control syncPage;
	private Control noChangesPage;
	private Control errorPage;
	
	public SharingWizardSyncPage(String pageName, String title, ImageDescriptor titleImage, String description) {
		super(pageName, title, titleImage, description);
	}

	public void setProject(IProject project) {
		this.project = project;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite composite = createComposite(parent, 1);
		setControl(composite);
		
		// set F1 help
		//WorkbenchHelp.setHelp(composite, IHelpContextIds.SHARE_WITH_EXISTING_TAG_SELETION_DIALOG);
		
		pageBook = new PageBook(composite, SWT.NONE);
		
		input = createCompareInput();
		syncPage = input.createContents(pageBook);
		syncPage.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		noChangesPage = createNoChangesPage(pageBook);
		noChangesPage.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		errorPage = createErrorPage(pageBook);
		errorPage.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		updatePage();
		
		Dialog.applyDialogFont(parent);	
	}
	
	private Control createNoChangesPage(PageBook pageBook) {
		Composite composite = createComposite(pageBook, 1);
		createWrappingLabel(composite, "The resources of project {0} are in-sync with the repository." + project.getName(), 0);
		return composite;
	}
	
	private Control createErrorPage(PageBook pageBook) {
		Composite composite = createComposite(pageBook, 1);
		createWrappingLabel(composite, "An error has occurred populating this view.", 0);
		Button showErrors = new Button(composite, SWT.PUSH);
		showErrors.setText("Show Errors");
		showErrors.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				showErrors();
			}
		});
		return composite;
	}

	/* private */ void showErrors() {
		ITeamStatus[] status = infos.getErrors();
		String title = Policy.bind("Errors Occurred"); //$NON-NLS-1$
		if (status.length == 1) {
			ErrorDialog.openError(getShell(), title, status[0].getMessage(), status[0]);
		} else {
			MultiStatus multi = new MultiStatus(CVSUIPlugin.ID, 0, status, "The following errors occurred.", null); //$NON-NLS-1$
			ErrorDialog.openError(getShell(), title, null, multi);
		}
	}
	
	private SynchronizeCompareInput createCompareInput() {
		infos = new SyncInfoTree();
		infos.addSyncSetChangedListener(this);
		WorkspaceSynchronizeParticipant participant = CVSUIPlugin.getPlugin().getCvsWorkspaceSynchronizeParticipant();
		collector = new FilteredSyncInfoCollector(participant.getSubscriberSyncInfoCollector().getSubscriberSyncInfoSet(), infos, new SyncInfoFilter() {
			public boolean select(SyncInfo info, IProgressMonitor monitor) {
				if (project == null)return false;
				return project.getFullPath().isPrefixOf(info.getLocal().getFullPath());
			}
		});
		collector.start(new NullProgressMonitor());
		TreeViewerAdvisor advisor = new TreeViewerAdvisor(participant.getId(), null, infos);
		CompareConfiguration cc = new CompareConfiguration();
		SynchronizeCompareInput input = new SynchronizeCompareInput(cc, advisor) {
			public String getTitle() {
				return Policy.bind("SharingWizardSyncPage.0"); //$NON-NLS-1$
			}
		};
		try {
			// model will be built in the background since we know the compare input was 
			// created with a subscriber participant
			input.run(new NullProgressMonitor());
		} catch (InterruptedException e) {
			Utils.handle(e);
		} catch (InvocationTargetException e) {
			Utils.handle(e);
		}
		return input;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#dispose()
	 */
	public void dispose() {
		collector.dispose();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.WizardPage#setPreviousPage(org.eclipse.jface.wizard.IWizardPage)
	 */
	public void setPreviousPage(IWizardPage page) {
		// There's no going back from this page
		super.setPreviousPage(null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.synchronize.ISyncInfoSetChangeListener#syncInfoSetReset(org.eclipse.team.core.synchronize.SyncInfoSet, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void syncInfoSetReset(SyncInfoSet set, IProgressMonitor monitor) {
		updatePage();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.synchronize.ISyncInfoSetChangeListener#syncInfoChanged(org.eclipse.team.core.synchronize.ISyncInfoSetChangeEvent, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void syncInfoChanged(ISyncInfoSetChangeEvent event, IProgressMonitor monitor) {
		updatePage();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.synchronize.ISyncInfoSetChangeListener#syncInfoSetErrors(org.eclipse.team.core.synchronize.SyncInfoSet, org.eclipse.team.core.ITeamStatus[], org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void syncInfoSetErrors(SyncInfoSet set, ITeamStatus[] errors, IProgressMonitor monitor) {
		updatePage();
	}

	private void updatePage() {
		if (infos.getErrors().length > 0) {
			pageBook.showPage(errorPage);
		} else if (infos.isEmpty()) {
			pageBook.showPage(noChangesPage);
		} else {
			pageBook.showPage(syncPage);
		}
	}
}
