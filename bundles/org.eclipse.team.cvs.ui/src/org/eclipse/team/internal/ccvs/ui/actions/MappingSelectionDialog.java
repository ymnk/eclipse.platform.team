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

import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberResourceMappingContext;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ui.dialogs.DetailsDialog;
import org.eclipse.team.internal.ui.dialogs.ResourceMappingResourceDisplayArea;
import org.eclipse.ui.model.*;

/**
 * Dialog that will display any mappings that contain resources whose 
 * sync state match the provided filter.
 */
public class MappingSelectionDialog extends DetailsDialog {

    private final ResourceMapping[] mappings;
    private final FastSyncInfoFilter resourceFilter;
    private CheckboxTableViewer viewer;
    private Object[] checkedMappings;
    private final Subscriber subscriber = CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber();
    
    ResourceMappingResourceDisplayArea mappingArea;

    public MappingSelectionDialog(Shell parentShell, String dialogTitle, ResourceMapping[] mappings, FastSyncInfoFilter resourceFilter) {
        super(parentShell, dialogTitle);
        this.mappings = mappings;
        this.resourceFilter = resourceFilter;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.dialogs.DetailsDialog#createMainDialogArea(org.eclipse.swt.widgets.Composite)
     */
    protected void createMainDialogArea(Composite parent) {
        Composite composite = createComposite(parent);
        
        createWrappingLabel(composite, "The following elements contain uncommitted changes that will not be versioned. Uncheck those that should be excluded from the tag operation.");
        
        // TODO: add handling for a single resource mapper
        // TODO: add handling for a single resource mapper that was derived from directly selected resources
        viewer = CheckboxTableViewer.newCheckList(composite, SWT.SINGLE | SWT.BORDER);
        GridData data = new GridData(GridData.FILL_BOTH);
        data.heightHint = 150;
        viewer.getControl().setLayoutData(data);
        viewer.setContentProvider(new BaseWorkbenchContentProvider());
        viewer.setLabelProvider(new WorkbenchLabelProvider());
        viewer.setInput(new AdaptableList(mappings));
        viewer.addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
                checkedMappings = viewer.getCheckedElements();
                updateEnablements();
            }
        });
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                if (mappingArea != null) {
                    mappingArea.setMapping(getSelectedMapping());
                }
            }
        });
        viewer.setCheckedElements(mappings);
        // TODO: Add select/deselect all buttons
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.dialogs.DetailsDialog#createDropDownDialogArea(org.eclipse.swt.widgets.Composite)
     */
    protected Composite createDropDownDialogArea(Composite parent) {
        if (mappingArea == null) {
            mappingArea = new ResourceMappingResourceDisplayArea(getSelectedMapping());
        }
        Composite c = createComposite(parent);
        mappingArea.createArea(c);
        return c;
    }

    private ResourceMapping getSelectedMapping() {
        ISelection selection = viewer.getSelection();
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection ss = (IStructuredSelection) selection;
            Object firstElement = ss.getFirstElement();
            if (firstElement instanceof ResourceMapping)
                return (ResourceMapping)firstElement;
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.dialogs.DetailsDialog#updateEnablements()
     */
    protected void updateEnablements() {
        // Can always finish
        setPageComplete(true);
    }

    /**
     * Prompt for any mappings that match the given filter in order to allow the
     * user to explicitly include/exclude those mappings.
     * @return the mappings that either didn't match the filter or were selected by the user
     */
    public ResourceMapping[] promptToSelectMappings() {
        ResourceMapping[] matchingMappings = getMatchingMappings();
        if (matchingMappings.length > 0) {
            int code = open();
            if (code == OK) {
                Set result = new HashSet();
                result.addAll(Arrays.asList(mappings));
                result.removeAll(Arrays.asList(matchingMappings));
                result.addAll(Arrays.asList(getCheckedMappings()));
                return (ResourceMapping[]) result.toArray(new ResourceMapping[result.size()]);
            }
            return new ResourceMapping[0];
        } else {
            // No mappings match the filter so return them all
            return mappings;
        }
    }

    private Object[] getCheckedMappings() {
        return checkedMappings;
    }

    private ResourceMapping[] getMatchingMappings() {
        Set result = new HashSet();
        for (int i = 0; i < mappings.length; i++) {
            ResourceMapping mapping = mappings[i];
            if (matchesFilter(mapping)) {
                result.add(mapping);
            }
        }
        return (ResourceMapping[]) result.toArray(new ResourceMapping[result.size()]);
    }

    private boolean matchesFilter(ResourceMapping mapping) {
        try {
            mapping.visit(new SubscriberResourceMappingContext(subscriber), new IResourceVisitor() {
                public boolean visit(IResource resource) throws CoreException {
                    SyncInfo info = subscriber.getSyncInfo(resource);
                    if (info != null && resourceFilter.select(info)) {
                        throw new CoreException(Status.OK_STATUS);
                    }
                    return true;
                }
            }, null);
        } catch (CoreException e) {
            if (e.getStatus().isOK()) {
                return true;
            }
            CVSUIPlugin.log(e);
        }
        return false;
    }

}
