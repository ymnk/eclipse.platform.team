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
package org.eclipse.team.internal.ccvs.ui;

 
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.ui.merge.ProjectElement;

/**
 * Dialog to prompt the user to choose a tag for a selected resource
 */
public class TagSelectionDialog extends Dialog implements IPropertyChangeListener {
	
	private TagSelectionArea tagSelectionArea;
	
	public static final int INCLUDE_HEAD_TAG = ProjectElement.INCLUDE_HEAD_TAG;
	public static final int INCLUDE_BASE_TAG = ProjectElement.INCLUDE_BASE_TAG;
	public static final int INCLUDE_BRANCHES = ProjectElement.INCLUDE_BRANCHES;
	public static final int INCLUDE_VERSIONS = ProjectElement.INCLUDE_VERSIONS;
	public static final int INCLUDE_DATES = ProjectElement.INCLUDE_DATES;
	public static final int INCLUDE_ALL_TAGS = ProjectElement.INCLUDE_ALL_TAGS;
	
	private Button okButton;
	
	// dialog title, should indicate the action in which the tag selection
	// dialog is being shown
	private String title;
	
	private boolean recurse = true;
	
	// constants
	private static final int SIZING_DIALOG_WIDTH = 400;
	private static final int SIZING_DIALOG_HEIGHT = 250;

    private CVSTag selection;
	
	public static CVSTag getTagToCompareWith(Shell shell, IProject[] projects) {
		return getTagToCompareWith(shell, getCVSFoldersFor(projects));
	}
		
	public static CVSTag getTagToCompareWith(Shell shell, ICVSFolder[] folders) {
		TagSelectionDialog dialog = new TagSelectionDialog(shell, folders, 
			Policy.bind("CompareWithTagAction.message"),  //$NON-NLS-1$
			Policy.bind("TagSelectionDialog.Select_a_Tag_1"), //$NON-NLS-1$
			TagSelectionDialog.INCLUDE_ALL_TAGS, 
			false, /* show recurse*/
			IHelpContextIds.COMPARE_TAG_SELECTION_DIALOG);
		dialog.setBlockOnOpen(true);
		int result = dialog.open();
		if (result == Dialog.CANCEL) {
			return null;
		}
		return dialog.getResult();
	}
	/**
	 * Creates a new TagSelectionDialog.
	 * @param resource The resource to select a version for.
	 */
	public TagSelectionDialog(Shell parentShell, IProject[] projects, String title, String message, int includeFlags, boolean showRecurse, String helpContext) {
		this(parentShell, getCVSFoldersFor(projects), title, message, includeFlags, showRecurse, helpContext); //$NON-NLS-1$		
	}
	
	private static ICVSFolder[] getCVSFoldersFor(IProject[] projects) {
		ICVSFolder[] folders = new ICVSFolder[projects.length];
		for (int i = 0; i < projects.length; i++) {
			folders[i] = CVSWorkspaceRoot.getCVSFolderFor(projects[i]);
		}
		return folders;
	}
	
	/**
	 * Creates a new TagSelectionDialog.
	 * @param resource The resource to select a version for.
	 */
	public TagSelectionDialog(Shell parentShell, ICVSFolder[] folders, String title, String message, int includeFlags, final boolean showRecurse, String helpContext) {
		super(parentShell);
		
		// Create a tag selection area with a custom recurse option
		tagSelectionArea = new TagSelectionArea(this, null, folders, message, includeFlags, helpContext) {
			protected void createCustomArea(Composite parent) {
				if(showRecurse) {
					final Button recurseCheck = new Button(parent, SWT.CHECK);
					recurseCheck.setText(Policy.bind("TagSelectionDialog.recurseOption")); //$NON-NLS-1$
					recurseCheck.addListener(SWT.Selection, new Listener() {
						public void handleEvent(Event event) {
							recurse = recurseCheck.getSelection();
						}
					});
					recurseCheck.setSelection(true);
				}
		    }
		};
		tagSelectionArea.addPropertyChangeListener(this);
		this.title = title;
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}
	
	/* (non-Javadoc)
	 * Method declared on Window.
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(title);
	}
	
	/**
	 * Creates this window's widgetry.
	 * <p>
	 * The default implementation of this framework method
	 * creates this window's shell (by calling <code>createShell</code>),
	 * its control (by calling <code>createContents</code>),
	 * and initializes this window's shell bounds 
	 * (by calling <code>initializeBounds</code>).
	 * This framework method may be overridden; however,
	 * <code>super.create</code> must be called.
	 * </p>
	 */
	public void create() {
		super.create();
		initialize();
	}
	
	/**
	 * Add buttons to the dialog's button bar.
	 *
	 * @param parent the button bar composite
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		okButton.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	/**
	 * Creates and returns the contents of the upper part 
	 * of this dialog (above the button bar).
	 * <p>
	 * The default implementation of this framework method
	 * creates and returns a new <code>Composite</code> with
	 * standard margins and spacing.
	 * Subclasses should override.
	 * </p>
	 *
	 * @param the parent composite to contain the dialog area
	 * @return the dialog area control
	 */
	protected Control createDialogArea(Composite parent) {
		Composite top = (Composite)super.createDialogArea(parent);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.widthHint = SIZING_DIALOG_WIDTH;
		data.heightHint = SIZING_DIALOG_HEIGHT;
		top.setLayoutData(data);
		
		// Delegate most of the dialog to the tag selection area
		tagSelectionArea.createArea(top);
		
		// Create a separator between the tag area and the button area
		Label seperator = new Label(top, SWT.SEPARATOR | SWT.HORIZONTAL);
		data = new GridData (GridData.FILL_BOTH);		
		data.horizontalSpan = 2;
		seperator.setLayoutData(data);
		
		updateEnablement();
        Dialog.applyDialogFont(parent);
        
		return top;
	}
	
	
	/**
	 * Utility method that creates a label instance
	 * and sets the default layout data.
	 *
	 * @param parent  the parent for the new label
	 * @param text  the text for the new label
	 * @return the new label
	 */
	protected Label createLabel(Composite parent, String text) {
		Label label = new Label(parent, SWT.LEFT);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalSpan = 1;
		data.horizontalAlignment = GridData.FILL;
		label.setLayoutData(data);
		return label;
	}
	
	/**
	 * Returns the selected tag.
	 */
	public CVSTag getResult() {
		return selection;
	}
	
	public boolean getRecursive() {
		return recurse;
	}

	/**
	 * Initializes the dialog contents.
	 */
	protected void initialize() {
		okButton.setEnabled(false);
	}

	
	/**
	 * Updates the dialog enablement.
	 */
	protected void updateEnablement() {
		if(okButton!=null) {
			okButton.setEnabled(selection != null);
		}
	}

    /* (non-Javadoc)
     * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent event) {
        String property = event.getProperty();
        if (property.equals(TagSelectionArea.SELECTED_TAG)) {
            selection = (CVSTag)event.getNewValue();
            updateEnablement();
        } else if (property.equals(TagSelectionArea.OPEN_SELECTED_TAG)) {
            okPressed();
        }
    }
}
