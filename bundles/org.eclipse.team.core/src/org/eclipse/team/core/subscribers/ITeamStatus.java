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
package org.eclipse.team.core.subscribers;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;

/**
 * This interface provides Team specific status information.
 */
public interface ITeamStatus extends IStatus {
	
	/**
	 * Status code that indicates that there was an authentication failure 
	 * connecting to the server.
	 * The <code>getResource()</code> method returns <code>null</code>
	 */
	public static final int AUTHENTICATION_FAILURE = 1000;
	
	/**
	 * Status code that indicates that the user ius not authorized to
	 * perform the operation.
	 * The <code>getResource()</code> method returns <code>null</code>
	 */
	public static final int AUTHORIZATION_FAILURE = 1001;
	
	/**
	 * Status code that indicates there was an I/O failure. The
	 * operation can be retried.
	 * The <code>getResource()</code> method returns <code>null</code>
	 */
	public static final int IO_FAILURE = 1002;
	
	/**
	 * Status code that indicates there was a failure for some other reason.
	 * The text message of the status should provide an indication as to the
	 * nature of the failure and possibly how it could be corrected.
	 * The <code>getResource()</code> method returns <code>null</code>
	 */
	public static final int OTHER_FAILURE = 1003;
	
	/**
	 * Status code that indicates that the provided label is invalid.
	 * The <code>getResource()</code> method returns <code>null</code>
	 */
	public static final int INVALID_LABEL = 1004;
	
	/**
	 * The requested operation is either unsupported or the operation
	 * with the given paramaters is unsupported (i.e. put with
	 * override remote changes).
	 * The <code>getResource()</code> method returns <code>null</code>
	 */
	public static final int OPERATION_UNSUPPORTED = 1005;
	
	/**
	 * Status code that indicates that a resource was auto-merged.
	 * The <code>getResource()</code> method returns a handle to the
	 * resource.
	 */
	public static final int AUTO_MERGED = 1010;
	
	/**
	 * Status code that indicates that a resource is ignored.
	 * The <code>getResource()</code> method returns a handle to the
	 * resource.
	 */
	public static final int IS_IGNORED = 1011;
	
	/**
	 * Status code that indicates that a resource is locally modified
	 * and has not been updated during a get operation.
	 * The <code>getResource()</code> method returns a handle to the
	 * resource.
	 */
	public static final int IS_LOCALLY_MODIFIED = 1012;
	
	/**
	 * Status code that indicates that a resource that is
	 * not controlled is in the way of a new remote resource
	 * during a get operation or that a local resource that is
	 * explicitly mentioned in a put is not controlled.
	 * The <code>getResource()</code> method returns a handle to the
	 * resource.
	 */
	public static final int UNCONTROLLED_LOCAL_EXISTS = 1013;
	
	/**
	 * Status code that indicates that, during a pur operation,
	 * a corresponding remote resource 
	 * has been changed remotely since the last get operation.
	 * The <code>getResource()</code> method returns a handle to the
	 * resource.
	 */
	public static final int REMOTE_IS_STALE = 1014;
	
	/**
	 * Status code that indicates that the provided label already exists
	 * on another version of the corrresponding remote resource.
	 * The <code>getResource()</code> method returns a handle to the
	 * resource.
	 */
	public static final int LABEL_EXISTS = 1015;
	
	/**
	 * Return the resource associated with the staus method or 
	 * <code>null</code> if there is no associated resource.
	 * @return a resource handle
	 */
	public IResource getResource();

}
