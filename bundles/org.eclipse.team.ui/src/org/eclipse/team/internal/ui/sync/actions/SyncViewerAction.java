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
package org.eclipse.team.internal.ui.sync.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ui.sync.views.SyncViewer;

/**
 * This class acts as the superclass for all actions in the SyncViewer
 */
public abstract class SyncViewerAction extends Action {

	private SyncViewer syncView;

	/**
	 * @param text
	 */
	public SyncViewerAction(SyncViewer viewer, String label) {
		super(label);
		this.syncView = viewer;
	}

	/**
	 * @return
	 */
	public SyncViewer getSyncView() {
		return syncView;
	}

	public Shell getShell() {
		return syncView.getSite().getShell();
	}
	
	public Viewer getViewer() {
		return syncView.getViewer();
	}
}
