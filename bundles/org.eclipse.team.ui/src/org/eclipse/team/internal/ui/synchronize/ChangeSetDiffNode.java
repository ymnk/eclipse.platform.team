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
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.core.subscribers.ChangeSet;
import org.eclipse.team.internal.ui.ITeamUIImages;
import org.eclipse.team.internal.ui.TeamUIPlugin;

/**
 * Node that represents a Change set in a synchronize page.
 */
public class ChangeSetDiffNode extends SynchronizeModelElement {

    private final ChangeSet set;

    public ChangeSetDiffNode(IDiffContainer parent, ChangeSet set) {
        super(parent);
        this.set = set;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.ui.synchronize.ISynchronizeModelElement#getResource()
     */
    public IResource getResource() {
        return null;
    }

    public ChangeSet getSet() {
        return set;
    }
    
	/* (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getImageDescriptor(java.lang.Object)
	 */
	public ImageDescriptor getImageDescriptor(Object object) {
		return TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_CHANGE_SET);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.structuremergeviewer.DiffNode#getName()
	 */
	public String getName() {
		return set.getName();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncInfoModelElement#toString()
	 */
	public String toString() {
		return getName();
	}
}
