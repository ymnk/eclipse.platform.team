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
package org.eclipse.team.internal.ccvs.ui.subscriber;

import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.sync.SyncInfo;
import org.eclipse.team.internal.ccvs.core.CVSSyncInfo;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ui.sync.views.SyncResource;
import org.eclipse.team.ui.sync.SubscriberAction;
import org.eclipse.team.ui.sync.SyncInfoFilter;

/**
 * @author Administrator
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public abstract class CVSSubscriberAction extends SubscriberAction {

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.SubscriberAction#getSyncInfoFilter()
	 */
	protected SyncInfoFilter getSyncInfoFilter() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * @param element
	 */
	protected void makeInSync(SyncResource element) throws TeamException {
		SyncInfo info = element.getSyncInfo();
		if (info == null) return;
		if (info instanceof CVSSyncInfo) {
			CVSSyncInfo cvsInfo= (CVSSyncInfo) info;
			cvsInfo.makeInSync();
		}
		
	}
	
	/**
	 * Sycn actions seem to need to be sync-execed to work
	 * @param t
	 */
	protected void handle(Throwable t) {
		CVSUIPlugin.openError(getShell(), getErrorTitle(), null, t, CVSUIPlugin.PERFORM_SYNC_EXEC | CVSUIPlugin.LOG_NONTEAM_EXCEPTIONS);
	}

	protected String getErrorTitle() {
		return null;
	}
}
