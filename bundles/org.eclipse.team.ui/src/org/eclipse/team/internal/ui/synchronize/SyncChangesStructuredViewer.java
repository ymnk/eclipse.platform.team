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
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.actions.*;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.internal.PluginAction;

public abstract class SyncChangesStructuredViewer extends SyncChangesViewer {

	private TeamSubscriberParticipant participant;
	private ISynchronizeView view;
	
	private OpenWithActionGroup openWithActions;
	private RefactorActionGroup refactorActions;
	private RefreshAction refreshSelectionAction;
	private Action expandAll;
	
	public SyncChangesStructuredViewer(TeamSubscriberParticipant participant, ISynchronizeView view) {
		super(participant, view);
		this.view = view;
		this.participant = participant;		
	}
	
	protected void createActions() {
		// context menus
		openWithActions = new OpenWithActionGroup(view, participant);
		refactorActions = new RefactorActionGroup(view.getSite().getPage().getActivePart());
		refreshSelectionAction = new RefreshAction(view.getSite().getPage(), participant, false /* refresh all */);
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
	
	protected void hookContextMenu() {
		if(getViewer() != null) {
			final MenuManager menuMgr = new MenuManager(participant.getId()); //$NON-NLS-1$
			menuMgr.setRemoveAllWhenShown(true);
			menuMgr.addMenuListener(new IMenuListener() {
				public void menuAboutToShow(IMenuManager manager) {
					setContextMenu(manager);
				}
			});
			Menu menu = menuMgr.createContextMenu(getViewer().getControl());
			menu.addMenuListener(new MenuListener() {
				public void menuHidden(MenuEvent e) {
				}
				// Hack to allow action contributions to update their
				// state before the menu is shown. This is required when
				// the state of the selection changes and the contributions
				// need to update enablement based on this. 
				public void menuShown(MenuEvent e) {
					IContributionItem[] items = menuMgr.getItems();
					for (int i = 0; i < items.length; i++) {
						IContributionItem item = items[i];
						if(item instanceof ActionContributionItem) {
							IAction actionItem = ((ActionContributionItem)item).getAction();
							if(actionItem instanceof PluginAction) {
								((PluginAction)actionItem).selectionChanged(getViewer().getSelection());
							}
						}
					}
				}
			});
			getViewer().getControl().setMenu(menu);			
			view.getSite().registerContextMenu(participant.getId(), menuMgr, getViewer());
		}
	}
	
	protected void setContextMenu(IMenuManager manager) {	
		openWithActions.fillContextMenu(manager);
		refactorActions.fillContextMenu(manager);
		manager.add(refreshSelectionAction);
		manager.add(new Separator());
		manager.add(expandAll);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	public abstract StructuredViewer getViewer();
	
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
	
	private void handleOpen(OpenEvent event) {
		openWithActions.openInCompareEditor();
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
		view.getViewSite().getActionBars().getStatusLineManager().setMessage(msg);
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