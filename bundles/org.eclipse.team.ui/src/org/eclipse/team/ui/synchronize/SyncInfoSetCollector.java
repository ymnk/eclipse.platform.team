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
package org.eclipse.team.ui.synchronize;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.ui.synchronize.sets.SyncSetInputFromSyncSet;
import org.eclipse.team.internal.ui.synchronize.sets.WorkingSetSyncSetInput;
import org.eclipse.team.ui.synchronize.actions.SyncInfoFilter;

/**
 * Collects changes from a provided sync info set and creates another set based on 
 * the provided filters.
 * 
 * @see SyncInfoCollector
 */
public class SyncInfoSetCollector {

	private WorkingSetSyncSetInput workingSetInput;
	private SyncSetInputFromSyncSet filteredInput;
	private SyncInfoSet source;

	public SyncInfoSetCollector(SyncInfoSet source, IResource[] workingSet, SyncInfoFilter filter) {
		this.source = source;
		
		// TODO: optimize and don't use working set if no roots are passed in
		workingSetInput = new WorkingSetSyncSetInput((SyncInfoSet)source);
		workingSetInput.setWorkingSet(workingSet);		
		filteredInput = new SyncSetInputFromSyncSet(workingSetInput.getSyncSet());
		if(filter == null) {
			setFilter(new SyncInfoFilter() {
				public boolean select(SyncInfo info) {
					return true;
				}
			}, null);
		} else {
			setFilter(filter, null);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.sets.SyncInfoSetDelegator#getSyncInfoSet()
	 */
	public SyncInfoSet getSyncInfoSet() {
		if(filteredInput != null) {
			return filteredInput.getSyncSet();
		} else {
			return workingSetInput.getSyncSet();
		}
	}
	
	public void setWorkingSet(IResource[] resources) {
		workingSetInput.setWorkingSet(resources);
	}
	
	public IResource[] getWorkingSet() {
		return workingSetInput.getWorkingSet();
	}
	
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
	
	public SyncInfoSet getWorkingSetSyncInfoSet() {
		return workingSetInput.getSyncSet();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISyncInfoSet#dispose()
	 */
	public void dispose() {
		workingSetInput.disconnect();
		if(filteredInput != null) {
			filteredInput.disconnect();
		}
	}
}
