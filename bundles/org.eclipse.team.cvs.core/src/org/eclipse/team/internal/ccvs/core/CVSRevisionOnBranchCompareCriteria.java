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

import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;

/**
 * Specialized revision comparison that only fails the comparison when the remote revision is a later
 * revision on the same branch as the local resource.
 */
public class CVSRevisionOnBranchCompareCriteria extends CVSRevisionNumberCompareCriteria {

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.CVSRevisionNumberCompareCriteria#compare(byte[], byte[])
	 */
	protected boolean compare(byte[] localBytes, byte[] remoteBytes) throws CVSException {
		// First, check if the revisions are the same
		if (super.compare(localBytes, remoteBytes)) return true;
		// Only fail the compare if the remote bytes are on the same branch but a later revision.
		// This is done to ignore stale sync bytes in the cache
		return !ResourceSyncInfo.isLaterRevisionOnSameBranch(remoteBytes, localBytes);
	}

}
