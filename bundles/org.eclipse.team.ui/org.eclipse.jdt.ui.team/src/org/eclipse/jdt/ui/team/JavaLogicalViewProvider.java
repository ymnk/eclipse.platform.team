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
package org.eclipse.jdt.ui.team;

import org.eclipse.team.internal.ui.synchronize.views.SyncSetContentProvider;
import org.eclipse.team.ui.synchronize.content.LogicalViewProvider;
import org.eclipse.team.ui.synchronize.content.SyncInfoLabelProvider;

/**
 * The Java logical view provider
 */
public class JavaLogicalViewProvider extends LogicalViewProvider {

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.content.LogicalViewProvider#getContentProvider()
	 */
	public SyncSetContentProvider getContentProvider() {
		return new JavaSyncInfoSetContentProvider();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.content.LogicalViewProvider#getLabelProvider()
	 */
	public SyncInfoLabelProvider getLabelProvider() {
		return new JavaSyncInfoLabelProvider();
	}

}
