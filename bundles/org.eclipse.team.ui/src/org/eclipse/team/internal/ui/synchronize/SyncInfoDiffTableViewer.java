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

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.synchronize.sets.SyncSet;
import org.eclipse.team.internal.ui.synchronize.views.SyncTableViewer;
import org.eclipse.team.internal.ui.synchronize.views.SyncViewerTableSorter;
import org.eclipse.team.ui.synchronize.TeamSubscriberParticipant;

public class SyncInfoDiffTableViewer extends TreeViewer {
	
	private SyncTableViewer viewer;
	
	public SyncInfoDiffTableViewer(Composite parent, TeamSubscriberParticipant participant, SyncSet set) {
		super(parent);
		//super(participant, set);
		// Create the table
//		Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
//		table.setHeaderVisible(true);
//		table.setLinesVisible(true);
//		GridData data = new GridData(GridData.FILL_BOTH);
//		table.setLayoutData(data);
//		
//		// Set the table layout
//		TableLayout layout = new TableLayout();
//		table.setLayout(layout);
//		
//		// Create the viewer
//		viewer = new SyncTableViewer(table);
//		
//		// Create the table columns
//		createColumns(table, layout, viewer);
//		
//		// Set the table contents
//		viewer.setContentProvider(new SyncSetTableContentProvider());
//		viewer.setSorter(new SyncViewerTableSorter());
//		viewer.setInput(getSyncSet());
	}
	
	/**
	 * Creates the columns for the sync viewer table.
	 */
	private void createColumns(Table table, TableLayout layout, TableViewer viewer) {
		SelectionListener headerListener = SyncViewerTableSorter.getColumnListener(viewer);
		// revision
		TableColumn col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(Policy.bind("TeamSubscriberParticipantPage.7")); //$NON-NLS-1$
		col.addSelectionListener(headerListener);
		layout.addColumnData(new ColumnWeightData(30, true));
		
		// tags
		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(Policy.bind("TeamSubscriberParticipantPage.8")); //$NON-NLS-1$
		col.addSelectionListener(headerListener);
		layout.addColumnData(new ColumnWeightData(50, true));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncChangesViewer#setContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	protected void setContextMenu(IMenuManager menu) {
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
		// TODO Auto-generated method stub	
	}	
}
