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
import org.eclipse.team.internal.ui.sync.views.SyncViewer;
import org.eclipse.team.ui.TeamImages;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;

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
								IResource[] resources = getSyncView().getSubscriber().roots();
								getSyncView().getSubscriber().refresh(resources, IResource.DEPTH_INFINITE, Policy.subMonitorFor(monitor, 80));
								view.refreshViewerInput(Policy.subMonitorFor(monitor, 20));
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
		
	/**
	 * @param viewer
	 */
	public SyncViewerActions(SyncViewer viewer) {
		super(viewer);
		initializeActions();
	}
	
	private void initializeActions() {
		// initialize action groups
		SyncViewer syncView = getSyncView();
		directionsFilters = new SyncViewerDirectionFilters(syncView);
		changeFilters = new SyncViewerChangeFilters(syncView);
		comparisonCriteria = new SyncViewerComparisonCriteria(syncView, syncView.getAllComparisonCritera());
		// initialize other actions
		refreshAction = new RefreshAction();
		toggleViewerType = new ToggleViewAction(SyncViewer.TABLE_VIEW);
		open = new OpenInCompareAction(getSyncView());
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
		
		IMenuManager menu = actionBars.getMenuManager();
		comparisonCriteria.fillContextMenu(menu);
		menu.add(new Separator());
		changeFilters.fillContextMenu(menu);
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

	/**
	 * 
	 */
	public int[] getDirectionFilters() {
		return directionsFilters.getDirectionFilters();
	}

	/**
	 * 
	 */
	public int[] getChangeFilters() {
		return changeFilters.getChangeFilters();
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

}
