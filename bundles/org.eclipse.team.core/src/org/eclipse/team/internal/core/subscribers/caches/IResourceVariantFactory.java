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
package org.eclipse.team.internal.core.subscribers.caches;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.IResourceVariant;

/**
 * Interface for fetching resource variants and
 * for storing and retrieving resource variants in a 
 * <code>ResourceVariantTree</code>
 */
public interface IResourceVariantFactory {
		
	/**
	 * Fetch the resource variant corresponding to the given resource.
	 * The depth parameter indicates the depth to which the resource 
	 * variant's desendants will be traversed by subsequent calls to 
	 * <code>fecthMembers(IResourceVariant, IProgressMonitor)</code>. 
	 * This method may prefetch the descendants to the provided depth
	 * or may just return the variant handle corresponding to the given 
	 * local resource, in which case
	 * the descendant variants will be fetched by <code>fecthMembers(IResourceVariant, IProgressMonitor)</code>.
	 * @param resource the local resource
	 * @param depth the depth of the refresh  (one of <code>IResource.DEPTH_ZERO</code>,
	 * <code>IResource.DEPTH_ONE</code>, or <code>IResource.DEPTH_INFINITE</code>)
	 * @param monitor a progress monitor
	 * @return the resource variant corresponding to the given local resource
	 */
	public IResourceVariant fetchVariant(IResource resource, int depth, IProgressMonitor monitor) throws TeamException;

	/**
	 * Fetch the members of the given resource variant handle. This method may
	 * return members that were fetched when 
	 * <code>fetchVariant(IResource, int, IProgressMonitor</code> 
	 * was called or may fetch the children directly. 
	 * @param variant the resource variant
	 * @param progress a progress monitor
	 * @return the members of the resource variant.
	 */
	public IResourceVariant[] fetchMembers(IResourceVariant variant, IProgressMonitor progress) throws TeamException;

	/**
	 * Returns the bytes that represent the state of the resource variant.
	 * The bytes can be stored in the <code>ResourceVariantTree</code> 
	 * and used to recreate the variant using <code>getResourceVariant</code>.
	 * 
	 * @param local the local resource
	 * @param remote the corresponding resource variant handle
	 * @return the bytes for the resource variant.
	 */
	public byte[] getBytes(IResource resource, IResourceVariant variant) throws TeamException;

	/**
	 * Create a resource variant handle for the local resource
	 * from the bytes stored in the given resource variant tree.
	 * @param resource the local resource
	 * @param tree the resource variant tree
	 * @return a resource variant handle
	 * @throws TeamException
	 */
	public IResourceVariant getResourceVariant(IResource resource, ResourceVariantTree tree) throws TeamException;
}
