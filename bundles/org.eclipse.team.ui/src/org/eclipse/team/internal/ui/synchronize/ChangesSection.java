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

import javax.naming.ldap.ControlFactory;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.synchronize.sets.SyncInfoStatistics;
import org.eclipse.team.ui.ISharedImages;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.part.PageBook;

/**
 * Section shown in a participant page to show the changes for this participant. This
 * includes a diff viewer for browsing the changes.
 * 
 * @since 3.0
 */
public class ChangesSection extends Composite {
	
	private TeamSubscriberParticipant participant;
	private Composite parent;
	private TeamSubscriberParticipantPage page;
	
	private static final String COLOR_WHITE = "changes_section.white";
			
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
	 * Listen to sync set changes so that we can update message to user and totals.
	 */
	private ISyncSetChangedListener changedListener = new ISyncSetChangedListener() {
		public void syncSetChanged(ISyncInfoSetChangeEvent event) {
			calculateDescription();
		}
	};
		
	/**
	 * Create a changes section on the following page.
	 * 
	 * @param parent the parent control 
	 * @param page the page showing this section
	 */
	public ChangesSection(Composite parent, TeamSubscriberParticipantPage page) {
		super(parent, SWT.NONE);
		this.page = page;
		this.participant = page.getParticipant();
		this.parent = parent;
		
		factory = new ControlFactory(parent.getDisplay());
		factory.registerColor(COLOR_WHITE, 255,255,255);
		factory.setBackgroundColor(factory.getColor(COLOR_WHITE));
		setBackground(factory.getBackgroundColor());
		
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessVerticalSpace = true;
		setLayoutData(data);
		
		changesSectionContainer = new PageBook(this, SWT.NONE);
		data = new GridData(GridData.FILL_BOTH);
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		changesSectionContainer.setLayoutData(data);
		
		changesViewer = page.createChangesViewer(changesSectionContainer);

		calculateDescription();
		
		participant.getInput().registerListeners(changedListener);
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
		layout.numColumns = 2;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessVerticalSpace = true;
		composite.setLayoutData(data);
		
		ITeamSubscriberSyncInfoSets input = participant.getInput();
		int changesInWorkspace = input.getSubscriberSyncSet().size();
		int changesInWorkingSet = input.getWorkingSetSyncSet().size();
		int changesInFilter = input.getFilteredSyncSet().size();
		
		SyncInfoStatistics stats = input.getWorkingSetSyncSet().getStatistics();
		long outgoingChanges = stats.countFor(SyncInfo.OUTGOING, SyncInfo.DIRECTION_MASK);
		long incomingChanges = stats.countFor(SyncInfo.INCOMING, SyncInfo.DIRECTION_MASK);		
		
		if(changesInFilter == 0 && changesInWorkingSet != 0) {
			int mode = participant.getMode();
			final int newMode = outgoingChanges != 0 ? TeamSubscriberParticipant.OUTGOING_MODE : TeamSubscriberParticipant.INCOMING_MODE;
			long numChanges = outgoingChanges != 0 ? outgoingChanges : incomingChanges;
			StringBuffer text = new StringBuffer();
			text.append(Policy.bind("ChangesSection.filterHides", Utils.modeToString(participant.getMode()))); //$NON-NLS-1$
			if(numChanges > 1) {
				text.append(Policy.bind("ChangesSection.filterHidesPlural", Long.toString(numChanges), Utils.modeToString(newMode))); //$NON-NLS-1$
			} else {
				text.append(Policy.bind("ChangesSection.filterHidesSingular", Long.toString(numChanges), Utils.modeToString(newMode))); //$NON-NLS-1$
			}
			Label warning = new Label(composite, SWT.NONE);
			warning.setImage(TeamUIPlugin.getPlugin().getImage(ISharedImages.IMG_WARNING));
			createHyperlink(factory, composite, Policy.bind("ChangesSection.filterChange", Utils.modeToString(newMode)), new HyperlinkAdapter() { //$NON-NLS-1$
				public void linkActivated(Control linkLabel) {
					participant.setMode(newMode);
				}
			});
			Label description = factory.createLabel(composite, text.toString(), SWT.WRAP);
			data = new GridData(GridData.FILL_HORIZONTAL);
			data.horizontalSpan = 2;
			data.widthHint = 100;
			description.setLayoutData(data);	
		} else if(changesInFilter == 0 && changesInWorkingSet == 0 && changesInWorkspace != 0) {
			Label warning = new Label(composite, SWT.NONE);
			warning.setImage(TeamUIPlugin.getPlugin().getImage(ISharedImages.IMG_WARNING));
			createHyperlink(factory, composite, Policy.bind("ChangesSection.workingSetRemove"), new HyperlinkAdapter() { //$NON-NLS-1$
				public void linkActivated(Control linkLabel) {
					participant.setWorkingSet(null);
				}
			});
			Label description = factory.createLabel(composite, Policy.bind("ChangesSection.workingSetHiding", Utils.workingSetToString(participant.getWorkingSet(), 50)), SWT.WRAP);
			data = new GridData(GridData.FILL_HORIZONTAL);
			data.horizontalSpan = 2;
			data.widthHint = 100;
			description.setLayoutData(data);	
		} else {
			factory.createLabel(composite, Policy.bind("ChangesSection.noChanges"), SWT.WRAP);
		}		
		return composite;
	}
	
	private void createHyperlink(IControlFactory factory, Composite composite, String text, HyperlinkAdapter adapter) {
		final Label label = factory.createLabel(composite, text, SWT.WRAP);
		GridData data = new GridData();
		label.setLayoutData(data);
		factory.turnIntoHyperlink(label, adapter);
	}

	public Viewer getChangesViewer() {
		return changesViewer;
	}
	
	public void dispose() {
		super.dispose();
		participant.getInput().deregisterListeners(changedListener);
	}
}