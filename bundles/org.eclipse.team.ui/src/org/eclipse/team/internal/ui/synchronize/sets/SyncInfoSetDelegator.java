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
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.ui.synchronize.ISyncInfoSet;
import org.eclipse.team.ui.synchronize.ISyncSetChangedListener;

public abstract class SyncInfoSetDelegator implements ISyncInfoSet {

	protected abstract ISyncInfoSet getSyncInfoSet();
	
	public void addSyncSetChangedListener(ISyncSetChangedListener listener) {
		getSyncInfoSet().addSyncSetChangedListener(listener);
	}

	public void removeSyncSetChangedListener(ISyncSetChangedListener listener) {
		getSyncInfoSet().removeSyncSetChangedListener(listener);
	}

	public IResource[] members(IResource resource) {
		return getSyncInfoSet().members(resource);
	}

	public SyncInfo[] getOutOfSyncDescendants(IResource resource) {
		return getSyncInfoSet().getOutOfSyncDescendants(resource);
	}

	public SyncInfo[] members() {
		return getSyncInfoSet().members();
	}

	public SyncInfo getSyncInfo(IResource resource) {
		return getSyncInfoSet().getSyncInfo(resource);
	}

	public int size() {
		return getSyncInfoSet().size();
	}

	public boolean hasMembers(IResource resource) {
		return getSyncInfoSet().hasMembers(resource);
	}
	public void dispose() {
		// nothing to dispose!
	}
}
