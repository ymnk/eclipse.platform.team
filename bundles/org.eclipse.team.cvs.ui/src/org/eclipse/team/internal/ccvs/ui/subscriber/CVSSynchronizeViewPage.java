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

import java.util.*;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.IActionDelegate;

public class CVSSynchronizeViewPage extends TeamSubscriberParticipantPage implements ISyncSetChangedListener {
	
	private List delegates = new ArrayList(2);

	protected class CVSActionDelegate extends Action {
		private IActionDelegate delegate;

		public CVSActionDelegate(IActionDelegate delegate) {
			this.delegate = delegate;
			addDelegate(this);
		}

		public void run() {
			IStructuredContentProvider cp = (IStructuredContentProvider) ((StructuredViewer)getChangesViewer()).getContentProvider();
			StructuredSelection selection = new StructuredSelection(cp.getElements(getSyncInfoSet()));
			if (!selection.isEmpty()) {
				delegate.selectionChanged(this, selection);
				delegate.run(this);
			}
		}

		public IActionDelegate getDelegate() {
			return delegate;
		}
	}

	public CVSSynchronizeViewPage(TeamSubscriberParticipant participant, ISynchronizeView view) {
		super(participant, view);
	}	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.ui.sync.AbstractSynchronizeParticipant#dispose()
	 */
	public void dispose() {
		super.dispose();
		getSyncInfoSet().removeSyncSetChangedListener(this);
		CVSUIPlugin.removePropertyChangeListener(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.internal.ui.sync.sets.ISyncSetChangedListener#syncSetChanged(org.eclipse.team.internal.ui.sync.sets.SyncSetChangedEvent)
	 */
	public void syncSetChanged(ISyncInfoSetChangeEvent event, IProgressMonitor monitor) {
		StructuredViewer viewer = (StructuredViewer)getChangesViewer();
		if (viewer != null && getSyncInfoSet() != null) {
			IStructuredContentProvider cp = (IStructuredContentProvider) viewer.getContentProvider();
			StructuredSelection selection = new StructuredSelection(cp.getElements(getSyncInfoSet()));
			for (Iterator it = delegates.iterator(); it.hasNext(); ) {
				CVSActionDelegate delegate = (CVSActionDelegate) it.next();
				delegate.getDelegate().selectionChanged(delegate, selection);
			}
		}
	}

	private void addDelegate(CVSActionDelegate delagate) {
		delegates.add(delagate);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {		
		super.propertyChange(event);
		String prop = event.getProperty();
		if(prop.equals(CVSUIPlugin.P_DECORATORS_CHANGED) && getChangesViewer() != null && getSyncInfoSet() != null) {
			((StructuredViewer)getChangesViewer()).refresh(true /* update labels */);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		
		// Sync changes are used to update the action state for the update/commit buttons.
		getSyncInfoSet().addSyncSetChangedListener(this);
		
		// Listen for decorator changed to refresh the viewer's labels.
		CVSUIPlugin.addPropertyChangeListener(this);
	}
	
	private SyncInfoSet getSyncInfoSet() {
		return getParticipant().getFilteredSyncInfoCollector().getSyncInfoSet();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.TeamSubscriberParticipantPage#createSyncInfoSetCompareConfiguration()
	 */
	protected TeamSubscriberPageDiffTreeViewerConfiguration createSyncInfoSetCompareConfiguration() {
		return new CVSSynchronizeViewCompareConfiguration(getSynchronizeView(), getParticipant());
	}
}