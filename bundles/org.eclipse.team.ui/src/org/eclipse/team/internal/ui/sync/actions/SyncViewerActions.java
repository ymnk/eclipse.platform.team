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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.core.Policy;
import org.eclipse.team.internal.ui.UIConstants;
import org.eclipse.team.internal.ui.sync.views.*;
import org.eclipse.team.ui.TeamImages;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionContext;

/**
 * This class managers the actions associated with the SyncViewer class.
 */
public class SyncViewerActions extends SyncViewerActionGroup {
		
	// action groups for view filtering
	private SyncViewerDirectionFilters directionsFilters;
	private SyncViewerChangeFilters changeFilters;
	private SyncViewerComparisonCriteria comparisonCriteria;
	
	// other view actions
	private Action refreshAction;
	private Action toggleViewerType;
	private Action open;
	
	private IMenuManager actionBarMenu;
	
	class RefreshAction extends Action {
		public RefreshAction() {
			setText("Refresh with Repository");
			setToolTipText("Refresh the selected resources with the repository");
			setImageDescriptor(TeamImages.getImageDescriptor(UIConstants.IMG_REFRESH_ENABLED));
			setDisabledImageDescriptor(TeamImages.getImageDescriptor(UIConstants.IMG_REFRESH_DISABLED));
			setHoverImageDescriptor(TeamImages.getImageDescriptor(UIConstants.IMG_REFRESH));
		}
		public void run() {
			final SyncViewer view = getSyncView();
			view.run(new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							try {
								monitor.beginTask(null, 100);
								ActionContext context = getContext();
								SubscriberInput input = (SubscriberInput)context.getInput();
								IResource[] resources = input.getSubscriber().roots();
								input.getSubscriber().refresh(resources, IResource.DEPTH_INFINITE, Policy.subMonitorFor(monitor, 100));
								view.refreshViewer();
							} catch (TeamException e) {
								throw new InvocationTargetException(e);
							} finally {
								monitor.done();
							}
						}
				});
		}
	}
	
	class ToggleViewAction extends Action {
		public ToggleViewAction(int initialState) {
			setText("Toggle Tree/Table");
			setToolTipText("Toggle Tree/Table");
			setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
						getImageDescriptor(org.eclipse.ui.ISharedImages.IMG_TOOL_COPY));
			setChecked(initialState == SyncViewer.TREE_VIEW);
		}
		public void run() {
			int viewerType;
			if(toggleViewerType.isChecked()) {
				viewerType = SyncViewer.TREE_VIEW;							
			} else {
				viewerType = SyncViewer.TABLE_VIEW;
			}
			getSyncView().switchViewerType(viewerType);
		}
	}
		
	public SyncViewerActions(SyncViewer viewer) {
		super(viewer);
		createActions();
	}
	
	private void createActions() {
		// initialize action groups
		SyncViewer syncView = getSyncView();
		directionsFilters = new SyncViewerDirectionFilters(syncView, this);
		changeFilters = new SyncViewerChangeFilters(syncView, this);
		comparisonCriteria = new SyncViewerComparisonCriteria(syncView);
		
		// initialize other actions
		refreshAction = new RefreshAction();
		refreshAction.setEnabled(false);
		
		toggleViewerType = new ToggleViewAction(SyncViewer.TABLE_VIEW);
		open = new OpenInCompareAction(syncView);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.ActionGroup#fillActionBars(org.eclipse.ui.IActionBars)
	 */
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		
		IToolBarManager manager = actionBars.getToolBarManager();
		directionsFilters.fillActionBars(actionBars);
		manager.add(new Separator());
		manager.add(refreshAction);
		manager.add(new Separator());
		manager.add(toggleViewerType);
		
		actionBarMenu = actionBars.getMenuManager();
		actionBarMenu.removeAll();
		actionBarMenu.add(new Separator());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.ActionGroup#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	public void fillContextMenu(IMenuManager manager) {
		super.fillContextMenu(manager);
		
		manager.add(open);
		manager.add(new Separator());
		manager.add(refreshAction);
		manager.add(new Separator());
		manager.add(toggleViewerType);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator("Additions"));
	}

	public void refreshFilters() {
		final SubscriberInput input = getSubscriberContext();
		if(input != null) {
			getSyncView().run(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						monitor.beginTask(null, 100);
						input.setFilter(new AndSyncSetFilter(
						new SyncSetFilter[] {
							new SyncSetDirectionFilter(directionsFilters.getDirectionFilters()), 
							new SyncSetChangeFilter(changeFilters.getChangeFilters())
						}), monitor);
					} catch (TeamException e) {
						throw new InvocationTargetException(e);
					} finally {
						monitor.done();
					}
					}
				});
		}
	}
	
	public void open() {
		open.run();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.team.ccvs.syncviews.actions.SyncViewerActionGroup#restore(org.eclipse.ui.IMemento)
	 */
	public void restore(IMemento memento) {
		if(memento == null) return;
		super.restore(memento);
		changeFilters.restore(memento);
		directionsFilters.restore(memento);
		comparisonCriteria.restore(memento);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ccvs.syncviews.actions.SyncViewerActionGroup#save(org.eclipse.ui.IMemento)
	 */
	public void save(IMemento memento) {
		if(memento == null) return;
		super.save(memento);
		changeFilters.save(memento);
		directionsFilters.save(memento);
		comparisonCriteria.save(memento);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.sync.actions.SyncViewerActionGroup#initializeActions()
	 */
	protected void initializeActions() {
		SubscriberInput input = getSubscriberContext();
		refreshAction.setEnabled(input != null);
		if(input == null) {
			actionBarMenu.removeAll();
			actionBarMenu.add(new Separator());
		} else {
			actionBarMenu.removeAll();
			comparisonCriteria.fillContextMenu(actionBarMenu);
			actionBarMenu.add(new Separator());
			changeFilters.fillContextMenu(actionBarMenu);
			getSyncView().redraw();
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.ActionGroup#setContext(org.eclipse.ui.actions.ActionContext)
	 */
	public void setContext(ActionContext context) {
		changeFilters.setContext(context);
		directionsFilters.setContext(context);
		comparisonCriteria.setContext(context);
		super.setContext(context);
	}
}
