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
package org.eclipse.team.internal.ccvs.ui.subscriber;

import org.eclipse.jface.action.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.team.core.subscribers.TeamSubscriber;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.ui.Utilities;
import org.eclipse.team.ui.sync.SubscriberPage;
import org.eclipse.team.ui.sync.actions.DirectionFilterActionGroup;
import org.eclipse.ui.IActionBars;

public class CVSSubscriberPage extends SubscriberPage {
	
	private DirectionFilterActionGroup modes;
	private Action commitAdapter;
	private Action updateAdapter;
	private int num;
	
	public CVSSubscriberPage(TeamSubscriber subscriber, String name, ImageDescriptor imageDescriptor, int num) {
		super(subscriber, name, imageDescriptor);
		this.num = num;
		modes = new DirectionFilterActionGroup(this, ALL_MODES);		
		commitAdapter = createCommitAdapter(new SubscriberCommitAction(), this);
		updateAdapter = createUpdateAdapter(new WorkspaceUpdateAction(), this);
	}
	
	public static Action createCommitAdapter(final SubscriberCommitAction commitAction, final SubscriberPage page) {
		Action adapter = new Action() {
			public void run() {
				IStructuredContentProvider cp = (IStructuredContentProvider)page.getPage().getViewer().getContentProvider(); 
				StructuredSelection selection = new StructuredSelection(cp.getElements(page.getInput()));
				if(! selection.isEmpty()) {
					commitAction.selectionChanged(this, selection);
					commitAction.run(this);
				}
			}
		};
		Utilities.initAction(adapter, "action.SynchronizeViewCommit.", Policy.getBundle());
		return adapter;
	}
	
	public static Action createUpdateAdapter(final WorkspaceUpdateAction updateAction, final SubscriberPage page) {
		Action adapter = new Action() {
			public void run() {
				IStructuredContentProvider cp = (IStructuredContentProvider)page.getPage().getViewer().getContentProvider(); 
				StructuredSelection selection = new StructuredSelection(cp.getElements(page.getInput()));
				if(! selection.isEmpty()) {
					updateAction.selectionChanged(this, selection);
					updateAction.run(this);
				}
			}
		};
		Utilities.initAction(adapter, "action.SynchronizeViewUpdate.", Policy.getBundle());
		return adapter;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.SubscriberPage#setActionsBars(org.eclipse.ui.IActionBars)
	 */
	public void setActionsBars(IActionBars actionBars) {
		super.setActionsBars(actionBars);
		IToolBarManager toolbar = actionBars.getToolBarManager();
		toolbar.add(new Separator());		
		modes.fillActionBars(actionBars, null);
		toolbar.add(new Separator());
		actionBars.getToolBarManager().add(updateAdapter);
		actionBars.getToolBarManager().add(commitAdapter);
	}
}
