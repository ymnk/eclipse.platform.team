/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.core.subscribers;

import org.eclipse.team.internal.core.subscribers.SyncSetInputFromSyncSet;

/**
 * Collects changes from a provided <code>SyncInfoSet</code> and populates an output set based on 
 * the provided filters. The <code>SyncInfo</code> are filtered by a <code>SyncInfoFilter</code>
 * and a working set defined by an array of <code>IResource</code> which define the roots of all
 * resources that can potentially appear in the output set.
 * <p>
 * This class is not intended to be subclassed by clients
 * 
 * @see SyncInfoSet
 * @see SyncInfoFilter
 * @see SubscriberSyncInfoCollector
 * 
 * @since 3.0
 */
public final class FilteredSyncInfoCollector {

	private SyncSetInputFromSyncSet filteredInput;
	private SyncInfoSet source;

	/**
	 * Create a filtered sync info collector that collects sync info from the source set.
	 * @param collector the collector that provides the source set
	 * @param set the source set
	 * @param filter the filter to be applied to the output set
	 */
	public FilteredSyncInfoCollector(SubscriberSyncInfoCollector collector, SyncInfoSet set, SyncInfoFilter filter) {
		this.source = set;
		filteredInput = new SyncSetInputFromSyncSet(source, collector.getEventHandler());
		filteredInput.setFilter(filter);
	}

	/**
	 * Start the collector. After this method returns the output <code>SyncInfoSet</code>
	 * of the collector will be populated.
	 */
	public void start() {
		filteredInput.reset();
	}
	
	/**
	 * Return the output <code>SyncInfoSet</code> that contains the filtered <code>SyncInfo</code>.
	 * @return the output <code>SyncInfoSet</code>
	 */
	public SyncInfoSet getSyncInfoSet() {
		return filteredInput.getSyncSet();
	}
	
	public SyncInfoFilter getFilter() {
		if(filteredInput != null) {
			return filteredInput.getFilter();
		}
		return null;
	}
	
	/**
	 * Dispose of the collector. The collector  cannot be restarted after it has been disposed.
	 */
	public void dispose() {
		if(filteredInput != null) {
			filteredInput.disconnect();
		}
	}
}
