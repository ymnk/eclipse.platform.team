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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.team.internal.ui.sync.sets.ISyncSetChangedListener;
import org.eclipse.team.internal.ui.sync.sets.SyncSetChangedEvent;
import org.eclipse.team.ui.sync.TeamSubscriberParticipant;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.part.IPageBookViewPage;

public abstract class CVSSynchronizeParticipant extends TeamSubscriberParticipant implements ISyncSetChangedListener {
	
	private List delegates = new ArrayList(2); 
	
	protected class CVSActionDelegate extends Action {
		private TeamSubscriberParticipant participant;
		private IActionDelegate delegate;
		
		public CVSActionDelegate(IActionDelegate delegate, TeamSubscriberParticipant participant) {
			this.delegate = delegate;
			this.participant = participant;
			addDelegate(this);			
		}
		
		public void run() {
			IStructuredContentProvider cp = (IStructuredContentProvider)participant.getPage().getViewer().getContentProvider(); 
			StructuredSelection selection = new StructuredSelection(cp.getElements(participant.getInput()));
			if(! selection.isEmpty()) {
				delegate.selectionChanged(this, selection);
				delegate.run(this);
			}
		}

		public IActionDelegate getDelegate() {
			return delegate;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.AbstractSynchronizeParticipant#dispose()
	 */
	protected void dispose() {
		super.dispose();
		getInput().getFilteredSyncSet().removeSyncSetChangedListener(this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.sync.sets.ISyncSetChangedListener#syncSetChanged(org.eclipse.team.internal.ui.sync.sets.SyncSetChangedEvent)
	 */
	public void syncSetChanged(SyncSetChangedEvent event) {
		IPageBookViewPage page = getPage();
		if(page != null) {
			StructuredViewer viewer = getPage().getViewer();
			if(viewer != null) {
				IStructuredContentProvider cp = (IStructuredContentProvider)viewer.getContentProvider(); 
				StructuredSelection selection = new StructuredSelection(cp.getElements(getInput()));
				for (Iterator it = delegates.iterator(); it.hasNext(); ) {
					CVSActionDelegate delegate = (CVSActionDelegate) it.next();
					delegate.getDelegate().selectionChanged(delegate, selection);
				}
			}
		}
	}
	
	private void addDelegate(CVSActionDelegate delagate) {
		delegates.add(delagate);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.AbstractSynchronizeParticipant#init()
	 */
	protected void init() {
		super.init();
		getInput().getFilteredSyncSet().addSyncSetChangedListener(this);
	}
}