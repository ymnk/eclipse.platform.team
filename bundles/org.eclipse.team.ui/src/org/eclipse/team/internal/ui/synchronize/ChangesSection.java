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

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.synchronize.sets.ISyncSetChangedListener;
import org.eclipse.team.internal.ui.synchronize.sets.SyncSetChangedEvent;
import org.eclipse.team.internal.ui.widgets.FormSection;
import org.eclipse.team.ui.controls.IControlFactory;
import org.eclipse.team.ui.synchronize.*;

public class ChangesSection extends FormSection {
	
	private TeamSubscriberParticipant participant;
	private Composite parent;
	private ParticipantComposite participantComposite;
	private ISynchronizeView view;
	private SyncChangesViewer changesViewer;

	private ISyncSetChangedListener changedListener = new ISyncSetChangedListener() {
		public void syncSetChanged(SyncSetChangedEvent event) {
			calculateDescription();
		}
	};
	private TeamSubscriberParticipantPage page;
		
	public ChangesSection(Composite parent, TeamSubscriberParticipantPage page) {
		this.page = page;
		this.participant = page.getParticipant();
		this.parent = parent;
		this.view = page.getSynchronizeView();
		setCollapsable(true);
		setCollapsed(false);
		setDescription("");
		calculateDescription();
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
		Composite clientComposite = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		clientComposite.setLayout(layout);
		changesViewer = page.createChangesViewer(clientComposite);
		participant.getInput().registerListeners(changedListener);
		return clientComposite;
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
					setDescription("There are no changes to see! Move on now...");
				}
			});
		} else {
			String description = getDescription();
			if(description != null) {
				TeamUIPlugin.getStandardDisplay().asyncExec(new Runnable() {
					public void run() {
						setDescription("");
					}
				});
			}
		}
	}
	
	public SyncChangesViewer getChangesViewer() {
		return changesViewer;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.widgets.FormSection#dispose()
	 */
	public void dispose() {
		super.dispose();
		changesViewer.dispose();
		participant.getInput().deregisterListeners(changedListener);
	}
}