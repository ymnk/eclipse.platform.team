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

import org.eclipse.team.ui.synchronize.content.*;

/**
 * This implementation compresses folder paths that do not contain out-of-sync resource.
 */
public class CompressFolderView extends LogicalViewProvider {

	public static final String ID = "org.eclipse.team.ui.compressed-folder-view";
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.content.ILogicalSyncInfoSetView#getContentProvider()
	 */
	public SyncInfoSetTreeContentProvider getContentProvider() {
		return new CompressedFolderContentProvider();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.content.ILogicalSyncInfoSetView#getLabelProvider()
	 */
	public SyncInfoLabelProvider getLabelProvider() {
		return new CompressedFolderLabelProvider();
	}

}
