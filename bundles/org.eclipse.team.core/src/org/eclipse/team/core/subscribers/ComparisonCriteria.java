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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;

/**
 * A ComparisonCriteria used by a <code>SyncTreeSubscriber</code> to calculate the sync
 * state of the workspace resources against the remote. Subscribers are free to use the criteria
 * best suited for their environment. For example, an FTP subscriber could choose to use file
 * size or file size as compasison criterias.
 * 
 * @see org.eclipse.team.core.subscribers.SyncInfo
 * @see org.eclipse.team.core.subscribers.SyncTreeSubscriber
 */
abstract public class ComparisonCriteria {
	
	private ComparisonCriteria[] preConditions;
	
	public ComparisonCriteria() {
	}
	
	public ComparisonCriteria(ComparisonCriteria[] preConditions) {
		this.preConditions = preConditions;
	}

	/**
	 * Return the comparison criteria, in a format that is suitable for display to an end 
	 * user.
	 */
	abstract public String getName();
	
	/**
	 * Return the unique id that identified this comparison criteria.
	 */
	abstract public String getId();

	abstract public boolean compare(Object e1, Object e2, IProgressMonitor monitor) throws TeamException;
	
	/**
	 * @return
	 */
	protected ComparisonCriteria[] getPreConditions() {
		return preConditions;
	}

	protected boolean checkPreConditions(Object e1, Object e2, IProgressMonitor monitor) throws TeamException {
		for (int i = 0; i < preConditions.length; i++) {
			ComparisonCriteria cc = preConditions[i];
			if(cc.compare(e1, e2, monitor)) {
				return true;
			}
		}	
		return false;
	}
}
