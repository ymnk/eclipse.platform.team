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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.sync.ComparisonCriteria;
import org.eclipse.team.internal.ui.sync.views.SubscriberInput;
import org.eclipse.team.internal.ui.sync.views.SyncViewer;

/**
 * This action group allows the user to choose one or more comparison critera
 * to be applied to a comparison
 */
public class SyncViewerComparisonCriteria extends SyncViewerActionGroup {
	
	private static final String MEMENTO_KEY = "SelectedComparisonCriteria";

	private ComparisonCriteria[] criteria;
	private ComparisonCriteriaAction[] actions;

	/**
	 * Action for filtering by change type.
	 */
	class ComparisonCriteriaAction extends Action {
		private ComparisonCriteria criteria;
		public ComparisonCriteriaAction(ComparisonCriteria criteria) {
			super(criteria.getName());
			this.criteria = criteria;
		}
		public void run() {
			SyncViewerComparisonCriteria.this.activate(this);
		}
		public ComparisonCriteria getComparisonCriteria() {
			return criteria;
		}
	}
	
	public SyncViewerComparisonCriteria(SyncViewer syncView) {
		super(syncView);
		setContext(null);
	}
	
	/**
	 * @param action
	 */
	public void activate(ComparisonCriteriaAction activatedAction) {
		for (int i = 0; i < actions.length; i++) {
			ComparisonCriteriaAction action = actions[i];
			action.setChecked(activatedAction == action);
		}
		final SyncViewer view = getSyncView();
		view.run(new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					// when the comparison criteria changes, recalculate the entire sync set based on
					// the new input.
					view.getInput().prepareInput(monitor);
				} catch (TeamException e) {
					throw new InvocationTargetException(e);
				}
			}
		});
	}

	public void initializeActions() {
		SubscriberInput input = getSubscriberContext(); 
		if(input != null) {
			this.criteria = input.getSubscriber().getComparisonCriterias();
			this.actions = new ComparisonCriteriaAction[criteria.length];
			for (int i = 0; i < criteria.length; i++) {
				ComparisonCriteria c = criteria[i];
				actions[i] = new ComparisonCriteriaAction(c);
				actions[i].setChecked(c == getSyncView().getInput().getSubscriber().getCurrentComparisonCriteria());
			}
		} else {
			// there aren't any comparison criterias to show!
			this.actions = null;
			this.criteria = null;
			
		}
	}

	private ComparisonCriteria[] getActiveComparisonCriteria() {
		List result = new ArrayList();
		if(getSyncView().getInput() != null) {
			for (int i = 0; i < actions.length; i++) {
				ComparisonCriteriaAction action = actions[i];
				if (action.isChecked()) {
					result.add(action.getComparisonCriteria());
				}
			}
		}
		return (ComparisonCriteria[]) result.toArray(new ComparisonCriteria[result.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.ActionGroup#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		if(getSubscriberContext() != null) {
			for (int i = 0; i < actions.length; i++) {
				ComparisonCriteriaAction action = actions[i];
				menu.add(action);
			}
		}
	}
}
