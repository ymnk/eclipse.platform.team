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
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.core.Policy;
import org.eclipse.team.internal.ui.UIConstants;
import org.eclipse.team.internal.ui.sync.views.SubscriberInput;
import org.eclipse.team.internal.ui.sync.views.SyncViewer;
import org.eclipse.team.ui.ISharedImages;
import org.eclipse.team.ui.TeamImages;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.WorkingSetFilterActionGroup;
import org.eclipse.team.ui.sync.*;

/**
 * This class managers the actions associated with the SyncViewer class.
 */
public class SyncViewerActions extends SyncViewerActionGroup {
		
	// action groups for view filtering
	private SyncViewerDirectionFilters directionsFilters;
	private SyncViewerChangeFilters changeFilters;
	private SyncViewerComparisonCriteria comparisonCriteria;
	private SyncViewerSubscriberListActions subscriberInputs;
	private SyncViewerSubscriberActions subscriberActions;
	
	private WorkingSetFilterActionGroup workingSetGroup;
	
	private SyncViewerToolbarDropDownAction chooseSubscriberAction;
	private ChooseComparisonCriteriaAction chooseComparisonCriteriaAction;
	
	private IWorkingSet workingSet;
	
	// other view actions
	private Action collapseAll;
	private RefreshAction refreshAction;
	private Action toggleViewerType;
	private Action open;
	
	class RefreshAction extends SyncViewerAction {
		public RefreshAction() {
			super (getSyncView(), "Refresh with Repository");
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
						IResource[] resources = input.roots();
						input.getSubscriber().refresh(resources, IResource.DEPTH_INFINITE, Policy.subMonitorFor(monitor, 100));
					} catch (TeamException e) {
						throw new InvocationTargetException(e);
					} finally {
						monitor.done();
					}
				}
			});
		}
	}
	
	class CollapseAllAction extends Action {
		public CollapseAllAction() {
			super("Collapse All", TeamImages.getImageDescriptor(ISharedImages.IMG_COLLAPSE_ALL_ENABLED));
			setToolTipText("Collapse all entries in the view");
			setHoverImageDescriptor(TeamImages.getImageDescriptor(ISharedImages.IMG_COLLAPSE_ALL));
		}
		public void run() {
			getSyncView().collapseAll();
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
	
	class ChooseSubscriberAction extends SyncViewerToolbarDropDownAction {
		public ChooseSubscriberAction(SyncViewerActionGroup actionGroup) {
			super(actionGroup);
			setText("Select Subscriber");
			setToolTipText("Select Subscriber");
			setImageDescriptor(TeamImages.getImageDescriptor(UIConstants.IMG_SITE_ELEMENT));
		}
		
	}
	
	class ChooseComparisonCriteriaAction extends SyncViewerToolbarDropDownAction {
		public ChooseComparisonCriteriaAction(SyncViewerActionGroup actionGroup) {
			super(actionGroup);
			setText("Select Comparison Criteria");
			setToolTipText("Select Comparison Criteria");
			setImageDescriptor(TeamImages.getImageDescriptor(UIConstants.IMG_CONTENTS));
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.ActionGroup#updateActionBars()
	 */
	public void updateActionBars() {
		super.updateActionBars();
		changeFilters.updateActionBars();
		directionsFilters.updateActionBars();
		comparisonCriteria.updateActionBars();
		subscriberInputs.updateActionBars();
		subscriberActions.updateActionBars();
		
		
		refreshAction.selectionChanged(getContext().getSelection());
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
		subscriberActions = new SyncViewerSubscriberActions(syncView);
		
		// initialize the dropdown for choosing a subscriber
		subscriberInputs = new SyncViewerSubscriberListActions(syncView);
		chooseSubscriberAction = new ChooseSubscriberAction(subscriberInputs);
		
		// initialize the dropdown for choosing a comparison criteria
		comparisonCriteria = new SyncViewerComparisonCriteria(syncView);
		chooseComparisonCriteriaAction = new ChooseComparisonCriteriaAction(comparisonCriteria);
		
		// initialize other actions
		refreshAction = new RefreshAction();
		refreshAction.setEnabled(false);
		collapseAll = new CollapseAllAction();
		
		toggleViewerType = new ToggleViewAction(SyncViewer.TABLE_VIEW);
		open = new OpenInCompareAction(syncView);
		
		IPropertyChangeListener workingSetUpdater = new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				String property = event.getProperty();
				
				if (WorkingSetFilterActionGroup.CHANGE_WORKING_SET.equals(property)) {
					Object newValue = event.getNewValue();
					
					if (newValue instanceof IWorkingSet) {	
						setWorkingSet((IWorkingSet) newValue);
					}
					else 
					if (newValue == null) {
						setWorkingSet(null);
					}
				}
			}
		};
		workingSetGroup = new WorkingSetFilterActionGroup(syncView.getSite().getShell(), workingSetUpdater);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.ActionGroup#fillActionBars(org.eclipse.ui.IActionBars)
	 */
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		
		IToolBarManager manager = actionBars.getToolBarManager();
		manager.add(chooseSubscriberAction);
		manager.add(chooseComparisonCriteriaAction);
		manager.add(new Separator());
		directionsFilters.fillActionBars(actionBars);
		manager.add(new Separator());
		manager.add(refreshAction);
		manager.add(collapseAll);
		manager.add(toggleViewerType);
		
		IMenuManager dropDownMenu = actionBars.getMenuManager();
		workingSetGroup.fillActionBars(actionBars);
		dropDownMenu.add(new Separator());
		changeFilters.fillContextMenu(dropDownMenu);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.ActionGroup#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	public void fillContextMenu(IMenuManager manager) {
		super.fillContextMenu(manager);
		
		manager.add(open);
		manager.add(new Separator());
		manager.add(refreshAction);
		// Subscriber menus go here
		subscriberActions.fillContextMenu(manager);
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
						input.setFilter(new AndSyncInfoFilter(
						new SyncInfoFilter[] {
							new SyncInfoDirectionFilter(directionsFilters.getDirectionFilters()), 
							new SyncInfoChangeTypeFilter(changeFilters.getChangeFilters())
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
		// This is invoked before the subscriber input is initialized
		if (input.getWorkingSet() == null) {
			// set the input to use the last selected working set
			input.setWorkingSet(getWorkingSet());
		} else {
			// set the menu to select the set from the input
			// the callback will not prepare the input since the set
			// for the input is the same as the one being passed to the menu
			workingSetGroup.setWorkingSet(getWorkingSet());
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.ActionGroup#setContext(org.eclipse.ui.actions.ActionContext)
	 */
	public void setContext(ActionContext context) {
		changeFilters.setContext(context);
		directionsFilters.setContext(context);
		comparisonCriteria.setContext(context);
		subscriberInputs.setContext(context);
		subscriberActions.setContext(context);
		
		// causes initializeActions to be called. Must be called after
		// setting the context for contained groups.
		super.setContext(context);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.ActionGroup#setContext(org.eclipse.ui.actions.ActionContext)
	 */
	public void addContext(ActionContext context) {
		subscriberInputs.addContext(context);
	}
	
	/*
	 * Get the selected working set from the subscriber input
	 * @return
	 */
	private IWorkingSet getWorkingSet() {
		SubscriberInput input = getSubscriberContext();
		// There's no subscriber input so use the last selected workingSet
		if (input == null) return workingSet;
		IWorkingSet set = input.getWorkingSet();
		// There's no subscriber working set so use the last selected workingSet
		if (set == null ) return workingSet;
		return set;
	}
	
	/**
	 * @param set
	 */
	protected void setWorkingSet(IWorkingSet set) {
		// Keep track of the last working set selected
		if (set != null) workingSet = set;
		final SubscriberInput input = getSubscriberContext();
		if (input == null) return;
		if (workingSetsEqual(input.getWorkingSet(), set)) return;
		input.setWorkingSet(set);
		getSyncView().run(new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					// when the working set changes, recalculate the entire sync set based on
					// the new input.
					input.prepareInput(monitor);
				} catch (TeamException e) {
					throw new InvocationTargetException(e);
				}
			}
		});
	}

	/**
	 * @param set
	 * @param set2
	 * @return
	 */
	private boolean workingSetsEqual(IWorkingSet set, IWorkingSet set2) {
		if (set == null && set2 == null) return true;
		if (set == null || set2 == null) return false;
		return set.equals(set2);
	}
}
