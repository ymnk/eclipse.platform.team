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

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.synchronize.sets.SyncInfoStatistics;
import org.eclipse.team.internal.ui.widgets.*;
import org.eclipse.team.ui.controls.IControlFactory;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.part.PageBook;

/**
 * Section shown in a participant page to show the changes for this participant. This
 * includes a diff viewer for browsing the changes.
 * 
 * @since 3.0
 */
public class ChangesSection extends FormSection {
	
	private TeamSubscriberParticipant participant;
	private Composite parent;
	private final ISynchronizeView view;
	private IControlFactory factory;
	private TeamSubscriberParticipantPage page;
	private Button smartModeSwitchButton;
			
	/**
	 * Page book either shows the diff tree viewer if there are changes or
	 * shows a message to the user if there are no changes that would be
	 * shown in the tree.
	 */
	private PageBook changesSectionContainer;
	
	/**
	 * Shows message to user is no changes are to be shown in the diff
	 * tree viewer.
	 */
	private Composite filteredContainer;
	
	/**
	 * Diff tree viewer that shows synchronization changes. This is created
	 * by the participant.
	 */
	private Viewer changesViewer;
	
	/**
	 * Label to the right of the section header showing the total number of
	 * changes in the workspace.
	 */
	private Label changeTotalsLabel;
	private int totalChanges = -1;

	/**
	 * Listen to sync set changes so that we can update message to user and totals.
	 */
	private ISyncSetChangedListener changedListener = new ISyncSetChangedListener() {
		public void syncSetChanged(ISyncInfoSetChangeEvent event) {
			calculateDescription();
			updateChangeTotals(true);
		}
	};
	
	
	/**
	 * Create a changes section on the following page.
	 * 
	 * @param parent the parent control 
	 * @param page the page showing this section
	 */
	public ChangesSection(Composite parent, TeamSubscriberParticipantPage page) {
		this.page = page;
		this.participant = page.getParticipant();
		this.parent = parent;
		this.view = page.getSynchronizeView();
		setCollapsable(true);
		setCollapsed(false);
		setDescription("");
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.widgets.FormSection#getHeaderText()
	 */
	public String getHeaderText() {
		return "Changes";
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.widgets.FormSection#createClient(org.eclipse.swt.widgets.Composite,
	 *      org.eclipse.team.internal.ui.widgets.FormWidgetFactory)
	 */
	public Composite createClient(Composite parent, IControlFactory factory) {
		this.factory = factory;
		Composite clientComposite = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		clientComposite.setLayout(layout);
		changesSectionContainer = new PageBook(clientComposite, SWT.NONE);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		changesSectionContainer.setLayoutData(data);
		
		changesViewer = page.createChangesViewer(changesSectionContainer);

		calculateDescription();		
		participant.getInput().registerListeners(changedListener);
		return clientComposite;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.widgets.FormSection#createHeaderRight(org.eclipse.swt.widgets.Composite, org.eclipse.team.internal.ui.widgets.ControlFactory)
	 */
	protected Composite createHeaderRight(Composite parent, ControlFactory factory) {
		Composite top = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		top.setLayout(layout);
		top.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		changeTotalsLabel = factory.createLabel(top, "", 20);
		int newChanges = participant.getInput().getSubscriberSyncSet().size();
		changeTotalsLabel.setText(Integer.toString(newChanges) + " Total");
		return top;
	}
	
	protected void reflow() {
		super.reflow();
		if(parent != null && !parent.isDisposed()) {
			parent.setRedraw(false);
			parent.getParent().setRedraw(false);
			parent.layout(true);
			parent.getParent().layout(true);
			parent.setRedraw(true);
			parent.getParent().setRedraw(true);
		}
	}
	
	private void calculateDescription() {
		if(participant.getInput().getFilteredSyncSet().size() == 0) {
			TeamUIPlugin.getStandardDisplay().asyncExec(new Runnable() {
				public void run() {
						filteredContainer = getEmptyChangesComposite(changesSectionContainer, factory);
						changesSectionContainer.showPage(filteredContainer);
				}
			});
		} else {
			TeamUIPlugin.getStandardDisplay().asyncExec(new Runnable() {
				public void run() {
					if(filteredContainer != null) {
						filteredContainer.dispose();
						filteredContainer = null;
					}
					changesSectionContainer.showPage(changesViewer.getControl());
				}
			});
		}
	}
	
	private Composite getEmptyChangesComposite(Composite parent, IControlFactory factory) {
		Composite composite = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		
		ITeamSubscriberSyncInfoSets input = participant.getInput();
		int changesInWorkspace = input.getSubscriberSyncSet().size();
		int changesInWorkingSet = input.getWorkingSetSyncSet().size();
		int changesInFilter = input.getFilteredSyncSet().size();
		
		SyncInfoStatistics stats = input.getWorkingSetSyncSet().getStatistics();
		long outgoingChanges = stats.countFor(SyncInfo.OUTGOING, SyncInfo.DIRECTION_MASK);
		long incomingChanges = stats.countFor(SyncInfo.INCOMING, SyncInfo.DIRECTION_MASK);		
		
		Label l;
		
		if(changesInFilter == 0 && changesInWorkingSet != 0) {
			int mode = participant.getMode();
			final int newMode = outgoingChanges != 0 ? TeamSubscriberParticipant.OUTGOING_MODE : TeamSubscriberParticipant.INCOMING_MODE;
			long numChanges = outgoingChanges != 0 ? outgoingChanges : incomingChanges;
			String text;
			if(numChanges > 1) {
				text = Policy.bind("ChangesSection.filterHides", Utils.modeToString(participant.getMode())) +
				Policy.bind("ChangesSection.filterHidesPlural", Long.toString(numChanges), Utils.modeToString(newMode));
			} else {
				text = Policy.bind("ChangesSection.filterHidesSingular", Utils.modeToString(participant.getMode()), Long.toString(numChanges), Utils.modeToString(newMode));
			}													
			createHyperlink(factory, composite, Policy.bind("ChangesSection.filterChange", Utils.modeToString(newMode)), new HyperlinkAdapter() {
				public void linkActivated(Control linkLabel) {
					participant.setMode(newMode);
				}
			});
			l = factory.createLabel(composite, text , SWT.WRAP);
			
//			smartModeSwitchButton = new Button(composite, SWT.CHECK | SWT.FLAT | SWT.WRAP);
//			smartModeSwitchButton.setText("Enable smart mode switching.");
//			smartModeSwitchButton.setBackground(factory.getBackgroundColor());
//			Label description = factory.createLabel(composite, "Smart mode switching will detect when there are changes that aren't displayed with the given mode selected and automatically change the mode that would show the changes.", SWT.WRAP);
//			GridData data = new GridData(GridData.FILL_HORIZONTAL);
//			data.widthHint = 100;
//			description.setLayoutData(data);
//			smartModeSwitchButton.addSelectionListener(new SelectionListener() {
//				public void widgetSelected(SelectionEvent e) {
//					TeamUIPlugin.getPlugin().getPreferenceStore().setValue(IPreferenceIds.SYNCVIEW_VIEW_SMART_MODE_SWITCH, smartModeSwitchButton.getSelection());
//				}
//				public void widgetDefaultSelected(SelectionEvent e) {
//				}
//			});
		} else if(changesInFilter == 0 && changesInWorkingSet == 0 && changesInWorkspace != 0) {			
			createHyperlink(factory, composite, Policy.bind("ChangesSection.workingSetRemove"), new HyperlinkAdapter() {
				public void linkActivated(Control linkLabel) {
					participant.setWorkingSet(null);
				}
			});
			l = factory.createLabel(composite, Policy.bind("ChangesSection.workingSetHiding", Utils.workingSetToString(participant.getWorkingSet(), 50)), SWT.WRAP);
		} else {
			l= factory.createLabel(composite, Policy.bind("ChangesSection.noChanges"), SWT.WRAP);
		}
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 100;
		l.setLayoutData(data);	
		return composite;
	}
	
	/**
	 * @param factory
	 * @param composite
	 */
	private void createHyperlink(IControlFactory factory, Composite composite, String text, HyperlinkAdapter adapter) {
		final Label label = factory.createLabel(composite, text, SWT.WRAP);
		GridData data = new GridData();
		label.setLayoutData(data);
		factory.turnIntoHyperlink(label, adapter);
	}

	public Viewer getChangesViewer() {
		return changesViewer;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.widgets.FormSection#dispose()
	 */
	public void dispose() {
		super.dispose();
		participant.getInput().deregisterListeners(changedListener);
	}
	
	private int getSmartMode() {
		ISyncInfoSet set = participant.getInput().getWorkingSetSyncSet();
		if(set.size() == 0) {
			return -1;
		}		
		SyncInfoStatistics stats = set.getStatistics();
		long outgoingChanges = stats.countFor(SyncInfo.OUTGOING, SyncInfo.DIRECTION_MASK);
		long incomingChanges = stats.countFor(SyncInfo.INCOMING, SyncInfo.DIRECTION_MASK);
		return incomingChanges != 0 ?  TeamSubscriberParticipant.INCOMING_MODE : TeamSubscriberParticipant.OUTGOING_MODE;
	}
	
	public void updateChangeTotals(final boolean reflow) {
		final int newChanges = participant.getInput().getSubscriberSyncSet().size();
		if (totalChanges != newChanges) {
			TeamUIPlugin.getStandardDisplay().asyncExec(new Runnable() {
				public void run() {
					changeTotalsLabel.setText(Integer.toString(newChanges) + " Total");
					totalChanges = newChanges;
				}
			});
		}
	}
}