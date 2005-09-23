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
package org.eclipse.team.ui.mapping;

import org.eclipse.core.internal.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.internal.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

public abstract class ResourceMappingChangeDeterminationContext {

    /**
	 * Helper method to look for any changes in the given resource to the depth
	 * specified. By default, this method searches for files to the given depth
	 * using <code>fetchMembers</code> and uses the
	 * <code>contentDiffers</code> method to determine if there is a change.
	 * Subclasses may override to provide a more optimal search or include
	 * folder differences.
	 * 
	 * @param resource the resource.
	 * @param depth the depth
	 * @return whether there are any differences to the depth specified
	 * @throws CoreException if the refresh fails. Reasons include:
	 *             <ul>
	 *             <li>The server could not be contacted for some reason (e.g.
	 *             the context in which the operation is being called must be
	 *             short running). The status code will be
	 *             SERVER_CONTACT_PROHIBITED. </li>
	 *             </ul>
	 */
	protected boolean hasResourceDifference(IResource resource, int depth,
			IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(null, IProgressMonitor.UNKNOWN);
			if (resource.getType() == IResource.FILE) {
				return getRemoteContext().contentDiffers((IFile) resource, new SubProgressMonitor(
						monitor, IProgressMonitor.UNKNOWN));
			}
			if (depth != IResource.DEPTH_ZERO) {
				IResource[] members = getRemoteContext().fetchMembers((IContainer) resource,
						new SubProgressMonitor(monitor,
								IProgressMonitor.UNKNOWN));
				for (int i = 0; i < members.length; i++) {
					IResource member = members[i];
					if (hasResourceDifference(
							member,
							depth == IResource.DEPTH_INFINITE ? IResource.DEPTH_INFINITE
									: IResource.DEPTH_ZERO,
							new SubProgressMonitor(monitor,
									IProgressMonitor.UNKNOWN))) {
						return true;
					}
				}
			}
			return false;
		} finally {
			monitor.done();
		}
	}
    
    public abstract RemoteResourceMappingContext getRemoteContext();

	/**
	 * Calculate the change state of the given resources that were obtained from
	 * the given resource mapping. This method will first determine if any of
	 * the provided resources differ. If they do, the context will then delegate
	 * individual file checks to the provided resource mapping by calling the
	 * <code>ResourceMapping#calculateChangeState(RemoteResourceMappingContext, 
	 * IFile, IProgressMonitor)</code>
	 * method. If the server contact is required but not possible (i.e. an
	 * exception with the <code>SERVER_CONTACT_PROHIBITED</code> occurs) this
	 * method should return <code>MAY_HAVE_DIFFERENCE</code>. Subclass may
	 * override this method.
	 * 
	 * @param mapping
	 *            the resource mapping
	 * @param resources
	 *            the resources of the resource mapping
	 * @param depth
	 *            the depth to traverse the resources
	 * @param monitor
	 *            a progress monitor
	 * @return the calculated change state of <code>HAS_DIFFERENCE</code> if
	 *         the resources differ from their remote counterparts,
	 *         <code>NO_DIFFERENCE</code> if they do not or
	 *         <code>MAY_HAVE_DIFFERENCE</code> if server contact is required
	 *         to calculate the state but is prohibited (see
	 *         <code>SERVER_CONTACT_PROHIBITED</code>).
	 * @throws CoreException
	 *             if the state cannot be determined
	 */
	public int calculateChangeState(ResourceMapping mapping, ResourceMappingWithChangeDetermination changeDetermination,
			IResource[] resources, int depth, IProgressMonitor monitor)
			throws CoreException {
		try {
			monitor.beginTask(null, IProgressMonitor.UNKNOWN);
			int changeState = ResourceMappingWithChangeDetermination.NO_DIFFERENCE;
			for (int i = 0; i < resources.length; i++) {
				IResource resource = resources[i];
				// First, check if there is a difference in any resource to the
				// depth provided
				if (hasResourceDifference(resource, depth,
						new SubProgressMonitor(monitor,
								IProgressMonitor.UNKNOWN))) {
					// For files, we may need to check the contents
					if (resource.getType() == IResource.FILE) {
						int resourceChangeState = changeDetermination.calculateChangeState(
								this, (IFile) resource, new SubProgressMonitor(
										monitor, IProgressMonitor.UNKNOWN));
						if (resourceChangeState == ResourceMappingWithChangeDetermination.HAS_DIFFERENCE) {
							// There is a difference so we can stop searching
							changeState = resourceChangeState;
							break;
						} else if (resourceChangeState == ResourceMappingWithChangeDetermination.MAY_HAVE_DIFFERENCE) {
							// They may be a difference so we record it and keep
							// searching for a definite difference
							changeState = resourceChangeState;
						}
					} else {
						return ResourceMappingWithChangeDetermination.HAS_DIFFERENCE;
					}
				}
			}
			return changeState;
		} catch (CoreException e) {
			if (e.getStatus().getCode() == RemoteResourceMappingContext.SERVER_CONTACT_PROHIBITED)
				return ResourceMappingWithChangeDetermination.MAY_HAVE_DIFFERENCE;
			throw e;
		} finally {
			monitor.done();
		}
	}
}
