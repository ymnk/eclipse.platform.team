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
package org.eclipse.team.internal.ccvs.ui.tags;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.util.Assert;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ui.dialogs.DialogArea;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * An area that displays the Refresh and Configure Tags buttons
 */
public class TagRefreshButtonArea extends DialogArea {
    
    private TagSource tagSource;
    private final Shell shell;
    private Button refreshButton;

    public TagRefreshButtonArea(Shell shell, TagSource tagSource) {
        Assert.isNotNull(shell);
        Assert.isNotNull(tagSource);
        this.shell = shell;
        this.tagSource = tagSource;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.dialogs.DialogArea#createArea(org.eclipse.swt.widgets.Composite)
     */
    public void createArea(Composite parent) {
	 	Composite buttonComp = new Composite(parent, SWT.NONE);
		GridData data = new GridData ();
		data.horizontalAlignment = GridData.END;		
		buttonComp.setLayoutData(data);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		buttonComp.setLayout (layout);
	 	
	 	refreshButton = createTagRefreshButton(buttonComp, Policy.bind("TagConfigurationDialog.20")); //$NON-NLS-1$
		data = new GridData();
//		if(hHint!=0 && wHint!=0) {
//			data.heightHint = hHint;
//			//don't crop labels with large font
//			//int widthHint = wHint;
//			//data.widthHint = Math.max(widthHint, refreshButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
//		}
		data.horizontalAlignment = GridData.END;
		data.horizontalSpan = 1;
		refreshButton.setLayoutData (data);		

		Button addButton = new Button(buttonComp, SWT.PUSH);
		addButton.setText (Policy.bind("TagConfigurationDialog.21")); //$NON-NLS-1$
		data = new GridData ();
//		if(hHint!=0 && wHint!=0) {
//			data.heightHint = hHint;
//			//don't crop labels with large font
//			//int widthHint = wHint;
//			//data.widthHint = Math.max(widthHint, addButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
//		}
		data.horizontalAlignment = GridData.END;
		data.horizontalSpan = 1;
		addButton.setLayoutData (data);
		addButton.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					TagConfigurationDialog d = new TagConfigurationDialog(shell, tagSource);
					d.open();
					updateEnablementOnRefreshButton(refreshButton, tagSource);
				}
			});		
		
		WorkbenchHelp.setHelp(refreshButton, IHelpContextIds.TAG_CONFIGURATION_REFRESHACTION);
		WorkbenchHelp.setHelp(addButton, IHelpContextIds.TAG_CONFIGURATION_OVERVIEW);		
		Dialog.applyDialogFont(buttonComp);
    }

	 
	 private static void updateEnablementOnRefreshButton(Button refreshButton, TagSource tagSource) {
	 	try {
			ICVSFolder folder = getSingleFolder(tagSource);
			if (folder != null) {
	            String[] files = CVSUIPlugin.getPlugin().getRepositoryManager().getAutoRefreshFiles(folder);
				refreshButton.setEnabled(files.length != 0);
			} else {
			    refreshButton.setEnabled(false);
			}
		} catch (CVSException e) {
			refreshButton.setEnabled(false);
			CVSUIPlugin.log(e);
		}
	 }
	
	/*
	 * Returns a button that implements the standard refresh tags operation. The runnable is run immediatly after 
	 * the tags are fetched from the server. A client should refresh their widgets that show tags because they
	 * may of changed. 
	 */
	private Button createTagRefreshButton(Composite composite, String title) {
		Button refreshButton = new Button(composite, SWT.PUSH);
		refreshButton.setText (title);
		refreshButton.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					try {
						PlatformUI.getWorkbench().getProgressService().run(true, true, new IRunnableWithProgress() {
							public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
								try {
								    tagSource.refresh(monitor);
								} catch (TeamException e) {
									throw new InvocationTargetException(e);
								}
							}
						});
					} catch (InterruptedException e) {
						// operation cancelled
					} catch (InvocationTargetException e) {
						CVSUIPlugin.openError(shell, Policy.bind("TagConfigurationDialog.14"), null, e); //$NON-NLS-1$
					}
				}
			});
		updateEnablementOnRefreshButton(refreshButton, tagSource);
		return refreshButton;		
	 }
		
	protected static ICVSFolder getSingleFolder(TagSource tagSource) {
	    if (tagSource instanceof SingleFolderTagSource)
	        return ((SingleFolderTagSource)tagSource).getFolder();
	    return null;
	}
    public TagSource getTagSource() {
        return tagSource;
    }
    public void setTagSource(TagSource tagSource) {
        Assert.isNotNull(tagSource);
        this.tagSource = tagSource;
        if (refreshButton != null && !refreshButton.isDisposed())
            updateEnablementOnRefreshButton(refreshButton, tagSource);
    }
}
