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
package org.eclipse.team.internal.ccvs.ui.actions;

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.internal.ui.dialogs.DetailsDialog;

/**
 * Dialog that will display any mappings that contain resources whose 
 * sync state match the provided filter.
 */
public class MappingSelectionDialog extends DetailsDialog {

    private final ResourceMapping[] mappings;
    private final FastSyncInfoFilter resourceFilter;

    public MappingSelectionDialog(Shell parentShell, String dialogTitle, ResourceMapping[] mappings, FastSyncInfoFilter resourceFilter) {
        super(parentShell, dialogTitle);
        this.mappings = mappings;
        this.resourceFilter = resourceFilter;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.dialogs.DetailsDialog#createMainDialogArea(org.eclipse.swt.widgets.Composite)
     */
    protected void createMainDialogArea(Composite parent) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.dialogs.DetailsDialog#createDropDownDialogArea(org.eclipse.swt.widgets.Composite)
     */
    protected Composite createDropDownDialogArea(Composite parent) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.dialogs.DetailsDialog#updateEnablements()
     */
    protected void updateEnablements() {
        // TODO Auto-generated method stub

    }

    /**
     * @return
     */
    public ResourceMapping[] promtpToSelectMappings() {
        if (hasMatchingMappings()) {
            int code = open();
            if (code == OK) {
                // TODO: 
                return;
            }
        }
        return new ResourceMapping[0];
    }

}
