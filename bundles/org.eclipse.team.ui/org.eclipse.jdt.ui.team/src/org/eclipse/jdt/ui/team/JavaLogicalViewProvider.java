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

import org.eclipse.team.internal.ui.synchronize.views.*;
import org.eclipse.team.ui.synchronize.DiffTreeViewerConfiguration;
import org.eclipse.team.ui.synchronize.views.*;

/**
 * The Java logical view provider
 */
public class JavaLogicalViewProvider extends LogicalViewProvider {

	public JavaLogicalViewProvider(DiffTreeViewerConfiguration configuration) {
		super(configuration);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.content.LogicalViewProvider#getContentProvider()
	 */
	public SyncInfoSetTreeContentProvider getContentProvider() {
		return new JavaSyncInfoSetContentProvider();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.content.LogicalViewProvider#getLabelProvider()
	 */
	public SyncInfoLabelProvider getLabelProvider() {
		return new JavaSyncInfoLabelProvider();
	}

}
