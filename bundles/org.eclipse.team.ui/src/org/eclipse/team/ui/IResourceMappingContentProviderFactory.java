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
package org.eclipse.team.ui;

import org.eclipse.core.internal.resources.mapping.ResourceMapping;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.team.internal.ui.synchronize.ISynchronizeModelProvider;

/**
 * Factory for creating the content providers and other UI artifacts for
 * displaying resource mappings resource mappings in a form consistent with the
 * model during Team operations. The mechanism for obtaining the factory for
 * a set of resource mappings would be similar to obtaining an
 * <code>IResourceMappingMerger</code>.
 * 
 * @see org.eclipse.team.ui.mapping.IResourceMappingMerger
 */
public interface IResourceMappingContentProviderFactory {

    /**
     * Returns a content provider that can be used to display the provided
     * mappings in an element hierarchy that is consistent with the model. The
     * resulting hierarchy may contain additional elements as long as they are
     * for organizational purposes only. Any additional elements must not
     * include additional resources (i.e. <code>IResource</code>) in the
     * resource mappings contained in the view.
     * 
     * @param mappings the mappings to be displayed
     * @return a content provider that will arrange the provided mappings into a
     *         hierarchy.
     */
    public IResourceMappingContentProvider createContentProvider(
            ResourceMapping[] mappings);
    
    /**
     * Return a label provider that can be used to provide
     * labels for any resource mappings that adapt to this 
     * factory and any modle objects that appear in the tree
     * created by the <code>IResourceMappingContentProvider</code>
     * returned from the <code>createContentProvider</code>.
     * method.
     * @return a label provider
     */
    public ILabelProvider getLabelProvider();

    /**
     * Returns a model provider that can be used to display the synchronization
     * state of the model elements in the configuration. The configuration
     * provides access to everything the model should need in order to determine
     * and display the synchronization state for model elements. The resulting
     * tree should contain nodes for both the model elements being synchronized
     * and the files in which these models are persisted.
     * 
     * @param mappings the resource mappings for the model elements to be
     *            displayed
     * @param configuration the configuration that gives the model access to the
     *            information required to calculate the synchronization state of
     *            model elements and to properly root the synchronization tree
     *            that will be created by the provider.
     * @return a model provider that can be used to display the synchronization
     *         state of the model elements in the configuration
     */
    public ISynchronizeModelProvider createSynchronizeModelProvider(
            ResourceMapping[] mappings,
            ISynchronizeModelProviderConfiguration configuration);
    
    /**
	 * Return the resource mapping tree that provides the ability to travers the
	 * model the mapping belongs to.
	 * 
	 * @return a resource mapping tree
	 */
	public IResourceMappingTreeItem getResourceMappingTree();

}
