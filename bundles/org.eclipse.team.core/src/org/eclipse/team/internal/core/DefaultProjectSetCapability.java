/**********************************************************************
 Copyright (c) 2004 Dan Rubel and others.
 All rights reserved.   This program and the accompanying materials
 are made available under the terms of the Common Public License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/legal/cpl-v10.html

 Contributors:

 Dan Rubel - initial API and implementation

 **********************************************************************/

package org.eclipse.team.internal.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.IProjectSetSerializer;
import org.eclipse.team.core.ProjectSetCapability;
import org.eclipse.team.core.ProjectSetSerializationContext;
import org.eclipse.team.core.TeamException;

/**
 * An internal class for backward compatibility with the 
 * {@link org.eclipse.team.core.IProjectSetSerializer} interface.
 * 
 * @since 3.0
 */
public class DefaultProjectSetCapability extends ProjectSetCapability {

	/**
	 * The old serialization interface
	 */
	private IProjectSetSerializer serializer;

	/**
	 * Create a new instance wrappering the specified serializer.
	 * 
	 * @param serializer the old serialization interface
	 */
	public DefaultProjectSetCapability(IProjectSetSerializer serializer) {
		this.serializer = serializer;
	}

	/**
	 * Redirect the request to the old serialization interface
	 * 
	 * @see IProjectSetSerializer
	 * @see org.eclipse.team.core.ProjectSetCapability#asReference(org.eclipse.core.resources.IProject[], org.eclipse.team.core.ProjectSetSerializationContext, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public String[] asReference(
		IProject[] providerProjects,
		ProjectSetSerializationContext context,
		IProgressMonitor monitor)
		throws TeamException {

		return serializer.asReference(providerProjects, context.getShell(), monitor);
	}

	/**
	 * Redirect the request to the old serialization interface
	 * 
	 * @see IProjectSetSerializer
	 * @see org.eclipse.team.core.ProjectSetCapability#addToWorkspace(java.lang.String[], org.eclipse.team.core.ProjectSetSerializationContext, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IProject[] addToWorkspace(
		String[] referenceStrings,
		ProjectSetSerializationContext context,
		IProgressMonitor monitor)
		throws TeamException {
			
		return serializer.addToWorkspace(referenceStrings, null, context.getShell(), monitor);
	}

}
