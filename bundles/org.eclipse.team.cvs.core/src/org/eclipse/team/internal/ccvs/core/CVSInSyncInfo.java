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
package org.eclipse.team.internal.ccvs.core;

import org.eclipse.core.resources.IFile;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.IResourceVariant;

/**
 * Optimization to delya the loading of resource sync info for in-sync files
 */
public class CVSInSyncInfo extends CVSSyncInfo {

	/**
	 * Create an in-sync sync info for the given file
	 */
	public CVSInSyncInfo(IFile local, CVSWorkspaceSubscriber subscriber) {
		// The base and remote are initialized to null but will be returned if queried
		super(local, null, null, subscriber);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.synchronize.SyncInfo#calculateKind()
	 */
	protected int calculateKind() throws TeamException {
		return IN_SYNC;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.synchronize.SyncInfo#getBase()
	 */
	public IResourceVariant getBase() {
		CVSWorkspaceSubscriber subscriber = (CVSWorkspaceSubscriber)getSubscriber();
		try {
			return subscriber.getBaseResource(getLocal());
		} catch (TeamException e) {
			CVSProviderPlugin.log(e);
			return super.getBase();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.synchronize.SyncInfo#getRemote()
	 */
	public IResourceVariant getRemote() {
		CVSWorkspaceSubscriber subscriber = (CVSWorkspaceSubscriber)getSubscriber();
		try {
			return subscriber.getRemoteResource(getLocal());
		} catch (TeamException e) {
			CVSProviderPlugin.log(e);
			return super.getRemote();
		}
	}
}
