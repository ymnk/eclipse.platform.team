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
package org.eclipse.team.internal.ccvs.ui.repo;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.IHelpContextIds;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.wizards.CVSWizardPage;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * @author Administrator
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class CVSWorkingSetFolderSelectionPage extends CVSWizardPage {
	
	private String workingSetName;
	private boolean showModules;
	
	private Text nameField;
	private CheckboxTreeViewer tree;
	private Button showFoldersButton;
	private Button showModulesButton;

	/**	 * @see org.eclipse.team.internal.ccvs.ui.wizards.CVSWizardPage#CVSWizardPage(String, String, ImageDescriptor, String)	 */
	public CVSWorkingSetFolderSelectionPage(String pageName, String title, ImageDescriptor titleImage, String description) {
		super(pageName, title, titleImage, description);
	}
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite composite= createComposite(parent, 2);
		setControl(composite);
		
		WorkbenchHelp.setHelp(composite, IHelpContextIds.WORKING_SET_FOLDER_SELECTION_PAGE);
		
		createLabel(composite, Policy.bind("CVSWorkingSetFolderSelectionPage.name")); //$NON-NLS-1$
		nameField = createTextField(composite);
		nameField.addListener(SWT.Modify, new Listener() {
			public void handleEvent(Event event) {
				workingSetName = nameField.getText();
				updateWidgetEnablements();
			}
		});
		
		createWrappingLabel(composite, Policy.bind("CVSWorkingSetFolderSelectionPage.treeLabel"), 0, 2); //$NON-NLS-1$
		
		tree = createFolderSelectionTree(composite, 2);

		// Add radio buttons to toggle show folders/show modules
		Composite filterComposite = createComposite(composite, 2);
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		data.horizontalSpan = 1;
		filterComposite.setLayoutData(data);
		showFoldersButton = createRadioButton(filterComposite, Policy.bind("CVSWorkingSetFolderSelectionPage.showFolders"), 1); //$NON-NLS-1$
		showModulesButton = createRadioButton(filterComposite, Policy.bind("CVSWorkingSetFolderSelectionPage.showModules"), 1); //$NON-NLS-1$
		showModulesButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				showModules = showModulesButton.getSelection();
				CVSWorkingSetFolderSelectionPage.this.refreshTree();
			}
		});
		
		initializeValues();
		updateWidgetEnablements();
		tree.getControl().setFocus();
	}
	/**
	 * Method refreshTree.
	 */
	public void refreshTree() {
		tree.refresh();
	}
	
	/**
	 * Method initializeValues.
	 */
	private void initializeValues() {
		showModules = false;
		workingSetName = "";
		showModulesButton.setSelection(showModules);
	}

	/**
	 * Method updateWidgetEnablement.
	 */
	private void updateWidgetEnablements() {
		// Make sure the name is valid
		if (isValidName(workingSetName)) {
			setPageComplete(false);
			setErrorMessage(Policy.bind("CVSWorkingSetFolderSelectionPage.invalidWorkingSetName", workingSetName)); //$NON-NLS-1$
		}
		// The page is complete when a least one folder is checked
		boolean complete = false;
		setPageComplete(complete);
	}
	
	/**
	 * Method isValidName.
	 * @param workingSetName
	 * @return boolean
	 */
	private boolean isValidName(String workingSetName) {
		if (workingSetName.length() == 0)
			return false;
		for (int i = 0; i < workingSetName.length(); i++) {
			char c = workingSetName.charAt(i);
			if (! Character.isLetterOrDigit(c))
				return false;
		}
		return true;
	}
	
	protected CheckboxTreeViewer createFolderSelectionTree(Composite composite, int span) {
		CheckboxTreeViewer tree = new CheckboxTreeViewer(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		tree.setUseHashlookup(true);
		tree.setContentProvider(new WorkbenchContentProvider() {
			public Object[] getChildren(Object parentElement) {
				if (parentElement instanceof ICVSRepositoryLocation) {
					return CVSWorkingSetFolderSelectionPage.this.getChildren((ICVSRepositoryLocation)parentElement);
				}
				return null;
			}
			public Object getParent(Object element) {
				if (element instanceof ICVSRemoteFolder) {
					return ((ICVSRemoteFolder)element).getRepository();
				}
				return null;
			}
		});
		tree.setLabelProvider(new WorkbenchLabelProvider());
		tree.setSorter(new RepositorySorter());
		
		GridData data = new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL);
		data.heightHint = LIST_HEIGHT_HINT;
		data.horizontalSpan = span;
		tree.getControl().setLayoutData(data);
		return tree;
	}
	
	/**
	 * Method getChildren.
	 * @param iCVSRepositoryLocation
	 * @return Object[]
	 */
	public ICVSRemoteResource[] getChildren(final ICVSRepositoryLocation location) {
		final ICVSRemoteResource[][] result = new ICVSRemoteResource[1][0];
		result[0] = null;
		try {
			getContainer().run(true /* fork */, true /* cancelable */, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						result[0] = location.members(CVSTag.DEFAULT, isShowModules(), monitor);
					} catch (CVSException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InvocationTargetException e) {
			CVSUIPlugin.openError(getContainer().getShell(), null, null, e, CVSUIPlugin.PERFORM_SYNC_EXEC);
		} catch (InterruptedException e) {
		}
		return result[0];
	}
	
	private boolean isShowModules() {
		return false;
	}
}
