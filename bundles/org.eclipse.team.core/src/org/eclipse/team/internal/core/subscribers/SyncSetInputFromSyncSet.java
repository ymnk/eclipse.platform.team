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
package org.eclipse.team.internal.core.subscribers;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.internal.core.TeamPlugin;

/**
 * Ths class uses the contents of one sync set as the input of another.
 */
public class SyncSetInputFromSyncSet extends SyncSetInput implements ISyncSetChangedListener {

	SyncInfoSet inputSyncSet;

	public SyncSetInputFromSyncSet(SyncInfoSet set) {
		this.inputSyncSet = set;
		inputSyncSet.addSyncSetChangedListener(this);
	}

	public SyncInfoSet getInputSyncSet() {
		return inputSyncSet;
	}
	
	public void disconnect() {
		if (inputSyncSet == null) return;
		inputSyncSet.removeSyncSetChangedListener(this);
		inputSyncSet = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ccvs.syncviews.views.AbstractSyncSet#initialize(java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void fetchInput(IProgressMonitor monitor) throws TeamException {
		if (inputSyncSet == null) return;
		SyncInfo[] infos = inputSyncSet.getSyncInfos();
		for (int i = 0; i < infos.length; i++) {
			collect(infos[i], monitor);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ccvs.syncviews.views.ISyncSetChangedListener#syncSetChanged(org.eclipse.team.ccvs.syncviews.views.SyncSetChangedEvent)
	 */
	public void syncSetChanged(ISyncInfoSetChangeEvent event, IProgressMonitor monitor) {
		MutableSyncInfoSet syncSet = getSyncSet();
		try {
			syncSet.beginInput();
			if (event.isReset()) {
				syncSet.clear();
				try {
					fetchInput(monitor);
				} catch (TeamException e) {
					// TODO: this could be bad
					TeamPlugin.log(e);
				}
			} else {
				syncSetChanged(event.getChangedResources(), monitor);			
				syncSetChanged(event.getAddedResources(), monitor);
				remove(event.getRemovedResources());
			}
		} finally {
			getSyncSet().endInput(monitor);
		}
	}

	private void syncSetChanged(SyncInfo[] infos, IProgressMonitor monitor) {
		for (int i = 0; i < infos.length; i++) {
			collect(infos[i], monitor);
		}
	}
	
	private void remove(IResource[] resources) {
		for (int i = 0; i < resources.length; i++) {
			remove(resources[i]);
		}
	}
}
