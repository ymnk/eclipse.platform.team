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
package org.eclipse.team.ui.synchronize.subscribers;

import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.TreeViewerAdvisor;

/**
 * Overrides the SyncInfoDiffViewerConfiguration to configure the diff viewer
 * for the synchroniza view
 */
public class SynchronizeViewerAdvisor extends TreeViewerAdvisor {

	/**
	 * @param configuration
	 */
	public SynchronizeViewerAdvisor(ISynchronizePageConfiguration configuration) {
		super(configuration);
		// TODO Auto-generated constructor stub
	}

}
