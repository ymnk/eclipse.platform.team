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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ui.widgets.ControlFactory;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.controls.IControlFactory;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipantListener;
import org.eclipse.ui.part.Page;

/**
 * Page that displays the overview information for Synchronize participants.
 * 
 * @since 3.0
 */
public class SynchronizeOverviewPage extends Page implements ISynchronizeParticipantListener {

	private Composite pageComposite;
	private Map participantsToComposites = new HashMap();
	private SynchronizeView view;
	private IControlFactory factory;
	private static final String COLOR_WHITE = "__colorwhite__";
	
	public SynchronizeOverviewPage(SynchronizeView view) {
		this.view = view;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.Page#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {		
		factory = new ControlFactory(parent.getDisplay());
		factory.setBackgroundColor(factory.registerColor(COLOR_WHITE, 255, 255, 255));		
		//ScrolledComposite scrolledPageComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		//scrolledPageComposite.setBackground(factory.getBackgroundColor());		
		pageComposite = factory.createComposite(parent, SWT.NONE);
		//scrolledPageComposite.setContent(pageComposite);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		layout.horizontalSpacing = 0;
		pageComposite.setLayout(layout);
		pageComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		//pageComposite.setSize(pageComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		Composite introComposite = factory.createComposite(pageComposite);
		layout = new GridLayout();
		layout.marginHeight = 5;
		layout.marginWidth = 5;
		introComposite.setLayout(layout);
		introComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Label message = factory.createLabel(introComposite, "Manage the synchronization of files in your workspace with their remote locations.", SWT.WRAP);
		GridData data =
			new GridData(
					GridData.GRAB_HORIZONTAL
					| GridData.HORIZONTAL_ALIGN_FILL
					| GridData.VERTICAL_ALIGN_BEGINNING);
		data.widthHint = 200;
		message.setLayoutData(data);
				
		Label seperator = factory.createSeparator(introComposite, SWT.HORIZONTAL);
		data =
			new GridData(
					GridData.GRAB_HORIZONTAL
					| GridData.HORIZONTAL_ALIGN_FILL
					| GridData.VERTICAL_ALIGN_BEGINNING);
		seperator.setLayoutData(data);
		
		createParticipants(pageComposite);
		TeamUI.getSynchronizeManager().addSynchronizeParticipantListener(this);
	}

	/**
	 * @param pageComposite2
	 */
	private void createParticipants(Composite parent) {
		ISynchronizeParticipant[] participants = TeamUI.getSynchronizeManager().getSynchronizeParticipants();
		for (int i = 0; i < participants.length; i++) {
			ISynchronizeParticipant participant = participants[i];
			participantsToComposites.put(participant, new ParticipantOverviewComposite(parent, factory, participant, view, SWT.NONE));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.Page#getControl()
	 */
	public Control getControl() {
		return pageComposite;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.Page#setFocus()
	 */
	public void setFocus() {
		pageComposite.setFocus();
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.ISynchronizeParticipantListener#participantsAdded(org.eclipse.team.ui.sync.ISynchronizeParticipant[])
	 */
	public void participantsAdded(final ISynchronizeParticipant[] participants) {
		if (isAvailable()) {
			Runnable r = new Runnable() {
				public void run() {
					for (int i = 0; i < participants.length; i++) {
						if (isAvailable()) {
							ISynchronizeParticipant participant = participants[i];
							participantsToComposites.put(participant, new ParticipantOverviewComposite(pageComposite, factory, participant, view, SWT.NONE));
							
							// re-layout and redraw with new participant added
							pageComposite.setRedraw(false);
							pageComposite.getParent().setRedraw(false);
							pageComposite.layout(true);
							pageComposite.getParent().layout(true);
							pageComposite.setRedraw(true);
							pageComposite.getParent().setRedraw(true);
						}
					}
				}
			};
			asyncExec(r);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.ISynchronizeParticipantListener#participantsRemoved(org.eclipse.team.ui.sync.ISynchronizeParticipant[])
	 */
	public void participantsRemoved(final ISynchronizeParticipant[] consoles) {
		if (isAvailable()) {
			Runnable r = new Runnable() {
				public void run() {
					for (int i = 0; i < consoles.length; i++) {
						if (isAvailable()) {
							ISynchronizeParticipant console = consoles[i];
							Composite composite = (Composite)participantsToComposites.get(console);
							composite.dispose();
							pageComposite.layout(true);
							pageComposite.redraw();
						}
					}
				}
			};
			asyncExec(r);
		}
	}
	
	/**
	 * @return
	 */
	private boolean isAvailable() {
		return ! pageComposite.isDisposed() && ! pageComposite.getDisplay().isDisposed();
	}

	/**
	 * Registers the given runnable with the display
	 * associated with this view's control, if any.
	 * 
	 * @see org.eclipse.swt.widgets.Display#asyncExec(java.lang.Runnable)
	 */
	public void asyncExec(Runnable r) {
		if (isAvailable()) {
			pageComposite.getDisplay().asyncExec(r);
		}
	}
}