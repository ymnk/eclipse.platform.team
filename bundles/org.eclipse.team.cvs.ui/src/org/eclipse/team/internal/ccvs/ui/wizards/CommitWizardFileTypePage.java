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

import java.util.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.core.Team;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Command.KSubstOption;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ui.SWTUtils;
import org.eclipse.team.internal.ui.preferences.FileTypeTable;

/**                
 * 
 */
public class CommitWizardFileTypePage extends WizardPage implements FileTypeTable.PixelConverter {

    private final Collection fExtensions;
    private final Collection fNames;
    
    public CommitWizardFileTypePage(Collection unknownExtensions, Collection unknownNames) {
        super(Policy.bind("CommitWizardFileTypePage.0")); //$NON-NLS-1$
        setTitle("Add Resources"); //$NON-NLS-1$
        setDescription(Policy.bind("CommitWizardFileTypePage.2")); //$NON-NLS-1$
        
        fNames= new ArrayList();
        for (final Iterator iter = unknownNames.iterator(); iter.hasNext();) {
            final String name = (String) iter.next();
            fNames.add(new FileTypeTable.Name(name));
        }
        
        fExtensions= new ArrayList();
        for (final Iterator iter = unknownExtensions.iterator(); iter.hasNext();) {
            final String extension= (String)iter.next();
            fExtensions.add(new FileTypeTable.Extension(extension));
        }
    }
    
    public void createControl(Composite parent) {
        
        initializeDialogUnits(parent);
        
        final Composite composite= new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        
	    final Label label= new Label(composite, SWT.WRAP);
	    label.setText(Policy.bind("CommitWizardFileTypePage.3")); //$NON-NLS-1$
	    label.setLayoutData(SWTUtils.createHFillGridData());
        
        final List items= new ArrayList();
        items.addAll(fNames);
        items.addAll(fExtensions);
        new FileTypeTable(composite, this, items, true);

        setControl(composite);
    }

	public void getModesForExtensions(Map toSave, Map notToSave) {
	    getModes(fExtensions, toSave, notToSave);
	}

	public void getModesForNames(Map toSave, Map notToSave) {
	    getModes(fNames, toSave, notToSave);
	}

    private static void getModes(Collection items, Map toSave, Map notToSave) {
	    for (final Iterator iter = items.iterator(); iter.hasNext();) {
	        final FileTypeTable.Item item= (FileTypeTable.Item)iter.next();
	        final Map destination= item.save ? toSave : notToSave;
	        destination.put(item.name, new Integer(item.mode));
	    }
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.preferences.FileTypeTable.PixelConverter#convertWidthInCharsToPixels(int)
     */
    public int convertWidthInCharsToPixels(int chars) {
        return super.convertWidthInCharsToPixels(chars);
    }
    
    public KSubstOption getOption(IFile file) throws CVSException {
        final String extension= file.getFileExtension();
        if (extension != null) {
            for (Iterator iter = fExtensions.iterator(); iter.hasNext();) {
                final FileTypeTable.Item element = (FileTypeTable.Item) iter.next();
                if (element.name.equals(extension)) {
                    return element.mode == Team.TEXT ? KSubstOption.getDefaultTextMode() : Command.KSUBST_BINARY;
                }
            }
        } else {
            final String name= file.getName();
            for (Iterator iter = fNames.iterator(); iter.hasNext();) {
                FileTypeTable.Item item = (FileTypeTable.Item) iter.next();
                if (item.name.equals(name)) {
                    return item.mode == Team.TEXT ? KSubstOption.getDefaultTextMode() : Command.KSUBST_BINARY;
                }
            }
        }
        final ICVSFile cvsFile = CVSWorkspaceRoot.getCVSFileFor(file);
		ResourceSyncInfo fileInfo = cvsFile.getSyncInfo();
		return fileInfo != null ? fileInfo.getKeywordMode() : KSubstOption.fromFile(file);
    }
}
