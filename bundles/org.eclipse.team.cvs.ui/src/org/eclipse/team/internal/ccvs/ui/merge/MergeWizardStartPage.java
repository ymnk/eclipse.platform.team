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


import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.wizards.CVSWizardPage;
import org.eclipse.ui.help.WorkbenchHelp;

public class MergeWizardStartPage extends CVSWizardPage {
    
	private CVSTag result;
    private final TagSource tagSource;
    private TagSelectionArea tagArea;
	
	/**
	 * MergeWizardStartPage constructor.
	 * 
	 * @param pageName  the name of the page
	 * @param title  the title of the page
	 * @param titleImage  the image for the page
	 * @param tagSource
	 */
	public MergeWizardStartPage(String pageName, String title, ImageDescriptor titleImage, TagSource tagSource) {
		super(pageName, title, titleImage);
        this.tagSource = tagSource;
		setDescription(Policy.bind("MergeWizardStartPage.description")); //$NON-NLS-1$
	}
	
	/*
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite composite = createComposite(parent, 1);
		// set F1 help
		WorkbenchHelp.setHelp(composite, IHelpContextIds.MERGE_START_PAGE);
		
		tagArea = new TagSelectionArea(getShell(), tagSource, "&Select start tag:", TagSelectionArea.INCLUDE_VERSIONS, null);
		tagArea.createArea(composite);
		tagArea.addPropertyChangeListener(new IPropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                if (event.getProperty().equals(TagSelectionArea.SELECTED_TAG)) {
                    result = tagArea.getSelection();
                    setPageComplete(result != null);
                } else if (event.getProperty().equals(TagSelectionArea.OPEN_SELECTED_TAG)) {
                    getContainer().showPage(getNextPage());
                }

            }
        });

		setControl(composite);
		Dialog.applyDialogFont(parent);
		setPageComplete(false);
	}
	public CVSTag getTag() {
		return result;
	}
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible && tagArea != null) {
			tagArea.setFocus();
		}
	}
}
