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
package org.eclipse.team.ui.synchronize.content;

import org.eclipse.ui.views.navigator.ResourceSorter;

/**
 * Implementations can be contributed via extension point and used by team participants.
 */
public abstract class LogicalViewProvider {

	public abstract SyncInfoSetContentProvider getContentProvider();
	
	public abstract SyncInfoLabelProvider getLabelProvider();
	
	/**
	 * Return the sorter to be used to sort elements from the logical view's
	 * content provider.
	 * @return a <code>SyncViewerSorter</code>
	 */
	public SyncViewerSorter getSorter() {
		return new SyncViewerSorter(ResourceSorter.NAME);
	}
}
