/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.core.traversals;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

/**
 * A model element traversal is simply a set of resources and the depth to which
 * each are to be traversed. A set of traversals is used to describe the
 * resources that constitute a model element.
 * 
 * @see org.eclipse.core.resources.IResource
 * @see org.eclipse.core.resources.model.IModelElement
 * @since 3.1
 */
public interface ITraversal {

	/**
	 * Returns the project which contains the resources that make up this
	 * taversal.
	 * 
	 * @return the project handle
	 */
	public IProject getProject();

	/**
	 * Returns the file system resource(s) for this traversal. The returned
	 * resources must be contained within the same project and need not exist in
	 * the local file system. The traversal of the returned resources should be
	 * done considering the flag returned by getDepth. If a resource returned by
	 * a traversal is a file, it should always be visited. If a resource of a
	 * traversal is a folder then files contained in the folder can only be
	 * visited if the folder is IResource.DEPTH_ONE or IResource.DEPTH_INFINITE.
	 * Child folders should only be visited if the depth is
	 * IResource.DEPTH_INFINITE.
	 */
	public IResource[] getResources();

	/**
	 * Return the depth to which the resources should be traversed.
	 * 
	 * @return the depth to which the physical resources are to be traversed
	 * (one of IResource.DEPTH_ZERO, IResource.DEPTH_ONE or
	 * IResource.DEPTH_INFINITE)
	 */
	public int getDepth();
}