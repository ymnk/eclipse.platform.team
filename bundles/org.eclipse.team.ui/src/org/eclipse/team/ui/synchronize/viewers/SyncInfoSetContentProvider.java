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
package org.eclipse.team.ui.synchronize.viewers;

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;

/**
 * This class provides the contents for a StructuredViewer using the <code>SyncInfo</code>
 * contained in a <code>SyncInfoSet</code> as the model.
 * 
 * @see SyncInfo
 * @see SyncInfoSet
 */
public class SyncInfoSetContentProvider extends BaseWorkbenchContentProvider {
	
	private Viewer viewer;
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		this.viewer = v;
		SyncInfoDiffNodeRoot oldNode = (SyncInfoDiffNodeRoot)oldInput;
		SyncInfoDiffNodeRoot newNode = (SyncInfoDiffNodeRoot)newInput;
		if (oldNode != newNode) {
			if (oldNode != null) {
				oldNode.dispose();
			}
			if (newNode != null) {
				newNode.setViewer((AbstractTreeViewer)v);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose() {
		Object input = viewer.getInput();
		if (input instanceof SyncInfoDiffNodeRoot) {
			((SyncInfoDiffNodeRoot)input).dispose();
		}
	}
}