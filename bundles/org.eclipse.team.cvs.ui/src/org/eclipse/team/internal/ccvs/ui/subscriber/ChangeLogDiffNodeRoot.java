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

import org.eclipse.team.core.subscribers.SyncInfoSet;
import org.eclipse.team.ui.synchronize.SyncInfoDiffNodeRoot;
import org.eclipse.team.ui.synchronize.views.SyncInfoDiffNodeBuilder;

public class ChangeLogDiffNodeRoot extends SyncInfoDiffNodeRoot {

	public ChangeLogDiffNodeRoot(SyncInfoSet set) {
		super(set);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncInfoDiffNodeRoot#createBuilder()
	 */
	protected SyncInfoDiffNodeBuilder createBuilder() {
		return new ChangeLogDiffNodeBuilder(this);
	}
}
