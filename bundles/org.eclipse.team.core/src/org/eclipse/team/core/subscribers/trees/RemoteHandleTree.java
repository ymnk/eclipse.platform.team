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
package org.eclipse.team.core.subscribers.trees;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.sync.IRemoteResource;

/**
 * A RemoteHandleTree stores remote handles for local resources. 
 */
public abstract class RemoteHandleTree {
	/**
	 * Returns the remote handle stored in this tree for the given local resource.
	 * @param resource the resource for which to find a remote
	 * @return a remote handle or <code>null</code> if the tree doesn't contain
	 * a handle for the resource.
	 */
	public abstract IRemoteResource getRemoteHandle(IResource resource);
	
	/** 
	 * Refreshes the resource hierarchy from the given resources and their children (to the specified depth) 
	 * from the corresponding resources in the remote location.
	 * @param resources
	 * @param depth
	 * @param monitor
	 * @return IResource[]
	 */
	public abstract IResource[] refresh(IResource[] resources, int depth, IProgressMonitor monitor);
	
	/**
	 * Forget about the given handle.
	 * @param resource
	 */
	public abstract void remove(IResource resource);
	
	/**
	 * Disposes of this tree. It is the client's responsibility to ensure that this method is called when the
	 * remote tree is no longer needed.	 
	 */
	public abstract void dispose();
}
