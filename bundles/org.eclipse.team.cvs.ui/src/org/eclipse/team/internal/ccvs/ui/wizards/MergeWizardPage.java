/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.wizards;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.IHelpContextIds;
import org.eclipse.team.internal.ccvs.ui.tags.*;

public class MergeWizardPage extends CVSWizardPage {

    private Text endTagField;
    private Button endTagBrowseButton;
    private TagSource tagSource;
    private Text startTagField;
    private Button startTagBrowseButton;
    private TagRefreshButtonArea tagRefreshArea;
    private CVSTag startTag;
    private CVSTag endTag;

    public MergeWizardPage(String pageName, String title, ImageDescriptor titleImage, String description, TagSource tagSource) {
        super(pageName, title, titleImage, description);
        this.tagSource = tagSource;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent) {
        Composite composite = createComposite(parent, 1, true);
        
        Composite mainArea = createComposite(composite, 2, true);
        createEndTagArea(mainArea);
        createStartTagArea(mainArea);
        createTagRefreshArea(composite);

        Dialog.applyDialogFont(composite);
        setControl(composite);
    }

    private void createTagRefreshArea(Composite parent) {
	    tagRefreshArea = new TagRefreshButtonArea(getShell(), getTagSource());
	    tagRefreshArea.setRunnableContext(getContainer());
	    tagRefreshArea.createArea(parent); 
    }

    private void createEndTagArea(Composite parent) {
        createWrappingLabel(parent, "Enter the branch or version being merged (end tag):", 0, 2);
        endTagField = createTextField(parent);
        endTagField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                updateEndTag(endTagField.getText());
            }
        });
        TagContentAssistProcessor.createContentAssistant(endTagField, tagSource, TagSelectionArea.INCLUDE_ALL_TAGS);
        endTagBrowseButton = createPushButton(parent, "Browse...");
        endTagBrowseButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                TagSelectionDialog dialog = new TagSelectionDialog(getShell(), getTagSource(), 
                        "Choose End Tag",
                        "&Select end tag",
                        TagSelectionDialog.INCLUDE_ALL_TAGS,
                        false, IHelpContextIds.MERGE_END_PAGE);
                if (dialog.open() == Dialog.OK) {
                    CVSTag selectedTag = dialog.getResult();
                    setEndTag(selectedTag);
                }
            }
        });
    }

    private void createStartTagArea(Composite parent) {
        createWrappingLabel(parent, "Enter the version that is the common base (start tag):", 0, 2);
        startTagField = createTextField(parent);
        startTagField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                updateStartTag(startTagField.getText());
            }
        });
        TagContentAssistProcessor.createContentAssistant(startTagField, tagSource, TagSelectionArea.INCLUDE_VERSIONS | TagSelectionArea.INCLUDE_DATES);
        startTagBrowseButton = createPushButton(parent, "Browse...");
        startTagBrowseButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                TagSelectionDialog dialog = new TagSelectionDialog(getShell(), getTagSource(), 
                        "Choose Start Tag",
                        "&Select start tag",
                        TagSelectionDialog.INCLUDE_VERSIONS,
                        false, IHelpContextIds.MERGE_START_PAGE);
                if (dialog.open() == Dialog.OK) {
                    CVSTag selectedTag = dialog.getResult();
                    setStartTag(selectedTag);
                }
            }
        });   
    }

    protected void updateEndTag(String text) {
        if (endTag == null || !endTag.getName().equals(text)) {
            CVSTag tag = getTagFor(text, false);
            if (tag != null) {
                endTag = tag;
            }
        }
        updateEnablements();
    }
    
    protected void updateStartTag(String text) {
        if (startTag == null || !startTag.getName().equals(text)) {
            CVSTag tag = getTagFor(text, true);
            if (tag != null) {
                startTag = tag;
            }
        }
        updateEnablements();
    }

    private CVSTag getTagFor(String text, boolean versionsOnly) {
        if (text.equals(CVSTag.DEFAULT.getName())) {
            if (versionsOnly) return null;
            return CVSTag.DEFAULT;
        }
        if (text.equals(CVSTag.BASE.getName())) {
            if (versionsOnly) return null;
            return CVSTag.BASE;
        }
        CVSTag[] tags;
        if (versionsOnly) {
            tags = tagSource.getTags(new int[] { CVSTag.VERSION, CVSTag.DATE });
        } else {
            tags = tagSource.getTags(new int[] { CVSTag.VERSION, CVSTag.BRANCH, CVSTag.DATE });
        }
        for (int i = 0; i < tags.length; i++) {
            CVSTag tag = tags[i];
            if (tag.getName().equals(text)) {
                return tag;
            }
        }
        return null;
    }

    protected void setEndTag(CVSTag selectedTag) {
        if (selectedTag == null || endTag == null || !endTag.equals(selectedTag)) {
	        endTag = selectedTag;
	        if (endTagField != null) {
	            endTagField.setText(endTag.getName());
	            if (startTag == null && endTag.getType() == CVSTag.BRANCH) {
	                CVSTag tag = findCommonBaseTag(endTag);
	                if (tag != null) {
	                    setStartTag(tag);
	                }
	            }
	        }
	        updateEnablements();           
        }
    }

    protected void setStartTag(CVSTag selectedTag) {
        if (selectedTag == null || startTag != null || !endTag.equals(selectedTag)) {
	        startTag = selectedTag;
	        if (startTagField != null) {
	            startTagField.setText(startTag.getName());
	        }
	        updateEnablements();
        }
    }
    
    private CVSTag findCommonBaseTag(CVSTag tag) {
        CVSTag[] tags = tagSource.getTags(CVSTag.VERSION);
        for (int i = 0; i < tags.length; i++) {
            CVSTag potentialMatch = tags[i];
            if (potentialMatch.getName().indexOf(tag.getName()) != -1) {
                return potentialMatch;
            }
        }
        return null;
    }

    private void updateEnablements() {
        if (endTag == null && endTagField.getText().length() > 0) {
            setErrorMessage("The specified end tag is not known to exist. Either enter a different tag or refresh the known tags.");
        } else if (startTag == null && startTagField.getText().length() > 0) {
            setErrorMessage("The specified start tag is not known to exist. Either enter a different tag or refresh the known tags.");
        } else if (endTag != null && startTag != null && startTag.equals(endTag)) {
            setErrorMessage("The start and end tags are the same.");
        } else {
            setErrorMessage(null);
        }
        setPageComplete(startTag != null && endTag!= null && !startTag.equals(endTag));
    }

    protected TagSource getTagSource() {
         return tagSource;
    }

    private Button createPushButton(Composite parent, String label) {
        Button b = new Button(parent, SWT.PUSH);
        b.setText(label);
        b.setLayoutData(new GridData(GridData.END | GridData.HORIZONTAL_ALIGN_FILL));
        return b;
    }

    public CVSTag getStartTag() {
        return startTag;
    }
    
    public CVSTag getEndTag() {
        return endTag;
    }
}
