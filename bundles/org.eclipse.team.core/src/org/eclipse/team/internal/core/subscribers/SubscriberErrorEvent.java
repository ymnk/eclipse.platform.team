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
package org.eclipse.team.internal.core.subscribers;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * Event used to propogate errors from a <code>SubscriberEventHandler</code> through to 
 * a view based on a subset of a subscribers state.
 *  */
public class SubscriberErrorEvent {

	private CoreException exception;
	private IResource resource;

	public SubscriberErrorEvent(CoreException e, IResource resource) {
		this.exception = e;
		this.resource = resource;
	}
	/**
	 * @return Returns the exception.
	 */
	public CoreException getException() {
		return exception;
	}
	/**
	 * @return Returns the resource.
	 */
	public IResource getResource() {
		return resource;
	}
}
