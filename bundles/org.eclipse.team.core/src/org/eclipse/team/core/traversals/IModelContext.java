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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A model context provides a model element with a view of the remote state
 * of a local resource as it relates to a repository operation that is in
 * progress. A repository provider can pass an instance of this interface to a
 * model element when obtaining a set of traversals for a model element. This
 * allows the model element to query the remote state of a resource in order to
 * determine if there are resources that exist remotely but do not exist locally
 * that should be included in the traversal.
 * 
 * @since 3.1
 */
public interface IModelContext {

	/**
	 * Return whether the contents of the corresponding remote differs from the
	 * content of the local file. This can be used by clients to determine if
	 * they nee to fetch the remote contents in order to determine if the
	 * resources that constitute the model element are different in another
	 * location.
	 * 
	 * @param file the local file
	 * @param monitor a progress monitor
	 * @return whether the contents of the corresponding remote differs from the
	 *         content of the local file
	 * @throws CoreException if this method fails. Reasons include:
	 *           - The corresponding remote resource does not exist (status
	 *             code will be IResourceStatus.REMOTE_DOES_NOT_EXIST).
	 *           - The corresponding remote resource is not a container
	 *             (status code will be IResourceStatus.REMOTE_WRONG_TYPE).
	 *           - The server communications failed (status code will be
	 *             IResourceStatus.REMOTE_COMMUNICATION_FAILURE).
	 */
	public boolean contentDiffers(IFile file, IProgressMonitor monitor) throws CoreException;

	/**
	 * Returns an instance of IStorage in order to allow the
	 * caller to access the contents of the remote that corresponds to the given
	 * local resource. The provided local file handle need not exist locally. A
	 * exception is thrown if the corresponding remote resource does not exist
	 * or is not a file.
	 * 
	 * This method may be long running as a server may need to be contacted to
	 * obtain the contents of the file.
	 * 
	 * @param file the local file
	 * @param monitor a progress monitor
	 * @return a storage that provides access to the contents of the local
	 *         resource's corresponding remote resource
	 * @throws CoreException if this method fails. Reasons include:
	 *           - The corresponding remote resource does not exist (status
	 *             code will beIResourceStatus.REMOTE_DOES_NOT_EXIST).
	 *           - The corresponding remote resource is not a file (status
	 *             code will be IResourceStatus.REMOTE_WRONG_TYPE).
	 *           - The server communications failed (status code will be
	 *             IResourceStatus.REMOTE_COMMUNICATION_FAILURE).
	 */
	public IStorage fetchContents(IFile file, IProgressMonitor monitor) throws CoreException;

	/**
	 * Returns the list of member resources whose corresponding remote resources
	 * are members of the corresponding remote resource of the given local
	 * container. The container need not exist locally and the result may
	 * include entries that do not exist locally and may not include all local
	 * children. An empty list is returned if the remote resource which
	 * corresponds to the container is empty. An exception is thrown if the
	 * corresponding remote does not exist or is not capable of having members.
	 * 
	 * This method may be long running as a server may need to be contacted to
	 * obtain the members of the containers corresponding remote resource.
	 * 
	 * @param container the local container
	 * @param monitor a progress monitor
	 * @return a list of member resources whose corresponding remote resources
	 *         are members of the remote counterpart of the given container
	 * @throws CoreException if this method fails. Reasons include:       
	 *           - The corresponding remote resource does not exist (status
	 *             code will be IResourceStatus.REMOTE_DOES_NOT_EXIST).
	 *           - The corresponding remote resource is not a container
	 *             (status code will be IResourceStatus.REMOTE_WRONG_TYPE).
	 *           - The server communications failed (status code will be
	 *             IResourceStatus.REMOTE_COMMUNICATION_FAILURE).
	 */
	public IResource[] fetchMembers(IContainer container, IProgressMonitor monitor) throws CoreException;
}