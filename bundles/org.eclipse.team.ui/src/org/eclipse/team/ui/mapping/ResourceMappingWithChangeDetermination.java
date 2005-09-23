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
import org.eclipse.core.internal.resources.mapping.ResourceTraversal;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public abstract class ResourceMappingWithChangeDetermination extends ResourceMapping {

    /**
	 * Constant returned by <code>calculateChangeState</code> to indicate that
	 * the model object of this resource mapping does not differ from the
	 * corresponding object in the remote location.
	 */
    public static final int NO_DIFFERENCE = 0;

    /**
	 * Constant returned by <code>calculateChangeState</code> to indicate that
	 * the model object of this resource mapping differs from the corresponding
	 * object in the remote location.
	 */
    public static final int HAS_DIFFERENCE = 1;

    /**
	 * Constant returned by <code>calculateChangeState</code> to indicate that
	 * the model object of this resource mapping may differ from the
	 * corresponding object in the remote location. This is returned when
	 * getChangeState was not provided with a progress monitor and the remote
	 * state of the object was not cached.
	 */
    public static final int MAY_HAVE_DIFFERENCE = 2;
    
    /**
	 * Calculate the change state of the local object when compared to it's
	 * remote representation. If server contact is required to properly
	 * calculate the state but is not allowed (as indicated by an exception with
	 * the code
	 * <code>RemoteResouceMappingContext.SERVER_CONTACT_PROHIBITED</code>),
	 * <code>MAY_HAVE_DIFFERENCE</code> should be returned. Otherwise
	 * <code>HAS_DIFFERENCE</code> or <code>NO_DIFFERENCE</code> should be
	 * returned as appropriate. Subclasses may override this method.
	 * <p>
	 * It is assumed that, when <code>canContactServer</code> is
	 * <code>false</code>, the methods
	 * <code>RemoteResourceMappingContext#contentDiffers</code> and
	 * <code>RemoteResourceMappingContext#fetchMembers</code> of the context
	 * provided to this method can be called without contacting the server.
	 * Clients should ensure that this is how the context they provide behaves.
	 * 
	 * @param context
	 *            a resource mapping context
	 * @param monitor
	 *            a progress monitor or <code>null</code>. If
	 *            <code>null</code> is provided, the server will not be
	 *            contacted and <code>MAY_HAVE_DIFFERENCE</code> will be
	 *            returned if the change state could not be properly determined
	 *            without contacting the server.
	 * @return the calculated change state of <code>HAS_DIFFERENCE</code> if
	 *         the object differs, <code>NO_DIFFERENCE</code> if it does not
	 *         or <code>MAY_HAVE_DIFFERENCE</code> if server contact is
	 *         required to calculate the state.
	 * @throws CoreException
	 */
    public int calculateChangeState(
    		ResourceMapping mapping,
            ResourceMappingChangeDeterminationContext context,
            IProgressMonitor monitor)
            throws CoreException {
        try {
			int changeState = NO_DIFFERENCE;
			ResourceTraversal[] traversals = getTraversals(mapping, context, monitor);
			for (int i = 0; i < traversals.length; i++) {
			    ResourceTraversal traversal = traversals[i];
			    // Calculate the change state for the traversal
			    int resourcesChangeState = context.calculateChangeState(mapping, this,
			            traversal.getResources(), traversal.getDepth(), monitor);
			    if (resourcesChangeState == HAS_DIFFERENCE) {
			        // There is a difference so we can stop searching
			        changeState = resourcesChangeState;
			        break;
			    } else if (resourcesChangeState == MAY_HAVE_DIFFERENCE) {
			        // They may be a difference so we record it and keep searching
			        // for a definite difference
			        changeState = resourcesChangeState;
			    }
			}
			return changeState;
		} catch (CoreException e) {
			if (e.getStatus().getCode() == RemoteResourceMappingContext.SERVER_CONTACT_PROHIBITED)
				return MAY_HAVE_DIFFERENCE;
			throw e;
		}
    }

    protected ResourceTraversal[] getTraversals(ResourceMapping mapping, ResourceMappingChangeDeterminationContext context, IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * This method is invoked from the
	 * <code>calculateChangeState(RemoteResourceMappingContext, IProgressMonitor)</code>
	 * method in order to determine if there is a change in a particular file.
	 * By default, the <code>RemoteResourceMappingContext#contentDiffers</code>
	 * method is used to determine if there is a file change. Model objects that
	 * only consist of part of the file may perform a more specific check by
	 * obtaining the contents using the
	 * <code>RemoteResourceMappingContext#fetchContents</code>. If server
	 * contact is required to properly calculate the state but is not allowed
	 * (as indicated by an exception with the code
	 * <code>RemoteResouceMappingContext.SERVER_CONTACT_PROHIBITED</code>),
	 * the fetch may fail. In this case, <code>MAY_HAVE_DIFFERENCE</code>
	 * should be returned. Subclasses may override this method.
	 * 
	 * @param context
	 *            the remote resource context
	 * @param file
	 *            the file being tested
	 * @param monitor
	 *            a progress monitor
	 * @return the calculated change state of <code>HAS_DIFFERENCE</code> if
	 *         the object differs, <code>NO_DIFFERENCE</code> if it does not
	 *         or <code>MAY_HAVE_DIFFERENCE</code> if server contact is
	 *         required to calculate the state.
	 * @throws CoreException if the change state could not be determined
	 */
    public int calculateChangeState(
    		ResourceMappingChangeDeterminationContext context, IFile file,
            IProgressMonitor monitor)
            throws CoreException {
        try {
			if (context.getRemoteContext().contentDiffers(file, monitor)) {
			    return HAS_DIFFERENCE;
			}
			return NO_DIFFERENCE;
		} catch (CoreException e) {
			if (e.getStatus().getCode() == RemoteResourceMappingContext.SERVER_CONTACT_PROHIBITED)
				return MAY_HAVE_DIFFERENCE;
			throw e;
		}
    }
}
