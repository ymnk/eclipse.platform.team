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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.team.core.synchronize.*;
import org.eclipse.team.internal.ui.ITeamUIImages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.TeamImages;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Provides a flat layout
 */
public class FlatModelProvider extends SynchronizeModelProvider {

	public static class FlatModelProviderDescriptor implements ISynchronizeModelProviderDescriptor {
		public static final String ID = TeamUIPlugin.ID + ".modelprovider_flat"; //$NON-NLS-1$
		public String getId() {
			return ID;
		}		
		public String getName() {
			return "Flat";
		}		
		public ImageDescriptor getImageDescriptor() {
			return TeamImages.getImageDescriptor(ITeamUIImages.IMG_FLAT);
		}
	}
	private static final FlatModelProviderDescriptor flatDescriptor = new FlatModelProviderDescriptor();
	
	/* *****************************************************************************
	 * Model element for the resources in this layout. They are displayed with filename and path
	 * onto the same line.
	 */
	public static class FullPathSyncInfoElement extends SyncInfoModelElement {
		public FullPathSyncInfoElement(IDiffContainer parent, SyncInfo info) {
			super(parent, info);
		}
		public String getName() {
			IResource resource = getResource();
			return resource.getName() + " - " + resource.getFullPath().toString(); //$NON-NLS-1$
		}
	}
	
    public FlatModelProvider(ISynchronizePageConfiguration configuration,
            SyncInfoSet set) {
        super(configuration, set);
    }
    
    public FlatModelProvider(AbstractSynchronizeModelProvider parentProvider, ISynchronizeModelElement modelRoot, ISynchronizePageConfiguration configuration, SyncInfoSet set) {
        super(parentProvider, modelRoot, configuration, set);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.ISynchronizeModelProvider#getViewerSorter()
     */
    public ViewerSorter getViewerSorter() {
		return new SynchronizeModelElementSorter() {
			protected int compareNames(IResource resource1, IResource resource2) {
				if (resource1.getType() == IResource.FILE && resource2.getType() == IResource.FILE) {
					return collator.compare(resource1.getFullPath().toString(), resource2.getFullPath().toString());
				}
				return super.compareNames(resource1, resource2);
			}
		};
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.AbstractSynchronizeModelProvider#buildModelObjects(org.eclipse.team.ui.synchronize.ISynchronizeModelElement)
     */
    protected IDiffElement[] buildModelObjects(ISynchronizeModelElement node) {
        if (node == getModelRoot());
        SyncInfo[] infos = getSyncInfoSet().getSyncInfos();
        List result = new ArrayList();
        for (int i = 0; i < infos.length; i++) {
            SyncInfo info = infos[i];
            result.add(createModelObject(node, info));
        }
        return (IDiffElement[]) result.toArray(new IDiffElement[result.size()]);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.AbstractSynchronizeModelProvider#handleResourceAdditions(org.eclipse.team.core.synchronize.ISyncInfoTreeChangeEvent)
     */
    protected void handleResourceAdditions(ISyncInfoTreeChangeEvent event) {
        addResources(event.getAddedResources());
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.AbstractSynchronizeModelProvider#handleResourceRemovals(org.eclipse.team.core.synchronize.ISyncInfoTreeChangeEvent)
     */
    protected void handleResourceRemovals(ISyncInfoTreeChangeEvent event) {
        IResource[] resources = event.getRemovedResources();
        for (int i = 0; i < resources.length; i++) {
            IResource resource = resources[i];
            removeFromViewer(resource);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.ISynchronizeModelProvider#getDescriptor()
     */
    public ISynchronizeModelProviderDescriptor getDescriptor() {
        return flatDescriptor;
    }

	protected void addResources(SyncInfo[] added) {
		for (int i = 0; i < added.length; i++) {
			SyncInfo info = added[i];
			ISynchronizeModelElement node = getModelObject(info.getLocal());
			if (node != null) {
				// Somehow the node exists. Remove it and read it to ensure
				// what is shown matches the contents of the sync set
				removeFromViewer(info.getLocal());
			}
			// Add the node to the root
			node = createModelObject(getModelRoot(), info);
		}
	}
	
	protected ISynchronizeModelElement createModelObject(ISynchronizeModelElement parent, SyncInfo info) {
	    SynchronizeModelElement newNode = new FullPathSyncInfoElement(parent, info);
		addToViewer(newNode);
		return newNode;
	}
}
