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
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberResourceMappingContext;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter.SyncInfoDirectionFilter;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.internal.ccvs.ui.operations.ITagOperation;
import org.eclipse.team.internal.ccvs.ui.operations.TagOperation;
import org.eclipse.team.internal.ui.dialogs.*;
import org.eclipse.team.internal.ui.dialogs.MappingSelectionDialog;
import org.eclipse.team.internal.ui.dialogs.ResourceMappingResourceDisplayArea;

public class TagLocalAction extends TagAction {

    private final class UncommittedFilter implements IResourceMappingResourceFilter {
        public boolean select(IResource resource,
                ResourceMapping mapping, ResourceTraversal traversal)
                throws CoreException {
            SyncInfo info = CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().getSyncInfo(resource);
            return (info != null && getResourceFilter().select(info));
        }
    }
    
    private final class UncommittedChangesDialog extends MappingSelectionDialog {

        private final Subscriber subscriber = CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber();
        private final FastSyncInfoFilter resourceFilter = getResourceFilter();
        
        private UncommittedChangesDialog(Shell parentShell, String dialogTitle, ResourceMapping[] mappings) {
            super(parentShell, dialogTitle, mappings, new UncommittedFilter());
        }

        protected String getSingleMappingMessage(ResourceMapping mapping) {
            String label = ResourceMappingResourceDisplayArea.getLabel(mapping);
            return "There are uncommitted changes contained in ''{0}'' that will not be tagged. Should ''{0}'' be included in the operation?" + label;
        }

        protected String getMultipleMappingsMessage() {
            return "The following contain uncommitted changes that will not be versioned. Uncheck those that should be excluded from the tag operation.";
        }

        protected String getResourceListMessage(ResourceMapping mapping) {
            if (mapping == null) {
                return "Select an item to see the resources it contains.";
            } else {
                String label = ResourceMappingResourceDisplayArea.getLabel(mapping);
                return "Preview the resource contained in ''{0}''" + label;
            }
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

    ResourceMapping[] mappings;
	
	protected boolean performPrompting()  {
		// Prompt for any uncommitted changes
        mappings = getCVSResourceMappings();
        UncommittedChangesDialog dialog = new UncommittedChangesDialog(getShell(), "Tag Uncommitted Changes?", mappings);
		mappings = dialog.promptToSelectMappings();
		if(mappings.length == 0) {
			// nothing to do
			return false;						
		}
		
		return true;
	}

    private FastSyncInfoFilter getResourceFilter() {
        // Return a filter that selects outgoing changes
        return new SyncInfoDirectionFilter(new int[] { SyncInfo.OUTGOING, SyncInfo.CONFLICTING });
    }

    protected ITagOperation createTagOperation() {
        if (mappings == null)
            mappings = getCVSResourceMappings();
		return new TagOperation(getTargetPart(), mappings);
	}
	
		/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#getId()
	 */
	public String getId() {
		return ICVSUIConstants.CMD_TAGASVERSION;
	}
}
