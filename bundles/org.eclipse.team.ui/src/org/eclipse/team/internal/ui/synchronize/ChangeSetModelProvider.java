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

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * @author Administrator
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ChangeSetModelProvider extends CompositeModelProvider {

    private ViewerSorter viewerSorter;

	/* *****************************************************************************
	 * Descriptor for this model provider
	 */
	public static class ChangeSetModelProviderDescriptor implements ISynchronizeModelProviderDescriptor {
		public static final String ID = TeamUIPlugin.ID + ".modelprovider_cvs_changelog"; //$NON-NLS-1$
		public String getId() {
			return ID;
		}		
		public String getName() {
			return Policy.bind("ChangeLogModelProvider.5"); //$NON-NLS-1$
		}		
		public ImageDescriptor getImageDescriptor() {
			return TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_CHANGE_SET);
		}
	};
	private static final ChangeSetModelProviderDescriptor descriptor = new ChangeSetModelProviderDescriptor();
	
    protected ChangeSetModelProvider(ISynchronizePageConfiguration configuration, SyncInfoSet set) {
        super(configuration, set);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.CompositeModelProvider#handleAddition(org.eclipse.team.core.synchronize.SyncInfo)
     */
    protected void handleAddition(SyncInfo info) {
        if (isLocalChange(info)) {
            handleLocalChange(info);
        }
        if (isRemoteChange(info)) {
            handleRemoteChange(info);
        }
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.CompositeModelProvider#handleChange(org.eclipse.team.core.synchronize.SyncInfo)
     */
    protected void handleChange(SyncInfo info) {
        // TODO Auto-generated method stub
        super.handleChange(info);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.CompositeModelProvider#handleRemoval(org.eclipse.core.resources.IResource)
     */
    protected void handleRemoval(IResource resource) {
        // TODO Auto-generated method stub
        super.handleRemoval(resource);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.AbstractSynchronizeModelProvider#buildModelObjects(org.eclipse.team.ui.synchronize.ISynchronizeModelElement)
     */
    protected IDiffElement[] buildModelObjects(ISynchronizeModelElement node) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.ISynchronizeModelProvider#getDescriptor()
     */
    public ISynchronizeModelProviderDescriptor getDescriptor() {
        return descriptor;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.ISynchronizeModelProvider#getViewerSorter()
     */
    public ViewerSorter getViewerSorter() {
        return viewerSorter;
    }

    /*
     * Method to allow ChangeSetActionGroup to set the viewer sorter of this provider.
     */
    /* package */ void setViewerSorter(ViewerSorter viewerSorter) {
        this.viewerSorter = viewerSorter;
        firePropertyChange(ISynchronizeModelProvider.P_VIEWER_SORTER, null, null);
    }

}
