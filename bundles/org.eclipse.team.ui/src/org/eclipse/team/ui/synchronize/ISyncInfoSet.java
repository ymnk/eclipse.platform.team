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
package org.eclipse.team.ui.synchronize;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.ui.synchronize.sets.SyncInfoStatistics;

/**
 * A dynamic collection of {@link SyncInfo} objects. 
 * 
 * This set is optimized for fast retrieval of out-of-sync resources.
 * 
 * @see org.eclipse.team.ui.synchronize.TeamSubscriberParticipant#getInput()
 * @since 3.0
 */
public interface ISyncInfoSet {
	/**
	 * Add a change listener
	 * @param provider
	 */
	public void addSyncSetChangedListener(ISyncSetChangedListener listener);
	/**
	 * Remove a change listener
	 * @param provider
	 */
	public void removeSyncSetChangedListener(ISyncSetChangedListener listener);	
	/**
	 * Return the children of the given container who are either out-of-sync or contain
	 * out-of-sync resources.
	 * 
	 * @param container
	 * @return
	 */
	public IResource[] members(IResource resource);
	/**
	 * Return the out-of-sync descendants of the given resource. If the given resource
	 * is out of sync, it will be included in the result.
	 * 
	 * @param container
	 * @return
	 */
	public SyncInfo[] getOutOfSyncDescendants(IResource resource);
	/**
	 * Return an array of all the resources that are known to be out-of-sync
	 * @return
	 */
	public SyncInfo[] members();
	public SyncInfo getSyncInfo(IResource resource);
	public int size();
	public SyncInfoStatistics getStatistics();
	/**
	 * Return wether the given resource has any children in the sync set
	 * @param resource
	 * @return
	 */
	public boolean hasMembers(IResource resource);
}