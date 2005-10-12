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

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Interface which supports the traversal of the model tree in which a given
 * resource mapping is contained. An instance of this interface can be obtained
 * from instances of <code>ResourceMapping</code> using the adaptable
 * mechanism (<code>getAdapter</code>) for those resource mappings that
 * support model traversal.
 * <p>
 * The API includes the resource mapping as a parameter of the methods in order
 * to allow implementors flexibility in whether to use a singleton
 * implementation of the interface or use an instance per resource mapping.
 * Clients should assume that each resource mapping adapts to a unique instance
 * of the interface but should also ensure that they pass the proper mapping to
 * any methods they call in case the implementor is using a singleton.
 * <p>
 * It is assumed that determining the parents of an existing resource mapping
 * using <code>getParents</code> will not be long running (i.e. the resource
 * mapping and its parents will already be loaded). The <code>getChildren</code>
 * method is included for completeness but clients should take care when using
 * this method as doing so may need load the model from disk. Hence, the method
 * require a progress monitor. 
 * 
 * TODO Should getChildren be included? It is not required for decoration
 * and could easily lead to aggressive model loading.
 */
public interface IResourceMappingTreeItem {

	/**
	 * Return the parents of the given resource mapping. The parent mappings
	 * should include mappings for any model elements that directly parents the
	 * model object of the given resource mapping in any views visible to the
	 * user. In most cases, there will be a single resource mapping
	 * corresponding to the parent of the model element the resource mapping
	 * represents. However, there can be multiple parents in cases where the
	 * model appears to the user in different forms.
	 * 
	 * @param mapping the resource mapping
	 * @return the parents of the resource mapping
	 */
	public ResourceMapping[] getParents(ResourceMapping mapping);

	/**
	 * Return the children of the given resource mapping. This method
	 * may be long running as the children may need to be loaded from
	 * disk.
	 * 
	 * @param mapping the resource mapping
	 * @param monitor a progress monitor
	 * @return the children of the resource mapping
	 */
	public ResourceMapping[] getChildren(ResourceMapping mapping, IProgressMonitor monitor);

}
