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
package org.eclipse.team.core.subscribers;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A <code>SyncInfoFilter</code> tests a <code>SyncInfo</code> for inclusion
 * in a <code>SyncInfoSet</code>.
 */
public abstract class SyncInfoFilter {
	
	/**
	 * Filter that selects those <code>SyncInfo</code> whose contents do not match.
	 */
	public static class ContentComparisonSyncInfoFilter extends SyncInfoFilter {
		ContentComparisonCriteria criteria = new ContentComparisonCriteria(false);
		public boolean select(SyncInfo info, IProgressMonitor monitor) {
			ISubscriberResource remote = info.getRemote();
			IResource local = info.getLocal();
			if (remote == null) return local.exists();
			if (!local.exists()) return true;
			return !criteria.compare(local, remote, monitor);
		}
	}
	
	/**
	 * Return true if the provided SyncInfo matches the filter.
	 * 
	 * @param info the sync info to be tested
	 * @param monitor a progress monitor
	 * @return
	 */
	public abstract boolean select(SyncInfo info, IProgressMonitor monitor);
	
}
