/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.synchronize.subscribers;

import org.eclipse.compare.internal.INavigatable;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.synchronize.ConfigureRefreshScheduleDialog;
import org.eclipse.team.internal.ui.synchronize.actions.*;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkingSet;

/**
 * Provides the actions to be associated with a synchronize page
 */
public class SubscriberActionContribution implements IActionContribution, IPropertyChangeListener {
	
	// the changes viewer are contributed via the viewer and not the page.
	private NavigateAction gotoNext;
	private NavigateAction gotoPrevious;
	private Action configureSchedule;
	private SyncViewerShowPreferencesAction showPreferences;
	private Action refreshAllAction;
	private Action refreshSelectionAction;
	private Action collapseAll;
	private WorkingSetFilterActionGroup workingSetGroup;
	private StatusLineContributionGroup statusLine;

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IActionContribution#initialize(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration)
	 */
	public void initialize(final ISynchronizePageConfiguration configuration) {
		final SubscriberParticipant participant = (SubscriberParticipant)configuration.getParticipant();
		final StructuredViewerAdvisor viewerAdvisor = configuration.getAdvisor();
		final ISynchronizePageSite site = configuration.getSite();
		final StructuredViewer viewer = configuration.getAdvisor().getViewer();
		// toolbar
		INavigatable nav = new INavigatable() {
			public boolean gotoDifference(boolean next) {
				return viewerAdvisor.navigate(next);
			}
		};
		gotoNext = new NavigateAction(site, participant.getName(), nav, true /*next*/);		
		gotoPrevious = new NavigateAction(site, participant.getName(), nav, false /*previous*/);
		
		if(participant.doesSupportSynchronize()) {
			refreshAllAction = new Action() {
				public void run() {
					// Prime the refresh wizard with an appropriate initial selection
					final SubscriberRefreshWizard wizard = new SubscriberRefreshWizard(participant);
					IWorkingSet set = (IWorkingSet)configuration.getProperty(ISynchronizePageConfiguration.P_WORKING_SET);
					if(set != null) {
						int scopeHint = SubscriberRefreshWizard.SCOPE_WORKING_SET;
						wizard.setScopeHint(scopeHint);
					}					
					WizardDialog dialog = new WizardDialog(site.getShell(), wizard);
					dialog.open();
				}
			};
			Utils.initAction(refreshAllAction, "action.refreshWithRemote."); //$NON-NLS-1$
			refreshSelectionAction = new Action() {
				public void run() {
					if(viewer != null && ! viewer.getControl().isDisposed()) {
						IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
						IResource[] resources = Utils.getResources(selection.toArray());
						participant.refresh(resources, participant.getRefreshListeners().createSynchronizeViewListener(participant), Policy.bind("Participant.synchronizing"), site.getWorkbenchSite()); //$NON-NLS-1$
					}
				}
			};
			Utils.initAction(refreshSelectionAction, "action.refreshWithRemote."); //$NON-NLS-1$

		}
		
		if (viewer instanceof AbstractTreeViewer) {
			collapseAll = new Action() {
				public void run() {
					if (viewer == null || viewer.getControl().isDisposed() || !(viewer instanceof AbstractTreeViewer)) return;
					viewer.getControl().setRedraw(false);		
					((AbstractTreeViewer)viewer).collapseToLevel(viewer.getInput(), TreeViewer.ALL_LEVELS);
					viewer.getControl().setRedraw(true);
				}
			};
			Utils.initAction(collapseAll, "action.collapseAll."); //$NON-NLS-1$
		}
		
		configureSchedule = new Action() {
			public void run() {
				ConfigureRefreshScheduleDialog d = new ConfigureRefreshScheduleDialog(
						site.getShell(), participant.getRefreshSchedule());
				d.setBlockOnOpen(false);
				d.open();
			}
		};
		Utils.initAction(configureSchedule, "action.configureSchedulel."); //$NON-NLS-1$
		
		// view menu
		workingSetGroup = new WorkingSetFilterActionGroup(site.getShell(), participant.toString(), this, (IWorkingSet)configuration.getProperty(ISynchronizePageConfiguration.P_WORKING_SET));		
		showPreferences = new SyncViewerShowPreferencesAction(site.getShell());		
		statusLine = new StatusLineContributionGroup(site.getShell(), this, configuration, workingSetGroup);
		
		participant.addPropertyChangeListener(this);
		TeamUIPlugin.getPlugin().getPreferenceStore().addPropertyChangeListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IActionContribution#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	public void fillContextMenu(IMenuManager manager) {
		if (refreshSelectionAction != null && manager.find(ISynchronizePageConfiguration.SYNCHRONIZE_GROUP) != null) {
			manager.insertAfter(ISynchronizePageConfiguration.SYNCHRONIZE_GROUP, refreshSelectionAction);
		}	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IActionContribution#setActionBars(org.eclipse.ui.IActionBars)
	 */
	public void setActionBars(IActionBars actionBars) {
		if(actionBars != null) {
			IToolBarManager manager = actionBars.getToolBarManager();			
			
			// toolbar
			if(refreshAllAction != null 
					&& manager.find(ISynchronizePageConfiguration.SYNCHRONIZE_GROUP) != null) {
				manager.insertAfter(ISynchronizePageConfiguration.SYNCHRONIZE_GROUP, refreshAllAction);
			}
			if (manager.find(ISynchronizePageConfiguration.NAVIGATE_GROUP) != null) {
				if(gotoNext != null) {
					manager.insertAfter(ISynchronizePageConfiguration.NAVIGATE_GROUP, gotoNext);
					manager.insertAfter(ISynchronizePageConfiguration.NAVIGATE_GROUP, gotoPrevious);
				}
				manager.insertAfter(ISynchronizePageConfiguration.NAVIGATE_GROUP, collapseAll);
			}

			// view menu
			IMenuManager menu = actionBars.getMenuManager();
			workingSetGroup.fillActionBars(actionBars);
			menu.add(new Separator());
			menu.add(new Separator());
			menu.add(new Separator("others")); //$NON-NLS-1$
			menu.add(new Separator());
			menu.add(configureSchedule);
			menu.add(new Separator());
			menu.add(showPreferences);
			
			// status line
			statusLine.fillActionBars(actionBars);
		}		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IActionContribution#dispose()
	 */
	public void dispose() {
		// Nothing to dispose
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		// Working set changed by user
		if(event.getProperty().equals(WorkingSetFilterActionGroup.CHANGE_WORKING_SET)) {
			settingWorkingSet = true;
			configuration.setWorkingSet((IWorkingSet)event.getNewValue());
		// Change to showing of sync state in text labels preference
		} else if(event.getProperty().equals(IPreferenceIds.SYNCVIEW_VIEW_SYNCINFO_IN_LABEL)) {
			if(changesViewer instanceof StructuredViewer) {
				((StructuredViewer)changesViewer).refresh(true /* update labels */);
			}
		}
	}
}
