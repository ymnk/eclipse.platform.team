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
import org.eclipse.jface.action.*;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.team.ui.synchronize.subscriber.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IActionDelegate;

public class CVSSynchronizeViewPage extends TeamSubscriberParticipantPage implements ISyncInfoSetChangeListener {
	
	private List delegates = new ArrayList(2);
	private CVSSynchronizeViewCompareConfiguration config;
	private Action groupByComment;

	protected class CVSActionDelegate extends Action {
		private IActionDelegate delegate;

		public CVSActionDelegate(IActionDelegate delegate) {
			this.delegate = delegate;
			addDelegate(this);
		}

		public void run() {
			ISelection selection = new StructuredSelection(getSyncInfoSet().getSyncInfos());
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
		groupByComment = new Action("Show incoming grouped by comment", Action.AS_CHECK_BOX) {
			public void run() {
				config.setGroupIncomingByComment(!config.isGroupIncomingByComment());
				setChecked(config.isGroupIncomingByComment());
			}
		};
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.TeamSubscriberParticipantPage#setActionBars(org.eclipse.ui.IActionBars)
	 */
	public void setActionBars(IActionBars actionBars) {
		super.setActionBars(actionBars);
		IMenuManager mgr = actionBars.getMenuManager();
		mgr.add(new Separator());
		mgr.add(groupByComment);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.internal.ui.sync.sets.ISyncSetChangedListener#syncSetChanged(org.eclipse.team.internal.ui.sync.sets.SyncSetChangedEvent)
	 */
	public void syncSetChanged(ISyncInfoSetChangeEvent event, IProgressMonitor monitor) {
		updateActionEnablement();
	}

	/*
	 * Update the enablement of any action delegates 
	 */
	private void updateActionEnablement() {
		StructuredViewer viewer = (StructuredViewer)getChangesViewer();
		if (viewer != null && getSyncInfoSet() != null) {
			ISelection selection = new StructuredSelection(getSyncInfoSet().getSyncInfos());
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
		
		// Update the enablement of any action delegates.
		// This is done after sync set registry to avoid the possibility of losing changes
		updateActionEnablement();
		
		// Listen for decorator changed to refresh the viewer's labels.
		CVSUIPlugin.addPropertyChangeListener(this);
	}
	
	private SyncInfoTree getSyncInfoSet() {
		return getParticipant().getSubscriberSyncInfoCollector().getSyncInfoTree();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.TeamSubscriberParticipantPage#createSyncInfoSetCompareConfiguration()
	 */
	protected TeamSubscriberPageDiffTreeViewerConfiguration createSyncInfoSetCompareConfiguration() {
		if(config == null) {
			config = new CVSSynchronizeViewCompareConfiguration(getSynchronizeView(), getParticipant());
		}
		return config;
	}
}