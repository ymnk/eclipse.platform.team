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
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.ConfigureRefreshScheduleDialog;
import org.eclipse.team.internal.ui.synchronize.actions.*;
import org.eclipse.team.ui.synchronize.ISynchronizePageSite;
import org.eclipse.team.ui.synchronize.StructuredViewerAdvisor;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkingSet;

/**
 * Provides the actions to be associated with a synchronize page
 */
public class SubscriberActionProvider {
	
	// the changes viewer are contributed via the viewer and not the page.
	private NavigateAction gotoNext;
	private NavigateAction gotoPrevious;
	private Action configureSchedule;
	private SyncViewerShowPreferencesAction showPreferences;
	private Action refreshAllAction;
	private Action collapseAll;
	private WorkingSetFilterActionGroup workingSetGroup;
	private StatusLineContributionGroup statusLine;
	private StructuredViewerAdvisor viewerAdvisor;
	private SubscriberConfiguration configuration;
	
	public void initialize() {
		final SubscriberParticipant participant = (SubscriberParticipant)configuration.getParticipant();
		// toolbar
		INavigatable nav = new INavigatable() {
			public boolean gotoDifference(boolean next) {
				return viewerAdvisor.navigate(next);
			}
		};
		gotoNext = new NavigateAction(getSite(), participant.getName(), nav, true /*next*/);		
		gotoPrevious = new NavigateAction(getSite(), participant.getName(), nav, false /*previous*/);
		
		if(participant.doesSupportSynchronize()) {
			refreshAllAction = new Action() {
				public void run() {
					// Prime the refresh wizard with an appropriate initial selection
					final SubscriberRefreshWizard wizard = new SubscriberRefreshWizard(participant);
					IWorkingSet set = configuration.getWorkingSet();
					if(set != null) {
						int scopeHint = SubscriberRefreshWizard.SCOPE_WORKING_SET;
						wizard.setScopeHint(scopeHint);
					}					
					WizardDialog dialog = new WizardDialog(site.getShell(), wizard);
					dialog.open();
				}
			};
			Utils.initAction(refreshAllAction, "action.refreshWithRemote."); //$NON-NLS-1$
		}
		
		collapseAll = new Action() {
			public void run() {
				if (changesViewer == null || !(changesViewer instanceof AbstractTreeViewer)) return;
				changesViewer.getControl().setRedraw(false);		
				((AbstractTreeViewer)changesViewer).collapseToLevel(changesViewer.getInput(), TreeViewer.ALL_LEVELS);
				changesViewer.getControl().setRedraw(true);
			}
		};
		Utils.initAction(collapseAll, "action.collapseAll."); //$NON-NLS-1$
		
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
		workingSetGroup = new WorkingSetFilterActionGroup(site.getShell(), participant.toString(), this, configuration.getWorkingSet());		
		showPreferences = new SyncViewerShowPreferencesAction(site.getShell());		
		statusLine = new StatusLineContributionGroup(site.getShell(), this, configuration, workingSetGroup);
		
		participant.addPropertyChangeListener(this);
		TeamUIPlugin.getPlugin().getPreferenceStore().addPropertyChangeListener(this);
	}
	
	/**
	 * @return
	 */
	public ISynchronizePageSite getSite() {
		return site;
	}

	public void setActionBars(IActionBars actionBars) {
		if(actionBars != null) {
			IToolBarManager manager = actionBars.getToolBarManager();			
			
			// toolbar
			if(refreshAllAction != null) {
				manager.add(refreshAllAction);
			}
			manager.add(new Separator());	
			if(gotoNext != null) {
				manager.add(gotoNext);
				manager.add(gotoPrevious);
			}
			manager.add(collapseAll);
			manager.add(new Separator());

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
}
