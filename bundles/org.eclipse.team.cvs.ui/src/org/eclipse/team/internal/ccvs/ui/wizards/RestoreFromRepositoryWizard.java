/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSStatus;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.ICVSRunnable;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.CommandOutputAdapter;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.client.Command.QuietOption;
import org.eclipse.team.internal.ccvs.core.connection.CVSServerException;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.internal.ccvs.ui.Policy;

/**
 * This wizard allows the user to show deleted resources in the history view
 */
public class RestoreFromRepositoryWizard extends Wizard {

	private RestoreFromRepositoryQueryPage queryPage;
	private RestoreFromRepositoryFileSelectionPage fileSelectionPage;
	private IContainer parent;
	
	/**
	 * Constructor for RestoreFromRepositoryWizard.
	 */
	public RestoreFromRepositoryWizard(IContainer initialParent) {
		this.parent = initialParent;
	}

	/**
	 * @see org.eclipse.jface.wizard.IWizard#performFinish()
	 */
	public boolean performFinish() {
		return false;
	}
	/**
	 * @see org.eclipse.jface.wizard.IWizard#addPages()
	 */
	public void addPages() {
		setNeedsProgressMonitor(true);
		ImageDescriptor substImage = CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_WIZBAN_CHECKOUT);
		
		queryPage = new RestoreFromRepositoryQueryPage("QueryPage", Policy.bind("RestoreFromRepositoryWizard.queryPageTitle"), substImage, Policy.bind("RestoreFromRepositoryWizard.queryPageDescription"));
		queryPage.setSelection(parent);
		addPage(queryPage);
		
		fileSelectionPage = new RestoreFromRepositoryFileSelectionPage("FileSelectionPage", Policy.bind("RestoreFromRepositoryWizard.fileSelectionPageTitle"), substImage, Policy.bind("RestoreFromRepositoryWizard.fileSelectionPageDescription"));
		addPage(fileSelectionPage);
	}

	/**
	 * @see org.eclipse.jface.wizard.IWizard#getNextPage(org.eclipse.jface.wizard.IWizardPage)
	 */
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == queryPage) {
			fileSelectionPage.setRecurse(queryPage.isRecurse());
			fileSelectionPage.setFolder(queryPage.getFolder());
		}
		return super.getNextPage(page);
	}

}
