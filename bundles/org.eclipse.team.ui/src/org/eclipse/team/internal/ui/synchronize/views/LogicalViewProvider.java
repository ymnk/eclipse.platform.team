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
package org.eclipse.team.internal.ui.synchronize.views;

import org.eclipse.team.ui.synchronize.DiffTreeViewerConfiguration;
import org.eclipse.team.ui.synchronize.views.*;
import org.eclipse.ui.views.navigator.ResourceSorter;

/**
 * Implementations can be contributed via extension point and used by team participants.
 * Logical Providers can be of two types:
 * 1. nature based (are applied directly to projects
 * 2. global based
 * 
 * TODO: Add project nature filter to extension point
 * 
 * @since 3.0
 */
public abstract class LogicalViewProvider {

	private DiffTreeViewerConfiguration configuration;

	public LogicalViewProvider(DiffTreeViewerConfiguration configuration) {
		this.configuration = configuration;
	}
	
	/**
	 * Return a content provider that can be used in a <code>TreeViewer</code>
	 * to show the hiearchical structure appropriate for this provider.
	 * @return a <code>SyncInfoSetTreeContentProvider</code>
	 */
	public abstract SyncInfoSetTreeContentProvider getContentProvider();
	
	/**
	 * Return a label provider that provides the text and image labels for
	 * the logical elements associated with this provider.
	 * @return
	 */
	public abstract SyncInfoLabelProvider getLabelProvider();
	
	/**
	 * Return the sorter to be used to sort elements from the logical view's
	 * content provider.
	 * @return a <code>SyncViewerSorter</code>
	 */
	public SyncViewerSorter getSorter() {
		return new SyncViewerSorter(ResourceSorter.NAME);
	}
	
	protected DiffTreeViewerConfiguration getConfiguration() {
		return configuration;
	}

	public void dispose() {
	}
}
