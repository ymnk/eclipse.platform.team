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
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.internal.ui.synchronize.views.SyncTreeViewer;
import org.eclipse.team.internal.ui.synchronize.views.SyncViewerSorter;
import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.eclipse.team.ui.synchronize.TeamSubscriberParticipant;
import org.eclipse.ui.views.navigator.ResourceSorter;

public class SyncChangesTreeViewer extends SyncChangesStructuredViewer {

	private SyncTreeViewer viewer;
	
	public SyncChangesTreeViewer(Composite parent, TeamSubscriberParticipant participant, ISynchronizeView view) {
		super(participant, view);
		GridData data = new GridData(GridData.FILL_BOTH);
		viewer = new SyncTreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setSorter(new SyncViewerSorter(ResourceSorter.NAME));
		((TreeViewer)viewer).getTree().setLayoutData(data);
		viewer.setInput(participant.getInput());
		
		createActions();
		hookContextMenu();
		initializeListeners();		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncChangesViewer#getViewer()
	 */
	public StructuredViewer getViewer() {
		return viewer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.actions.INavigableControl#gotoDifference(int)
	 */
	public boolean gotoDifference(int direction) {
		return viewer.gotoDifference(direction);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncChangesViewer#dispose()
	 */
	public void dispose() {
	}
}
