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
import org.eclipse.team.core.subscribers.*;

/**
 * Ths class uses the contents of one sync set as the input of another.
 */
public class SyncSetInputFromSyncSet extends SyncSetInput implements ISyncInfoSetChangeListener2 {

	SyncInfoSet inputSyncSet;

	public SyncSetInputFromSyncSet(SyncInfoSet set, SubscriberEventHandler handler) {
		super(handler);
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
	protected void fetchInput(IProgressMonitor monitor) {
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
		SyncInfoSet syncSet = getSyncSet();
		try {
			syncSet.beginInput();
			if (event.isReset()) {
				syncSet.clear();
				fetchInput(monitor);
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

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.core.subscribers.ISyncInfoSetChangeListener2#handleError(org.eclipse.team.internal.core.subscribers.SubscriberErrorEvent, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void handleError(SubscriberErrorEvent event, IProgressMonitor monitor) {
		getSyncSet().handleErrorEvent(event, monitor);
	}
	
	public void reset() {
		getSyncSet().removeSyncSetChangedListener(this);
		getSyncSet().connect(this);
	}
}
