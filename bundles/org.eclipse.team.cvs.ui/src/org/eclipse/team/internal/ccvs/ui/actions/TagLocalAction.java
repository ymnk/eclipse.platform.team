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
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter.SyncInfoDirectionFilter;
import org.eclipse.team.internal.ccvs.ui.operations.ITagOperation;
import org.eclipse.team.internal.ccvs.ui.operations.TagOperation;

public class TagLocalAction extends TagAction {

    ResourceMapping[] mappings;
	
	protected boolean performPrompting()  {
		// Prompt for any uncommitted changes
        mappings = getCVSResourceMappings();
        MappingSelectionDialog dialog = new MappingSelectionDialog(getShell(), "Tag Uncommitted Changes?", mappings, getResourceFilter());
		mappings = dialog.promtpToSelectMappings();
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
}
