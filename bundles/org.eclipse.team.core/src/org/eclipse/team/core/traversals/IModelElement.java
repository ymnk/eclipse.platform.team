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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * This interface defines an element of an application that models the
 * data/processes of a particular problem domain. This purpose of this interface
 * is to support the transformation of the application model into its underlying
 * file system resources for the purposes of reconciling the model with versions
 * of the model stored in a repository. Hence, this interface provides the
 * bridge between a logical element and the physical resource(s) into which it
 * is stored but does not provide more comprehensive model access or
 * manipulations.
 * 
 * @see IResource
 * @since 3.1
 */
public interface IModelElement {

	/**
	 * Returns one or more traversals that can be used to access all the
	 * physical resources that constitute the logical resource. A traversal is
	 * simply a set of resources and the depth to which they are to be
	 * traversed. This method returns an array of traversals in order to provide
	 * flexibility in describing the traversals that constitute a model element.
	 * A depth is included to allow the clients of this interface (most likely
	 * repository providers) an opportunity to optimize the operation and also
	 * ensure that resources that were or have become members of the model
	 * element are included in the operation.
	 * <p>
	 * Implementors of this interface should ensure, as much as possible, that
	 * all resources that are or may be members of the model element are
	 * included. For instance, a model element should return the same list of
	 * resources regardless of the existance of the files on the file system.
	 * For example, if a logical resource called "form" maps to "/p1/form.xml"
	 * and "/p1/form.java" then whether form.xml or form.java existed, they
	 * should be returned by this method.
	 *</p><p>
	 * In some cases, it may not be possible for a model element to know all the
	 * resources that may consitite the element without accessing the state of
	 * the model element in another location (e.g. a repository). This method is
	 * provided with a context which, when provided, gives access to
	 * the members of correcponding remote containers and the contenst of
	 * corresponding remote files. This gives the model element the opportunity
	 * to deduce what additional resources should be included in the traversal.
	 * </p>
	 * @param context gives access to the state of
	 *            remote resources that correspond to local resources for the
	 *            purpose of determining traversals that adequately cover the
	 *            model element resources given the state of the model element
	 *            in another location. A null may be provided, in
	 *            which case the implementor can assume that only the local
	 *            resources are of interest to the client.
	 * @param monitor a progress monitor
	 * @return a set of traversals that cover the resources that constitute the
	 *         model element
	 */
	public ITraversal[] getTraversals(IModelContext context, IProgressMonitor monitor) throws CoreException;
}