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

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;

/**
 * This interface provides access to a resource that is controlled
 * by a particular subscriber. The resource may be local or remote.
 * 
 * @since 3.0
 */
public interface ISubscriberResource {
	
	/**
	 * Answers a string that describes the name of the subscriber resource. The name may be
	 * displayed to the user.
	 * 
	 * @return name of the subscriber resource.
	 */
	public String getName();
	
	/**
	 * Answers if the remote element may have children.
	 * 
	 * @return <code>true</code> if the remote element may have children and 
	 * <code>false</code> otherwise.
	 */
	public boolean isContainer();
	
	/**
	 * Return an instance of IStorage or <code>null</code> if the subscriber resource
	 * does not have contents (i.e. is a folder). Since the <code>ISorage#getContents()</code>
	 * method does not accept an IProgressMonitor, this method must ensure that the contents
	 * access by the resulting IStorage is cahced locally (hence the IProgressMonitor 
	 * argument to this method). If the subscriber resource is local, this is not an issue
	 * but if the subscriber resource is remote, then implementations of this method should
	 * ensure that the resulting IStorage is accessing locally cached contents and is not
	 * contacting the server.
	 * @return
	 */
	public IStorage getStorage(IProgressMonitor monitor) throws TeamException;
	
	/**
	 * Return a content identifier that is used to differentiate versions
	 * or revisions of the same resource.
	 * 
	 * @return a String that identifies the version of the subscriber resource
	 * @throws TeamException
	 */
	public String getContentIdentifier() throws TeamException;
	
	/**
	 * Returns whether the subscriber resource is equal to the provided object.
	 * @param object
	 * @return
	 */
	public boolean equals(Object object);

}
