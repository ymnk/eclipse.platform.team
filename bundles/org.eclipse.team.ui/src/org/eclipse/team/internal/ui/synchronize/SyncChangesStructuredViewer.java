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

import java.util.Iterator;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.actions.*;
import org.eclipse.team.internal.ui.synchronize.sets.SyncSet;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.IWorkbenchActionConstants;

public abstract class SyncChangesStructuredViewer extends SyncChangesViewer {

	private TeamSubscriberParticipant participant;
	//private ISynchronizeView view;
	
	private OpenWithActionGroup openWithActions;
	private RefactorActionGroup refactorActions;
	private RefreshAction refreshSelectionAction;
	private Action expandAll;
	
	public SyncChangesStructuredViewer(TeamSubscriberParticipant participant, SyncSet set) {
		super(participant, set);
		this.participant = participant;		
	}
	
	protected void createActions() {
		// context menus
		//openWithActions = new OpenWithActionGroup(view, participant);
		//refactorActions = new RefactorActionGroup(view.getSite().getPage().getActivePart());
		//refreshSelectionAction = new RefreshAction(view.getSite().getPage(), participant, false /* refresh all */);
		expandAll = new Action() {
			public void run() {
				Viewer viewer = getViewer();
				ISelection selection = viewer.getSelection();
				if(viewer instanceof AbstractTreeViewer && ! selection.isEmpty()) {
					Iterator elements = ((IStructuredSelection)selection).iterator();
					while (elements.hasNext()) {
						Object next = elements.next();
						((AbstractTreeViewer) viewer).expandToLevel(next, AbstractTreeViewer.ALL_LEVELS);
					}
				}
			}
		};
		Utils.initAction(expandAll, "action.expandAll."); //$NON-NLS-1$
	}
	
	private void handleOpen(OpenEvent event) {
		openWithActions.openInCompareEditor();
	}
	
	protected void fillContextMenu(IMenuManager manager) {	
		//openWithActions.fillContextMenu(manager);
		//refactorActions.fillContextMenu(manager);
		//manager.add(refreshSelectionAction);
		manager.add(new Separator());
		manager.add(expandAll);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	protected void initializeListeners() {
		Viewer viewer = getViewer();
		getViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateStatusLine((IStructuredSelection)event.getSelection());
			}
		});
		if(viewer instanceof StructuredViewer) {
			StructuredViewer structuredViewer = (StructuredViewer)viewer;
			structuredViewer.addDoubleClickListener(new IDoubleClickListener() {
				public void doubleClick(DoubleClickEvent event) {
					handleDoubleClick(event);
				}
			});
			structuredViewer.addOpenListener(new IOpenListener() {
				public void open(OpenEvent event) {
					handleOpen(event);
				}
			});
		}
	}
	
	/**
	 * Handles a double-click event from the viewer.
	 * Expands or collapses a folder when double-clicked.
	 * 
	 * @param event the double-click event
	 */
	private void handleDoubleClick(DoubleClickEvent event) {
		IStructuredSelection selection = (IStructuredSelection) event.getSelection();
		Object element = selection.getFirstElement();	
		// Double-clicking should expand/collapse containers
		Viewer viewer = getViewer();
		if (viewer instanceof TreeViewer) {
			TreeViewer tree = (TreeViewer)viewer;
			if (tree.isExpandable(element)) {
				tree.setExpandedState(element, !tree.getExpandedState(element));
			}
		}		
	}
	
	/**
	 * Updates the message shown in the status line.
	 *
	 * @param selection the current selection
	 */
	private void updateStatusLine(IStructuredSelection selection) {
		String msg = getStatusLineMessage(selection);
		//view.getViewSite().getActionBars().getStatusLineManager().setMessage(msg);
	}
	
	/**
	 * Returns the message to show in the status line.
	 *
	 * @param selection the current selection
	 * @return the status line message
	 * @since 2.0
	 */
	private String getStatusLineMessage(IStructuredSelection selection) {
		if (selection.size() == 1) {
			Object first = selection.getFirstElement();
			if(first instanceof ITeamSubscriberParticipantNode) {
				SyncInfo info = ((ITeamSubscriberParticipantNode)first).getSyncInfo();
				if (info == null) {
					return Policy.bind("SynchronizeView.12"); //$NON-NLS-1$
				} else {
					return info.getLocal().getFullPath().makeRelative().toString();
				}
			}
		}
		if (selection.size() > 1) {
			return selection.size() + Policy.bind("SynchronizeView.13"); //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
	}
}