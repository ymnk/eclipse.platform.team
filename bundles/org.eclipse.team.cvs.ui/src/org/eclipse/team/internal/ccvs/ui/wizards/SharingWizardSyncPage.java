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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.core.subscribers.FilteredSyncInfoCollector;
import org.eclipse.team.core.synchronize.*;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.subscriber.WorkspaceSynchronizeParticipant;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.viewers.SynchronizeCompareInput;
import org.eclipse.team.ui.synchronize.viewers.TreeViewerAdvisor;

/**
 * Page that displays the compare input for sharing
 */
public class SharingWizardSyncPage extends CVSWizardPage {
	
	private SynchronizeCompareInput input;
	private FilteredSyncInfoCollector collector;
	private SyncInfoTree infos;
	private IProject project;
	
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
		
		input = createCompareInput();
		Control c = input.createContents(composite);
		c.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Dialog.applyDialogFont(parent);	
	}
	
	private SynchronizeCompareInput createCompareInput() {
		infos = new SyncInfoTree();
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
				return "Sharing";
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
}
