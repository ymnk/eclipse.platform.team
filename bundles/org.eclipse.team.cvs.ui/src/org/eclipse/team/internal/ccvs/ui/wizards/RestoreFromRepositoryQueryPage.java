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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.ui.AdaptableResourceList;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.IHelpContextIds;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.internal.WorkbenchPlugin;

/**
 * This page alows the user to pick the parent folder and specify a depth for
 * the operation
 */
public class RestoreFromRepositoryQueryPage extends CVSWizardPage {

	private TreeViewer tree;
	private Button recurseCheck;

	private IResource[] resources;
	private IResource selection;
	private boolean recurse = false;
	
	/**
	 * Constructor for RestoreFromRepositoryQueryPage.
	 * @param pageName
	 * @param title
	 * @param titleImage
	 * @param description
	 */
	public RestoreFromRepositoryQueryPage(
		String pageName,
		String title,
		ImageDescriptor titleImage,
		String description) {
		super(pageName, title, titleImage, description);
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite composite= createComposite(parent, 1);
		setControl(composite);
		
		WorkbenchHelp.setHelp(composite, IHelpContextIds.RESTORE_FROM_REPOSITORY_QUERY_PAGE_PAGE);
		createWrappingLabel(composite, Policy.bind("RestoreFromRepositoryQueryPage.treeLabel"), 0, 1); //$NON-NLS-1$
		
		tree = createResourceSelectionTree(composite, IResource.PROJECT | IResource.FOLDER, 1 /* horizontal span */);
		tree.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				handleResourceSelection(event);
			}
		});

		createWrappingLabel(composite, "", 0, 1); //$NON-NLS-1$
				
		// Should subfolders of the folder be checked out?
		recurseCheck = createCheckBox(composite, Policy.bind("RestoreFromRepositoryQueryPage.recurse")); //$NON-NLS-1$
		recurseCheck.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				recurse = recurseCheck.getSelection();
				updateWidgetEnablements();
			}
		});
				
		initializeValues();
		updateWidgetEnablements();
		recurseCheck.setFocus();
	}
	
	/**
	 * Method initializeValues.
	 */
	private void initializeValues() {
		recurse = false;
		recurseCheck.setSelection(recurse);
		tree.setInput(new AdaptableResourceList(getProjectSharedWithCVS()));
		tree.setSelection(new StructuredSelection(this.selection), true);
	}
	
	/**
	 * Method getProjectSharedWithCVS.
	 * @return Object
	 */
	private IProject[] getProjectSharedWithCVS() {
		List validTargets = new ArrayList();
		IResource[] projects;
		try {
			projects = WorkbenchPlugin.getPluginWorkspace().getRoot().members();
		} catch (CoreException e) {
			CVSUIPlugin.log(CVSException.wrapException(e));
			setErrorMessage(e.getMessage());
			return new IProject[0];
		}
		for (int i = 0; i < projects.length; i++) {
			IResource resource = projects[i];
			if (resource instanceof IProject) {
				IProject project = (IProject) resource;
				if (project.isAccessible()) {
					RepositoryProvider provider = RepositoryProvider.getProvider(project, CVSProviderPlugin.getTypeId());
					if (provider != null) {
						validTargets.add(project);
					}
				}
			}
		}
		return (IProject[]) validTargets.toArray(new IProject[validTargets.size()]);
	}
	
	/**
	 * Method updateWidgetEnablements.
	 */
	private void updateWidgetEnablements() {
		boolean complete = false;
		setErrorMessage(null);
		if (selection != null) {
			ICVSFolder folder = getSelectedFolder();
			try {
				if (folder.isCVSFolder()) {
					complete = true;
				} else {
					setErrorMessage(Policy.bind("RestoreFromRepositoryQueryPage.selectionNotCVSFolder"));
				}
			} catch (CVSException e) {
				setErrorMessage(e.getStatus().getMessage());
				CVSUIPlugin.log(e);
			}
		}
		setPageComplete(complete);
	}

	private void handleResourceSelection(SelectionChangedEvent event) {
		ISelection selection = event.getSelection();
		if (selection == null || selection.isEmpty()) {
			this.selection = null;
		} else if (selection instanceof IStructuredSelection) {
			this.selection = (IResource)((IStructuredSelection)selection).getFirstElement();
		}
		updateWidgetEnablements();
	}	
	/**
	 * Returns the selection.
	 * @return IResource
	 */
	public IResource getSelection() {
		return selection;
	}

	/**
	 * Sets the selection.
	 * @param selection The selection to set
	 */
	public void setSelection(IResource selection) {
		this.selection = selection;
		if (isControlCreated()) {
			tree.setSelection(new StructuredSelection(this.selection), true);
			updateWidgetEnablements();
		}
	}

	/**
	 * Returns the recurse.
	 * @return boolean
	 */
	public boolean isRecurse() {
		return recurse;
	}
	/**
	 * Method getSelectedFolder.
	 * @return ICVSFolder
	 */
	public ICVSFolder getSelectedFolder() {
		return CVSWorkspaceRoot.getCVSFolderFor(getFolder());
	}
	/**
	 * Method getFolder.
	 * @return IContainer
	 */
	public IContainer getFolder() {
		return (IContainer)getSelection();
	}

}
