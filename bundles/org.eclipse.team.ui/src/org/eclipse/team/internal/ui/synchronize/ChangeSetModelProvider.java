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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.team.core.subscribers.ChangeSet;
import org.eclipse.team.core.subscribers.IChangeSetChangeListener;
import org.eclipse.team.core.synchronize.*;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.ui.synchronize.*;

/**
 * Model provider for showing change sets in a sync page.
 */
public class ChangeSetModelProvider extends CompositeModelProvider {

    private ViewerSorter viewerSorter = new ChangeSetModelSorter(this, ChangeSetModelSorter.COMMENT);
	
	// The id of the sub-provider
    private final String subProvierId;
	
	private Map rootToProvider = new HashMap(); // Maps ISynchronizeModelElement -> AbstractSynchronizeModelProvider
	
	private ViewerSorter embeddedSorter;
	
	SyncInfoSetChangeSetCollector collector;
	
	IChangeSetChangeListener collectorListener = new IChangeSetChangeListener() {
	    
        /* (non-Javadoc)
         * @see org.eclipse.team.core.subscribers.IChangeSetChangeListener#setAdded(org.eclipse.team.core.subscribers.ChangeSet)
         */
        public void setAdded(ChangeSet set) {
            ISynchronizeModelElement node = getModelElement(set);
            ISynchronizeModelProvider provider = null;
            if (node != null) {
                provider = getProviderRootedAt(node);
            }
            if (provider == null) {
                provider = createProvider(set);
            }
            provider.prepareInput(null);
        }

        /* (non-Javadoc)
         * @see org.eclipse.team.core.subscribers.IChangeSetChangeListener#defaultSetChanged(org.eclipse.team.core.subscribers.ChangeSet, org.eclipse.team.core.subscribers.ChangeSet)
         */
        public void defaultSetChanged(ChangeSet previousDefault, ChangeSet set) {
            // There is no default set for checked-in change sets
        }

        /* (non-Javadoc)
         * @see org.eclipse.team.core.subscribers.IChangeSetChangeListener#setRemoved(org.eclipse.team.core.subscribers.ChangeSet)
         */
        public void setRemoved(ChangeSet set) {
            ISynchronizeModelElement node = getModelElement(set);
            if (node != null) {
	            ISynchronizeModelProvider provider = getProviderRootedAt(node);
	            clearModelObjects(node);
	            removeProvider(provider);
            }
        }

        /* (non-Javadoc)
         * @see org.eclipse.team.core.subscribers.IChangeSetChangeListener#nameChanged(org.eclipse.team.core.subscribers.ChangeSet)
         */
        public void nameChanged(ChangeSet set) {
            // The name of checked-in change sets should not change
        }

        /* (non-Javadoc)
         * @see org.eclipse.team.core.subscribers.IChangeSetChangeListener#resourcesChanged(org.eclipse.team.core.subscribers.ChangeSet, org.eclipse.core.resources.IResource[])
         */
        public void resourcesChanged(ChangeSet set, IResource[] resources) {
            // The sub-providers listen directly to the sets for changes
            // There is no global action to be taken for such changes
        }
    };
	
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
	
    protected ChangeSetModelProvider(ISynchronizePageConfiguration configuration, SyncInfoSet set, String subProvierId) {
        super(configuration, set);
        this.subProvierId = subProvierId;
        collector = getChangeSetCapability().createCheckedInChangeSetCollector(configuration);
        collector.setProvider(this);
        collector.addListener(collectorListener);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.AbstractSynchronizeModelProvider#handleChanges(org.eclipse.team.core.synchronize.ISyncInfoTreeChangeEvent, org.eclipse.core.runtime.IProgressMonitor)
     */
    protected void handleChanges(ISyncInfoTreeChangeEvent event, IProgressMonitor monitor) {
        collector.handleChange(event);
        // super.handleChanges(event, monitor); TODO Handle outgoing changes
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.CompositeModelProvider#handleAddition(org.eclipse.team.core.synchronize.SyncInfo)
     */
    protected void handleAddition(SyncInfo info) {
        // TODO: Nothing to do since change handling was bypassed
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.AbstractSynchronizeModelProvider#buildModelObjects(org.eclipse.team.ui.synchronize.ISynchronizeModelElement)
     */
    protected IDiffElement[] buildModelObjects(ISynchronizeModelElement node) {
        // This method is invoked on a reset after the provider state has been cleared.
        // Reseting the collector will rebuild the model
		if (node == getModelRoot()) {
			collector.reset(getSyncInfoSet());
		}
		return new IDiffElement[0];
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
    public void setViewerSorter(ViewerSorter viewerSorter) {
        this.viewerSorter = viewerSorter;
        firePropertyChange(ISynchronizeModelProvider.P_VIEWER_SORTER, null, null);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.AbstractSynchronizeModelProvider#runViewUpdate(java.lang.Runnable)
     */
    public void runViewUpdate(Runnable runnable) {
        super.runViewUpdate(runnable);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.AbstractSynchronizeModelProvider#createActionGroup()
     */
    protected SynchronizePageActionGroup createActionGroup() {
        return new ChangeSetActionGroup(this);
    }
    
    private ISynchronizeModelProvider createProviderRootedAt(ISynchronizeModelElement parent, SyncInfoTree set) {
        ISynchronizeModelProvider provider = createModelProvider(parent, getSubproviderId(), set);
        addProvider(provider);
        rootToProvider.put(parent, provider);
        return provider;
    }

    private ISynchronizeModelProvider getProviderRootedAt(ISynchronizeModelElement parent) {
        return (ISynchronizeModelProvider)rootToProvider.get(parent);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.CompositeModelProvider#removeProvider(org.eclipse.team.internal.ui.synchronize.AbstractSynchronizeModelProvider)
     */
    protected void removeProvider(ISynchronizeModelProvider provider) {
        rootToProvider.remove(provider.getModelRoot());
        super.removeProvider(provider);
    }
    
    /**
     * Return the id of the sub-provider used by the commit set provider.
     * @return the id of the sub-provider used by the commit set provider
     */
    public String getSubproviderId() {
        return subProvierId;
    }

    /**
     * Return the sorter associated with the sub-provider being used.
     * @return the sorter associated with the sub-provider being used
     */
    public ViewerSorter getEmbeddedSorter() {
        return embeddedSorter;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.synchronize.CompositeModelProvider#clearModelObjects(org.eclipse.team.ui.synchronize.ISynchronizeModelElement)
     */
    protected void clearModelObjects(ISynchronizeModelElement node) {
        super.clearModelObjects(node);
        if (node == getModelRoot()) {
            rootToProvider.clear();
            // Throw away the embedded sorter
            embeddedSorter = null;
            createRootProvider();
        }
    }

    /*
     * Create the root subprovider which is used to display resources
     * that are not in a commit set. This provider is created even if
     * it is empty so we can have access to the appropriate sorter 
     * and action group 
     */
    private void createRootProvider() {
        // Recreate the sub-provider at the root and use it's viewer sorter and action group
        final ISynchronizeModelProvider provider = createProviderRootedAt(getModelRoot(), new SyncInfoTree());
        embeddedSorter = provider.getViewerSorter();
        if (provider instanceof AbstractSynchronizeModelProvider) {
            SynchronizePageActionGroup actionGroup = ((AbstractSynchronizeModelProvider)provider).getActionGroup();
            if (actionGroup != null) {
                // This action group will be disposed when the provider is disposed
                getConfiguration().addActionContribution(actionGroup);
                provider.addPropertyChangeListener(new IPropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent event) {
                        if (event.getProperty().equals(P_VIEWER_SORTER)) {
                            embeddedSorter = provider.getViewerSorter();
                            ChangeSetModelProvider.this.firePropertyChange(P_VIEWER_SORTER, null, null);
                        }
                    }
                });
            }
        }
    }
    
    /*
     * Create a provider and node for the given change set
     */
    private ISynchronizeModelProvider createProvider(ChangeSet set) {
        ChangeSetDiffNode node = new ChangeSetDiffNode(getModelRoot(), set);
        return createProviderRootedAt(node, set.getSyncInfoSet());
    }
    
    /*
     * Find the root element for the given change set.
     * A linear searhc is used,
     */
    protected ISynchronizeModelElement getModelElement(ChangeSet set) {
        IDiffElement[] children = getModelRoot().getChildren();
        for (int i = 0; i < children.length; i++) {
            IDiffElement element = children[i];
            if (element instanceof ChangeSetDiffNode && ((ChangeSetDiffNode)element).getSet() == set) {
                return (ISynchronizeModelElement)element;
            }
        }
        return null;
    }

    /*
     * Return the change set capability
     */
    public ChangeSetCapability getChangeSetCapability() {
        return getConfiguration().getParticipant().getChangeSetCapability();
    }
    
    public void dispose() {
        collector.removeListener(collectorListener);
        collector.dispose();
        super.dispose();
    }
}
