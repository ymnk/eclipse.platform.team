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

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.actions.*;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.IWorkbenchActionConstants;

public class SyncInfoDiffViewerForSynchronizeView extends SyncInfoDiffTreeViewer {

	private TeamSubscriberParticipant participant;

	//private ISynchronizeView view;

	private OpenWithActionGroup openWithActions;
	private RefactorActionGroup refactorActions;
	private RefreshAction refreshSelectionAction;
	private Action expandAll;
	private ISynchronizeView view;

	public SyncInfoDiffViewerForSynchronizeView(Composite parent, ISynchronizeView view, TeamSubscriberParticipant participant, ISyncInfoSet set) {
		super(parent, participant, set);
		this.view = view;
		this.participant = participant;

		openWithActions = new OpenWithActionGroup(view, participant);
		refactorActions = new RefactorActionGroup(view.getSite().getPage().getActivePart());
		refreshSelectionAction = new RefreshAction(view.getSite().getPage(), participant, false /*refresh*/);
		expandAll = new Action() {
			public void run() {
				expandAllFromSelection();
			}
		};
		Utils.initAction(expandAll, "action.expandAll."); //$NON-NLS-1$
		setAcceptParticipantMenuContributions(true);
	}

	private void handleOpen(OpenEvent event) {
		openWithActions.openInCompareEditor();
	}

	protected void fillContextMenu(IMenuManager manager) {
		openWithActions.fillContextMenu(manager);
		refactorActions.fillContextMenu(manager);
		manager.add(refreshSelectionAction);
		manager.add(new Separator());
		manager.add(expandAll);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	protected void initializeListeners() {
		addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateStatusLine((IStructuredSelection) event.getSelection());
			}
		});
		addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				handleOpen(event);
			}
		});
		addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				handleDoubleClick(event);
			}
		});
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
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.ui.synchronize.SyncInfoDiffTreeViewer#handleDoubleClick(org.eclipse.jface.viewers.DoubleClickEvent)
	 */
	protected void handleDoubleClick(DoubleClickEvent event) {
		IStructuredSelection selection = (IStructuredSelection) event.getSelection();
		SyncInfoDiffNode node = (SyncInfoDiffNode) selection.getFirstElement();
		if (node != null) {
			if (node.getResource().getType() == IResource.FILE) {
				openWithActions.openInCompareEditor();
				return;
			}
		}
		// Double-clicking should expand/collapse containers
		super.handleDoubleClick(event);
	}

}