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

import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.synchronize.actions.*;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.team.ui.synchronize.actions.INavigableTree;
import org.eclipse.ui.IWorkbenchActionConstants;

/**
 * Overrides the SyncInfoDiffViewerConfiguration to configure the diff viewer for the synchroniza view
 */
public class SynchronizeViewCompareConfiguration extends SyncInfoSetCompareConfiguration {

	private ISynchronizeView view;
	private TeamSubscriberParticipant participant;
	
	private OpenWithActionGroup openWithActions;
	private RefactorActionGroup refactorActions;
	private TeamParticipantRefreshAction refreshSelectionAction;
	
	public SynchronizeViewCompareConfiguration(ISynchronizeView view, TeamSubscriberParticipant participant) {
		super(participant.getId(), participant.getFilteredSyncInfoCollector().getSyncInfoSet());
		this.view = view;
		this.participant = participant;
	}
	
	public StructuredViewer createViewer(Composite parent) {
		final StructuredViewer treeViewer = new SyncInfoDiffTreeViewer(parent, this);

		openWithActions = new OpenWithActionGroup(view, participant);
		refactorActions = new RefactorActionGroup(view.getSite().getPage().getActivePart());
		refreshSelectionAction = new TeamParticipantRefreshAction(treeViewer, participant, false /*refresh*/);
		return treeViewer;
	}
	
	protected void fillContextMenu(StructuredViewer viewer, IMenuManager manager) {
		openWithActions.fillContextMenu(manager);
		refactorActions.fillContextMenu(manager);
		manager.add(refreshSelectionAction);
		manager.add(new Separator());
		addLogicalViewSelection(viewer, manager);
		manager.add(new Separator());
		getNavigator().fillContextMenu(viewer, manager);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.ui.synchronize.SyncInfoDiffTreeViewer#handleDoubleClick(org.eclipse.jface.viewers.DoubleClickEvent)
	 */
	protected void handleDoubleClick(StructuredViewer viewer, DoubleClickEvent event) {
		IStructuredSelection selection = (IStructuredSelection) event.getSelection();
		DiffNode node = (DiffNode) selection.getFirstElement();
		if (node != null && node instanceof SyncInfoDiffNode) {
			SyncInfoDiffNode syncNode = (SyncInfoDiffNode)node; 
			SyncInfo info = syncNode.getSyncInfo();
			if (syncNode != null 
					&& syncNode.getResource() != null 
					&& syncNode.getResource().getType() == IResource.FILE) {
				openWithActions.openInCompareEditor();
				return;
			}
		}
		// Double-clicking should expand/collapse containers
		super.handleDoubleClick(viewer, event);
	}
	
	protected void initializeListeners(StructuredViewer viewer) {
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateStatusLine((IStructuredSelection) event.getSelection());
			}
		});
		viewer.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				handleOpen();
			}
		});
		super.initializeListeners(viewer);
	}
	
	protected void handleOpen() {
		openWithActions.openInCompareEditor();
	}
	
	/**
	 * Updates the message shown in the status line.
	 * 
	 * @param selection
	 *            the current selection
	 */
	private void updateStatusLine(IStructuredSelection selection) {
		String msg = getStatusLineMessage(selection);
		view.getViewSite().getActionBars().getStatusLineManager().setMessage(msg);
	}

	/**
	 * Returns the message to show in the status line.
	 * 
	 * @param selection
	 *            the current selection
	 * @return the status line message
	 * @since 2.0
	 */
	private String getStatusLineMessage(IStructuredSelection selection) {
		if (selection.size() == 1) {
			Object first = selection.getFirstElement();
			if (first instanceof SyncInfoDiffNode) {
				SyncInfo info = ((SyncInfoDiffNode) first).getSyncInfo();
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncInfoSetCompareConfiguration#initializeNavigation(org.eclipse.swt.widgets.Control, org.eclipse.team.internal.ui.synchronize.SyncInfoDiffTreeNavigator.INavigationTarget)
	 */
	protected void initializeNavigation(Control tree, INavigableTree target) {
		super.initializeNavigation(tree, target);
		getNavigator().setShowOpenAction(false);
	}
}
