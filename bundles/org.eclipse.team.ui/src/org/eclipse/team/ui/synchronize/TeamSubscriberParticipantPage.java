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
package org.eclipse.team.ui.synchronize;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ui.IPreferenceIds;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.jobs.JobBusyCursor;
import org.eclipse.team.internal.ui.synchronize.ChangesSection;
import org.eclipse.team.internal.ui.synchronize.ConfigureRefreshScheduleDialog;
import org.eclipse.team.internal.ui.synchronize.SyncInfoDiffViewerForSynchronizeView;
import org.eclipse.team.internal.ui.synchronize.actions.ComparisonCriteriaActionGroup;
import org.eclipse.team.internal.ui.synchronize.actions.NavigateAction;
import org.eclipse.team.internal.ui.synchronize.actions.RefreshAction;
import org.eclipse.team.internal.ui.synchronize.actions.StatusLineContributionGroup;
import org.eclipse.team.internal.ui.synchronize.actions.SyncViewerShowPreferencesAction;
import org.eclipse.team.internal.ui.synchronize.actions.WorkingSetFilterActionGroup;
import org.eclipse.team.ui.synchronize.actions.INavigableControl;
import org.eclipse.team.ui.synchronize.actions.SubscriberAction;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.ShowInContext;

/**
 * A synchronize view page that works with participants that are subclasses of 
 * {@link TeamSubscriberParticipant}. It shows changes in the tree or table view
 * and supports navigation, opening, and filtering changes.
 * <p>
 * Clients can subclass to extend the label decoration or add action bar 
 * contributions. For more extensive modifications, clients should create
 * their own custom control.
 * </p> 
 * @since 3.0
 */
public class TeamSubscriberParticipantPage implements IPageBookViewPage, IPropertyChangeListener {
	// Parent composite of this view. It is remembered so that we can dispose of its children when 
	// the viewer type is switched.
	private Composite composite = null;
	private ChangesSection changesSection;
	private boolean settingWorkingSet = false;
	
	// Remembering the current input and the previous.
	private ITeamSubscriberSyncInfoSets input = null;
	
	private JobBusyCursor busyCursor;
	private ISynchronizeView view;
	private TeamSubscriberParticipant participant;
	private IPageSite site;
	
	// Toolbar and status line actions for this page, note that context menu actions shown in 
	// the changes viewer are contributed via the viewer and not the page.
	private NavigateAction gotoNext;
	private NavigateAction gotoPrevious;
	private Action configureSchedule;
	private SyncViewerShowPreferencesAction showPreferences;
	private RefreshAction refreshAllAction;
	private ComparisonCriteriaActionGroup comparisonCriteriaGroup;
	private Action collapseAll;
	private WorkingSetFilterActionGroup workingSetGroup;
	private StatusLineContributionGroup statusLine;
		
	/**
	 * Constructs a new SynchronizeView.
	 */
	public TeamSubscriberParticipantPage(TeamSubscriberParticipant page, ISynchronizeView view, ITeamSubscriberSyncInfoSets input) {
		this.participant = page;
		this.view = view;
		this.input = input;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		composite = new Composite(parent, SWT.NONE); 
		//sc.setContent(composite);
		GridLayout gridLayout= new GridLayout();
		gridLayout.makeColumnsEqualWidth= false;
		gridLayout.marginWidth= 0;
		gridLayout.marginHeight = 0;
		gridLayout.verticalSpacing = 0;
		composite.setLayout(gridLayout);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessVerticalSpace = true;
		composite.setLayoutData(data);
		
		// Create the busy cursor with no control to start with (createViewer will set it)
		busyCursor = new JobBusyCursor(parent.getParent().getParent(), SubscriberAction.SUBSCRIBER_JOB_TYPE);
		
		changesSection = new ChangesSection(composite, this);		
		
		// toolbar
		gotoNext = new NavigateAction(view, changesSection.getChangesViewer(), INavigableControl.NEXT);		
		gotoPrevious = new NavigateAction(view, changesSection.getChangesViewer(), INavigableControl.PREVIOUS);
		refreshAllAction = new RefreshAction(getSite().getPage(), getParticipant(), true /* refresh all */);
		collapseAll = new Action() {
			public void run() {
				Viewer viewer = getChangesViewer();
				if (viewer == null || !(viewer instanceof AbstractTreeViewer)) return;
				viewer.getControl().setRedraw(false);		
				((AbstractTreeViewer)viewer).collapseToLevel(viewer.getInput(), TreeViewer.ALL_LEVELS);
				viewer.getControl().setRedraw(true);
			}
		};
		Utils.initAction(collapseAll, "action.collapseAll."); //$NON-NLS-1$
		
		configureSchedule = new Action() {
			public void run() {
				ConfigureRefreshScheduleDialog d = new ConfigureRefreshScheduleDialog(
						getShell(), participant.getRefreshSchedule());
				d.setBlockOnOpen(false);
				d.open();
			}
		};
		Utils.initAction(configureSchedule, "action.configureSchedulel."); //$NON-NLS-1$
		
		// view menu
		comparisonCriteriaGroup = new ComparisonCriteriaActionGroup(input);		
		workingSetGroup = new WorkingSetFilterActionGroup(getShell(), this, view, participant);		
		showPreferences = new SyncViewerShowPreferencesAction(getShell());		
		statusLine = new StatusLineContributionGroup(getShell(), getParticipant());
		
		participant.addPropertyChangeListener(this);
		TeamUIPlugin.getPlugin().getPreferenceStore().addPropertyChangeListener(this);
		participant.setMode(participant.getMode());
	}
	
	private Shell getShell() {
		return view.getSite().getShell();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IViewPart#init(org.eclipse.ui.IViewSite, org.eclipse.ui.IMemento)
	 */
	public void init(IPageSite site) throws PartInitException {
		this.site = site;		
	}
	
	protected Viewer getChangesViewer() {
		return changesSection.getChangesViewer();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IPage#setFocus()
	 */
	public void setFocus() {
		changesSection.setFocus();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#dispose()
	 */
	public void dispose() {
		busyCursor.dispose();
		changesSection.dispose();
	}
	
	/*
	 * Return the current input for the view.
	 */
	public ITeamSubscriberSyncInfoSets getInput() {
		return input;
	}

	/*
	 * This method enables "Show In" support for this view
	 * 
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class key) {
		if (key == IShowInSource.class) {
			return new IShowInSource() {
				public ShowInContext getShowInContext() {					
					StructuredViewer v = (StructuredViewer)getChangesViewer();
					if (v == null) return null;
					return new ShowInContext(null, v.getSelection());
				}
			};
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IPage#getControl()
	 */
	public Control getControl() {
		return composite;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IPage#setActionBars(org.eclipse.ui.IActionBars)
	 */
	public void setActionBars(IActionBars actionBars) {
		if(actionBars != null) {
			IToolBarManager manager = actionBars.getToolBarManager();			
			
			// toolbar
			manager.add(refreshAllAction);
			manager.add(new Separator());		
			manager.add(gotoNext);
			manager.add(gotoPrevious);
			manager.add(collapseAll);
			manager.add(new Separator());

			// view menu
			IMenuManager menu = actionBars.getMenuManager();
			MenuManager layoutMenu = new MenuManager(Policy.bind("action.layout.label")); //$NON-NLS-1$		
			MenuManager comparisonCriteria = new MenuManager(Policy.bind("action.comparisonCriteria.label")); //$NON-NLS-1$
			comparisonCriteriaGroup.addActionsToMenuMgr(comparisonCriteria);
			workingSetGroup.fillActionBars(actionBars);
			menu.add(new Separator());
			menu.add(comparisonCriteria);
			menu.add(new Separator());
			menu.add(configureSchedule);
			menu.add(new Separator());
			menu.add(showPreferences);
			
			// status line
			statusLine.fillActionBars(actionBars);
		}		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IPageBookViewPage#getSite()
	 */
	public IPageSite getSite() {
		return this.site;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		// Working set changed by user
		if(event.getProperty().equals(WorkingSetFilterActionGroup.CHANGE_WORKING_SET)) {
			if(settingWorkingSet) return;
			settingWorkingSet = true;
			participant.setWorkingSet((IWorkingSet)event.getNewValue());
			settingWorkingSet = false;
		// Working set changed programatically
		} else if(event.getProperty().equals(TeamSubscriberParticipant.P_SYNCVIEWPAGE_WORKINGSET)) {
			if(settingWorkingSet) return;
			settingWorkingSet = true;
			Object newValue = event.getNewValue();
			if (newValue instanceof IWorkingSet) {	
				workingSetGroup.setWorkingSet((IWorkingSet)newValue);
			} else if (newValue == null) {
				workingSetGroup.setWorkingSet(null);
			}
			settingWorkingSet = false;
		// Change to showing of sync state in text labels preference
		} else if(event.getProperty().equals(IPreferenceIds.SYNCVIEW_VIEW_SYNCINFO_IN_LABEL)) {
			Viewer viewer = getChangesViewer();
			if(viewer instanceof StructuredViewer) {
				((StructuredViewer)viewer).refresh(true /* update labels */);
			}
		}
	}
	
	/**
	 * @return Returns the participant.
	 */
	public TeamSubscriberParticipant getParticipant() {
		return participant;
	}
	
	/**
	 * @return Returns the view.
	 */
	public ISynchronizeView getSynchronizeView() {
		return view;
	}
	
	public Viewer createChangesViewer(Composite parent) {
		Viewer viewer =  new SyncInfoDiffViewerForSynchronizeView(parent, getSynchronizeView(), getParticipant(), getInput().getFilteredSyncSet());
		getSite().setSelectionProvider(viewer);		
		return viewer;
	}
}