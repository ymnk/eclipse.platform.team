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
package org.eclipse.team.internal.ui.synchronize.sets;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.ui.synchronize.ISyncInfoSet;
import org.eclipse.team.ui.synchronize.actions.SyncInfoFilter;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;

public class SubscriberInputSyncInfoSetExtension extends SyncInfoSetDelegator {

	private WorkingSetSyncSetInput workingSetInput;
	private SyncSetInputFromSyncSet filteredInput;
	private ISyncInfoSet set;

	public SubscriberInputSyncInfoSetExtension(SubscriberInput input, IResource[] roots, SyncInfoFilter filter) {
		workingSetInput = new WorkingSetSyncSetInput(input.getSubscriberSyncSet());
		IWorkingSet workingSet = PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet("SubscriberInput", roots);
		workingSet.setElements(roots);
		workingSetInput.setWorkingSet(workingSet);
		
		if(filter != null) {
			filteredInput = new SyncSetInputFromSyncSet(workingSetInput.getSyncSet());
			filteredInput.setFilter(filter);
			try {
				filteredInput.reset(null);
			} catch (TeamException e) {
			}
			set = filteredInput.getSyncSet();
		} else {		
			set = workingSetInput.getSyncSet();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.sets.SyncInfoSetDelegator#getSyncInfoSet()
	 */
	protected ISyncInfoSet getSyncInfoSet() {
		return set;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISyncInfoSet#dispose()
	 */
	public void dispose() {
		super.dispose();
		workingSetInput.disconnect();
		if(filteredInput != null) {
			filteredInput.disconnect();
		}
	}
}
