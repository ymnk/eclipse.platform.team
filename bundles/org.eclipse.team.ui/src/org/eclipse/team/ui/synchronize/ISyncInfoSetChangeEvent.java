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

/**
 * An event generated when a {@link ISyncInfoSet} collection is changed. The
 * mix of return types, SyncInfo and IResource, is a result of an optimization 
 * included in {@link ISyncInfoSet} collections that doesn't maintain SyncInfo objects
 * for in-sync resources.
 *  
 * @see ISyncInfoSet#addSyncSetChangedListener(ISyncSetChangedListener)
 * @see ISyncSetChangedListener
 * @since 3.0
 */
public interface ISyncInfoSetChangeEvent {
	/**
	 * Returns newly added out-of-sync <code>SyncInfo</code> elements. 
	 * 
	 * @return newly added <code>SyncInfo</code> elements or an empty list if this event 
	 * doesn't contain added resources.
	 */
	public SyncInfo[] getAddedResources();
	
	/**
	 * Returns parent resources of all newly added elements. 
	 * 
	 * @return parents of all newly added elements.  or an empty list if this event 
	 * doesn't contain added resources.
	 */
	public IResource[] getAddedRoots();
	
	/**
	 * Returns changed <code>SyncInfo</code> elements. The returned elements
	 * are still out-of-sync.
	 * 
	 * @return changed <code>SyncInfo</code> elements or an empty list if this event 
	 * doesn't contain changes resources.
	 */
	public SyncInfo[] getChangedResources();
	
	/**
	 * Returns removed <code>SyncInfo</code> elements. The returned elements
	 * are all  in-sync resources.
	 * 
	 * @return removed <code>SyncInfo</code> elements or an empty list if this event 
	 * doesn't contain removed resources.
	 */
	public IResource[] getRemovedResources();
	
	/**
	 * Returns parent resources of all newly removed elements. 
	 * 
	 * @return parents of all newly removed elements.  or an empty list if this event 
	 * doesn't contain added resources.
	 */
	public IResource[] getRemovedRoots();
	
	/**
	 * Returns the {@link ISyncInfoSet} that generated these events.
	 * 
	 * @return the {@link ISyncInfoSet} that generated these events.
	 */
	public ISyncInfoSet getSet();
	
	/**
	 * Returns <code>true</code> if the associated set has been reset and <code>false</code>
	 * otherwise. A sync info set is reset when changes in the set are all recalculated.
	 * 
	 * @return <code>true</code> if the associated set has been reset and <code>false</code>
	 * otherwise.
	 */
	public boolean isReset();
}