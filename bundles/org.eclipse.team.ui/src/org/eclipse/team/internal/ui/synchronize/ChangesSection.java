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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.ui.ISharedImages;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.forms.parts.*;
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
		
		participant.getSyncInfoSetCollector().getSyncInfoSet().addSyncSetChangedListener(changedListener);
		participant.getSyncInfoCollector().getSyncInfoSet().addSyncSetChangedListener(changedListener);
	}
	
	private void calculateDescription() {
		if(participant.getSyncInfoSetCollector().getSyncInfoSet().size() == 0) {
			TeamUIPlugin.getStandardDisplay().asyncExec(new Runnable() {
				public void run() {
						filteredContainer = getEmptyChangesComposite(changesSectionContainer);
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
	
	private Composite getEmptyChangesComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessVerticalSpace = true;
		composite.setLayoutData(data);
		
		SyncInfoSet workspace = participant.getSyncInfoCollector().getSyncInfoSet();
		SyncInfoSet workingSet = participant.getSyncInfoSetCollector().getWorkingSetSyncInfoSet();
		SyncInfoSet filteredSet = participant.getSyncInfoSetCollector().getSyncInfoSet();
		
		int changesInWorkspace = workspace.size();
		int changesInWorkingSet =workingSet.size();
		int changesInFilter = filteredSet.size();
		
		long outgoingChanges = workingSet.countFor(SyncInfo.OUTGOING, SyncInfo.DIRECTION_MASK);
		long incomingChanges = workingSet.countFor(SyncInfo.INCOMING, SyncInfo.DIRECTION_MASK);		
		
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
			
			Hyperlink link = new Hyperlink(composite, SWT.WRAP);
			link.setText(Policy.bind("ChangesSection.filterChange", Utils.modeToString(newMode)));
			link.addHyperlinkListener(new HyperlinkAdapter() {
				public void linkActivated(HyperlinkEvent e) {
					participant.setMode(newMode);
				}
			});
			link.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_WHITE));
			link.setUnderlined(true);
			createDescriptionLabel(composite, text.toString());
		} else if(changesInFilter == 0 && changesInWorkingSet == 0 && changesInWorkspace != 0) {
			Label warning = new Label(composite, SWT.NONE);
			warning.setImage(TeamUIPlugin.getPlugin().getImage(ISharedImages.IMG_WARNING));
			
			Hyperlink link = new Hyperlink(composite, SWT.WRAP);
			link.setText(Policy.bind("ChangesSection.workingSetRemove"));
			link.addHyperlinkListener(new HyperlinkAdapter() {
				public void linkActivated(HyperlinkEvent e) {
					participant.setWorkingSet(null);
				}
			});
			link.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_WHITE));
			link.setUnderlined(true);
			createDescriptionLabel(composite,Policy.bind("ChangesSection.workingSetHiding", Utils.workingSetToString(participant.getWorkingSet(), 50)));	
		} else {
			createDescriptionLabel(composite,Policy.bind("ChangesSection.noChanges", participant.getName()));	
		}		
		return composite;
	}
	
	public Viewer getChangesViewer() {
		return changesViewer;
	}
	
	private Label createDescriptionLabel(Composite parent, String text) {
		Label description = new Label(parent, SWT.WRAP);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		data.widthHint = 100;
		description.setLayoutData(data);
		description.setText(text);
		description.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		return description;
	}
	
	public void dispose() {
		super.dispose();
		participant.getSyncInfoSetCollector().getSyncInfoSet().removeSyncSetChangedListener(changedListener);
		participant.getSyncInfoCollector().getSyncInfoSet().removeSyncSetChangedListener(changedListener);
	}
}