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

/**
 * An <code>ISubscriberResourceComparator</code> is provided by a <code>Subscriber</code> 
 * and used by a <code>SyncInfo</code> to calculate the sync
 * state of the workspace resources. Subscribers should provide a criteria
 * best suited for their environment. For example, an FTP subscriber could choose to use file
 * size or file timestamps as comparison criterias whereas a CVS workspace subscriber would
 * use file revision numbers.
 * 
 * @see SyncInfo
 * @see Subscriber
 * @since 3.0
 */
public interface ISubscriberResourceComparator {
	
	/**
	 * Returns <code>true</code> if the local resource
	 * matches the remote resource based on this criteria and <code>false</code>
	 * otherwise. Comparing should be fast and based on cached information.
	 *  
	 * @param resource the local resource to be compared
	 * @param remote the remote resources to be compared
	 * @return <code>true</code> if local and remote are equal based on this criteria and <code>false</code>
	 * otherwise.
	 */
	public boolean compare(IResource local, ISubscriberResource remote);
	
	/**
	 * Returns <code>true</code> if the base resource
	 * matches the remote resource based on this criteria and <code>false</code>
	 * otherwise. Comparing should be fast and based on cached information.
	 *  
	 * @param base the base resource to be compared
	 * @param remote the remote resources to be compared
	 * @return <code>true</code> if base and remote are equal based on this criteria and <code>false</code>
	 * otherwise.
	 */
	public boolean compare(ISubscriberResource base, ISubscriberResource remote);
}
