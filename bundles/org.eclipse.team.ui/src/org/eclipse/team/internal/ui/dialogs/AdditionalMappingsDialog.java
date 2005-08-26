/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.dialogs;

import org.eclipse.core.internal.resources.mapping.ResourceMapping;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

public class AdditionalMappingsDialog extends DetailsDialog {

    private final ResourceMapping[] mappings;
    private final ResourceMapping[] selectedMappings;
    private ResourceMappingSelectionArea selectedMappingsArea;
    private ResourceMappingHierarchyArea allMappingsArea;

    public AdditionalMappingsDialog(Shell parentShell, String dialogTitle, ResourceMapping[] selectedMappings, ResourceMapping[] mappings) {
        super(parentShell, dialogTitle);
        this.selectedMappings = selectedMappings;
        this.mappings = mappings;
    }

    protected void createMainDialogArea(Composite parent) {
        createWrappingLabel(parent, "This dialog shows all the selected elements and all the elements that will be operated on by this operation due to the files in which the elements are stored.");
        createSelectedMappingsArea(parent);
        createAllMappingsArea(parent);
    }
    
    /*
     * Create a list that allows the selection of mappings via checkbox
     */
    private void createSelectedMappingsArea(Composite parent) {
        Composite composite = createComposite(parent);
        selectedMappingsArea = new ResourceMappingSelectionArea(selectedMappings, false, false);
        selectedMappingsArea.setDescription("Selected Elements");
        //selectedMappingsArea.addPropertyChangeListener(this);
        selectedMappingsArea.createArea(composite);
        // Create a separator between the two sets of buttons
        Label seperator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
        seperator.setLayoutData(new GridData (GridData.FILL_HORIZONTAL));
    }
    
    /*
     * Create a list that allows the selection of mappings via checkbox
     */
    private void createAllMappingsArea(Composite parent) {
        Composite composite = createComposite(parent);
        allMappingsArea = ResourceMappingHierarchyArea.create(mappings);
        allMappingsArea.setDescription("All elements to be operated on");
        //allMappingsArea.addPropertyChangeListener(this);
        allMappingsArea.createArea(composite);
        // Create a separator between the two sets of buttons
        Label seperator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
        seperator.setLayoutData(new GridData (GridData.FILL_HORIZONTAL));
    }

    protected Composite createDropDownDialogArea(Composite parent) {
        // TODO Auto-generated method stub
        return null;
    }

    protected void updateEnablements() {
        // TODO Auto-generated method stub

    }
    
    protected boolean includeDetailsButton() {
        return false;
    }

}
