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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.core.subscribers.SyncSetInputFromSyncSet;
import org.eclipse.team.internal.core.subscribers.WorkingSetSyncSetInput;

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

	private WorkingSetSyncSetInput workingSetInput;
	private SyncSetInputFromSyncSet filteredInput;
	private SyncInfoSet source;

	/**
	 * Create a filtered collector that filters using the provided <code>SyncInfoFilter</code>
	 * and working set resources. Only <code>SyncInfo</code> that are contained in the source set,
	 * are descendants of (or equal to) working set resources and match the provided <code>SyncInfoFilter</code>
	 * will appear in the output set.
	 * @param source the source <code>SyncInfoSet</code>
	 * @param workingSet the working set resource filter or <code>null</code> if there is no working set filter
	 * @param filter the <code>SyncInfoFilter</code> or <code>null</code> if no filtering is desired
	 */
	public FilteredSyncInfoCollector(SubscriberSyncInfoCollector subscriberCollector, IResource[] workingSet, SyncInfoFilter filter) {
		this.source = subscriberCollector.getSyncInfoSet();
		
		// TODO: optimize and don't use working set if no roots are passed in
		workingSetInput = new WorkingSetSyncSetInput(source);
		workingSetInput.setWorkingSet(workingSet);		
		filteredInput = new SyncSetInputFromSyncSet(workingSetInput.getSyncSet());
		if(filter == null) {
			filter = new SyncInfoFilter() {
				public boolean select(SyncInfo info, IProgressMonitor monitor) {
					return true;
				}
			};
		}
		filteredInput.setFilter(filter);
		
		try {
			start(new NullProgressMonitor()); // TODO
		} catch (TeamException e) {
			// TODO 		
		}
	}

	/**
	 * Start the collector. After this method returns the output <code>SyncInfoSet</code>
	 * of the collector will be populated.
	 * @param monitor a progress monitor
	 * @throws TeamException
	 */
	public void start(IProgressMonitor monitor) throws TeamException{
		workingSetInput.reset(monitor);
	}
	
	public void stop() {
		
	}
	
	/**
	 * Return the output <code>SyncInfoSet</code> that contains the filtered <code>SyncInfo</code>.
	 * @return the output <code>SyncInfoSet</code>
	 */
	public SyncInfoSet getSyncInfoSet() {
		if(filteredInput != null) {
			return filteredInput.getSyncSet();
		} else {
			return workingSetInput.getSyncSet();
		}
	}
	
	/**
	 * Set the working set resources used to filter the output <code>SyncInfoSet</code>.
	 * @param resources the working set resources
	 */
	public void setWorkingSet(IResource[] resources) {
		workingSetInput.setWorkingSet(resources);
		try {
			workingSetInput.reset(null /* TODO */);
		} catch (TeamException e) {
			// TODO 
		}
	}
	
	/**
	 * Get th working set resources used to filter the output working set.
	 * @return the working set resources
	 */
	public IResource[] getWorkingSet() {
		return workingSetInput.getWorkingSet();
	}
	
	/**
	 * Set the
	 * @param filter
	 * @param monitor
	 */
	public void setFilter(SyncInfoFilter filter, IProgressMonitor monitor) {
		filteredInput.setFilter(filter);
		try {
			filteredInput.reset(monitor);
		} catch (TeamException e) {
		}
	}
	
	public SyncInfoFilter getFilter() {
		if(filteredInput != null) {
			return filteredInput.getFilter();
		}
		return null;
	}
	
	/**
	 * Return a <code>SyncInfoSet</code> that contains the elements from the source set filtered
	 * by the working set resources but not the collector's <code>SyncInfoFilter</code>.
	 * @return a <code>SyncInfoSet</code>
	 */
	public SyncInfoSet getWorkingSetSyncInfoSet() {
		return workingSetInput.getSyncSet();
	}
	
	/**
	 * Dispose of the collector. The collector  cannot be restarted after it has been disposed.
	 */
	public void dispose() {
		workingSetInput.disconnect();
		if(filteredInput != null) {
			filteredInput.disconnect();
		}
	}
}
