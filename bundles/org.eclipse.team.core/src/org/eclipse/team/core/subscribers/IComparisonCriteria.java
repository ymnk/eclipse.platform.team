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


/**
 * A ComparisonCriteria used by a <code>TeamSubscriber</code> to calculate the sync
 * state of the workspace resources. Subscribers are free to use the criteria
 * best suited for their environment. For example, an FTP subscriber could choose to use file
 * size or file timestamps as compasison criterias whereas a CVS workspace subscriber would
 * use file revision numbers.
 * 
 * @see SyncInfo
 * @see TeamSubscriber
 * @since 3.0
 */
public interface IComparisonCriteria {
	/**
	 * Returns <code>true</code> if e1 and e2 are equal based on this criteria and <code>false</code>
	 * otherwise. Comparing should be fast and based on cached information.
	 *  
	 * @param e1 object to be compared
	 * @param e2 object to be compared
	 * @return <code>true</code> if e1 and e2 are equal based on this criteria and <code>false</code>
	 * otherwise.
	 */
	public boolean compare(Object e1, Object e2);
}
