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
 * Classes which implement this interface provide methods that deal with the
 * events that are generated as a {@link ISyncInfoSet} changes.
 * <p>
 * After creating an instance of a class that implements this interface it can
 * be added to a sync info set using the <code>addSyncSetChangedListener</code>
 * method and removed using the <code>removeSyncSetChangedListener</code>
 * method.
 * </p>
 * 
 * @see ISyncInfoSetChangeEvent
 * @since 3.0
 */
public interface ISyncSetChangedListener {

	/**
	 * Sent when a {@link ISyncInfoSet} changes. For example, when a resource's 
	 * synchronization state changes.
	 * 
	 * @param event an event containing information about the change.
	 */
	public void syncSetChanged(ISyncInfoSetChangeEvent event);
}
