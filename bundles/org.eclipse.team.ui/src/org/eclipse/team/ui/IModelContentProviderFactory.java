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

/**
 * Factory for creating a model content provider that will display
 * a set of resource mappings in a form consistent with the model.
 */
public interface IModelContentProviderFactory {
    
    /**
     * Returns a content provider that can be used to display the provided mappings
     * in an element hierarchy that is consistent with the model. The resulting hierarchy
     * may contain additional elements as long as they are for organizational purposes only.
     * Any additional elements must not include additional resources (i.e. <code>IResource</code>)
     * in the resource mappings contained in the view.
     * @param mappings the mappings to be displayed
     * @return a content provider that will arrange the provided mappings into a hierarchy.
     */
    public IModelContentProvider createContentProvider(ResourceMapping[] mappings);

}
