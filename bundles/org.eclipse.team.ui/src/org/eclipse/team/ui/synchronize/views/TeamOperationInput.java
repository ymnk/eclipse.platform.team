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
package org.eclipse.team.ui.synchronize.views;

import org.eclipse.core.resources.IResource;

/**
 * A container for the <code>IResource</code> based input to a team operation.
 * It is used by a logical view to communicate to a team operation what
 * resources make up a logical view element.
 * 
 * @see LogicalViewProvider
 */
public class TeamOperationInput {
	
	private IResource[] resources;
	private int depth;

	/**
	 * Create a team operation input that consists of the given resources,
	 * all of which should be traversed to the given depth.
	 * @param resources the resources to be operated on
	 * @param depth the depth to traverse the resources
	 */
	public TeamOperationInput(IResource[] resources, int depth) {
		this.resources = resources;
		this.depth = depth;
	}

	/**
	 * Return the depth that the team operation should traverse the resource.
	 * The depth values are those provided by the <code>IResource</code> interface,
	 * namely <code>IResource.DEPTH_ZERO</code>, <code>IResource.DEPTH_ONE</code>
	 * or <code>IResource.DEPTH_INFINITE</code>.
	 * @return Returns the depth
	 */
	public int getDepth() {
		return depth;
	}

	/**
	 * Return the resources to be included in the team operation. These resources should be traversed
	 * to the depth provided by this operation input.
	 * @return Returns the resources.
	 */
	public IResource[] getResources() {
		return resources;
	}

}
