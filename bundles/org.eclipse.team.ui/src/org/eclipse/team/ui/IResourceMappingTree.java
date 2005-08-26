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
 * Interface which supports the traversal of the model tree in which a given
 * resource mapping is contained.
 */
public interface IResourceMappingTree {

	/**
	 * Return the parents of the given resource mapping. The parant mappings
	 * should include mappings for any model elements that directly parents the
	 * model object of the given resource mapping in any views visible to the
	 * user. In most cases, there will be a single resource mapping
	 * corresponding to the parent of the model element the resource mapping
	 * represents. However, there can be multiple parents in cases where the
	 * model appears to the user in different forms.
	 * 
	 * @param mapping
	 *            the resource mapping
	 * @return the parents of the resource mapping
	 */
	public ResourceMapping[] getParents(ResourceMapping mapping);

	/**
	 * Return the children of the given resource mapping.
	 * 
	 * @param mapping
	 *            the resource mapping
	 * @return the children of the resource mapping
	 */
	public ResourceMapping[] getChildren(ResourceMapping mapping);

}
