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
package org.eclipse.team.internal.ccvs.ui.merge;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.wizards.CVSWizardPage;
import org.eclipse.ui.help.WorkbenchHelp;

public class MergeWizardEndPage extends CVSWizardPage {
	IProject project;
	TreeViewer tree;
	CVSTag result;
	ICVSRemoteFolder remote;
	// for accessing the start tag
	MergeWizardStartPage startPage;
	
	TagSelectionArea tagArea;
	
	/**
	 * MergeWizardEndPage constructor.
	 * 
	 * @param pageName  the name of the page
	 * @param title  the title of the page
	 * @param titleImage  the image for the page
	 */
	public MergeWizardEndPage(String pageName, String title, ImageDescriptor titleImage, MergeWizardStartPage startPage) {
		super(pageName, title, titleImage);
		setDescription(Policy.bind("MergeWizardEndPage.description")); //$NON-NLS-1$
		this.startPage = startPage;
	}
	/*
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite composite = createComposite(parent, 2);
		// set F1 help
		WorkbenchHelp.setHelp(composite, IHelpContextIds.MERGE_END_PAGE);
		
		tagArea = new TagSelectionArea(getShell(), new SingleFolderTagSource(CVSWorkspaceRoot.getCVSFolderFor(project)), "&Select a tag", TagSelectionArea.INCLUDE_ALL_TAGS, null);
		tagArea.createArea(composite);
		tagArea.addPropertyChangeListener(new IPropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                if (event.getProperty().equals(TagSelectionArea.SELECTED_TAG)) {
                    CVSTag selected = tagArea.getSelection();
    				if (selected != null) {
    					result = selected;
    					if(!result.equals(startPage.getTag())) {
    						setPageComplete(true);
    						setMessage(null);
    					} else {
    						setMessage(Policy.bind("MergeWizardEndPage.duplicateTagSelected", result.getName()), WARNING); //$NON-NLS-1$
    						setPageComplete(false);
    					}
    				} else {
    					setMessage(null);
    					result = null;
    					setPageComplete(false);
    				}
                }

            }
        });

		setControl(composite);
		setPageComplete(false);
	}
	public void setProject(IProject project) {
		this.project = project;
		try {
			this.remote = (ICVSRemoteFolder) CVSWorkspaceRoot.getRemoteResourceFor(project);
		} catch (TeamException e) {
			// To do
		}
	}
	public CVSTag getTag() {
		return result;
	}
	/**
	 * @see IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		// refresh the tree because tags may have been added in the previous page
		tree.refresh();
		if (visible) {
			tree.getControl().setFocus();
		}
	}
}
