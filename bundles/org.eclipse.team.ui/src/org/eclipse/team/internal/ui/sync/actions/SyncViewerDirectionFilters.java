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
package org.eclipse.team.internal.ui.sync.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.core.sync.SyncInfo;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.UIConstants;
import org.eclipse.team.internal.ui.sync.views.SyncViewer;
import org.eclipse.team.ui.TeamImages;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;

/**
 * This ActionGroup provides filtering of a sync set by change direction.
 * The actions are presented to the user as toolbar buttons where only one
 * button is active at a time
 */
public class SyncViewerDirectionFilters extends SyncViewerActionGroup {

	// constants or the possible sync modes
	public static final int SYNC_INCOMING = 0;
	public static final int SYNC_OUTGOING = 1;
	public static final int SYNC_BOTH = 2;
	public static final int SYNC_CONFLICTS = 3;
	
	private static final String MEMENTO_KEY = "SyncViewerDirectionFilters";
	
	// An array of the selection actions for the modes (indexed by mode constant)
	private DirectionFilterAction[] actions;
	private SyncViewerActions refreshGroup;
	
	// the currently active mode
	private int currentSyncMode = SYNC_BOTH;
	
	/**
	 * Action for toggling the sync mode.
	 */
	class DirectionFilterAction extends Action {
		// The sync mode that this action enables
		private int syncMode;
		// the direction filters for this mode
		private int[] directionFilters;
		// the title to be used for the view when this mode is active
		private String viewTitle;
		public DirectionFilterAction(String title, ImageDescriptor image, int mode, int[] directionFilters, String viewTitle) {
			super(title, image);
			this.syncMode = mode;
			this.directionFilters = directionFilters;
			this.viewTitle = viewTitle;
		}
		public void run() {
			setSyncMode(getSyncMode(), getViewTitle());
		}
		public int[] getDirectionFilters() {
			return directionFilters;
		}
		public int getSyncMode() {
			return syncMode;
		}
		public String getViewTitle() {
			return viewTitle;
		}

	}
	
	protected SyncViewerDirectionFilters(SyncViewer viewer, SyncViewerActions refreshGroup) {
		super(viewer);
		this.refreshGroup = refreshGroup;
		createActions();
	}
	
	/**
	 * Sets up the sync modes and the actions for switching between them.
	 */
	private void createActions() {
		// Create the actions
		DirectionFilterAction incomingMode = new DirectionFilterAction(
			Policy.bind("SyncView.incomingModeAction"), //$NON-NLS-1$
			TeamImages.getImageDescriptor(UIConstants.IMG_SYNC_MODE_CATCHUP_ENABLED),
			SYNC_INCOMING,
			new int[] { SyncInfo.INCOMING, SyncInfo.CONFLICTING },
			Policy.bind("SyncView.incomingModeTitle"));
		incomingMode.setToolTipText(Policy.bind("SyncView.incomingModeToolTip")); //$NON-NLS-1$
		incomingMode.setChecked(currentSyncMode == SYNC_INCOMING);
		incomingMode.setDisabledImageDescriptor(TeamImages.getImageDescriptor(UIConstants.IMG_SYNC_MODE_CATCHUP_DISABLED));
		incomingMode.setHoverImageDescriptor(TeamImages.getImageDescriptor(UIConstants.IMG_SYNC_MODE_CATCHUP));
			
		DirectionFilterAction outgoingMode = new DirectionFilterAction(
			Policy.bind("SyncView.outgoingModeAction"), //$NON-NLS-1$
			TeamImages.getImageDescriptor(UIConstants.IMG_SYNC_MODE_RELEASE_ENABLED),
			SYNC_OUTGOING,
			new int[] { SyncInfo.OUTGOING, SyncInfo.CONFLICTING },
			Policy.bind("SyncView.outgoingModeTitle"));
		outgoingMode.setToolTipText(Policy.bind("SyncView.outgoingModeToolTip")); //$NON-NLS-1$
		outgoingMode.setChecked(currentSyncMode == SYNC_OUTGOING);
		outgoingMode.setDisabledImageDescriptor(TeamImages.getImageDescriptor(UIConstants.IMG_SYNC_MODE_RELEASE_DISABLED));
		outgoingMode.setHoverImageDescriptor(TeamImages.getImageDescriptor(UIConstants.IMG_SYNC_MODE_RELEASE));
		
		DirectionFilterAction bothMode = new DirectionFilterAction(
			Policy.bind("SyncView.freeModeAction"), //$NON-NLS-1$
			TeamImages.getImageDescriptor(UIConstants.IMG_SYNC_MODE_FREE_ENABLED),
			SYNC_BOTH,
			new int[] { SyncInfo.INCOMING, SyncInfo.OUTGOING, SyncInfo.CONFLICTING },
			Policy.bind("SyncView.freeModeTitle"));
		bothMode.setToolTipText(Policy.bind("SyncView.freeModeToolTip")); //$NON-NLS-1$
		bothMode.setChecked(currentSyncMode == SYNC_BOTH);
		bothMode.setDisabledImageDescriptor(TeamImages.getImageDescriptor(UIConstants.IMG_SYNC_MODE_FREE_DISABLED));
		bothMode.setHoverImageDescriptor(TeamImages.getImageDescriptor(UIConstants.IMG_SYNC_MODE_FREE));
		
		DirectionFilterAction conflictsMode = new DirectionFilterAction(
			Policy.bind("CatchupReleaseViewer.showOnlyConflictsAction"), //$NON-NLS-1$
			TeamImages.getImageDescriptor(UIConstants.IMG_DLG_SYNC_CONFLICTING_ENABLED),
			SYNC_CONFLICTS,
			new int[] { SyncInfo.CONFLICTING },
			"Synchronize - Conflict Mode");
		conflictsMode.setToolTipText(Policy.bind("CatchupReleaseViewer.showOnlyConflictsAction")); //$NON-NLS-1$
		conflictsMode.setChecked(currentSyncMode == SYNC_CONFLICTS);
		conflictsMode.setDisabledImageDescriptor(TeamImages.getImageDescriptor(UIConstants.IMG_DLG_SYNC_CONFLICTING_DISABLED));
		conflictsMode.setHoverImageDescriptor(TeamImages.getImageDescriptor(UIConstants.IMG_DLG_SYNC_CONFLICTING));
		
		// place the actions in the instance varibale array
		actions = new DirectionFilterAction[4];
		actions[SYNC_INCOMING] = incomingMode;
		actions[SYNC_OUTGOING] = outgoingMode;
		actions[SYNC_BOTH] = bothMode;
		actions[SYNC_CONFLICTS] = conflictsMode;
	}
	
	/**
	 * Activates the given sync mode.
	 */
	void setSyncMode(int mode, String title) {
		boolean changed = currentSyncMode != mode;
		currentSyncMode = mode;
		activateCurrentMode();
		if(changed) {
			getSyncView().setTitle(title);  
			getRefreshGroup().refreshFilters();
		}
	}

	/*
	 * Select the current mode  and deselect all others.
	 */
	private void activateCurrentMode() {
		for (int i = 0; i < actions.length; i++) {
			DirectionFilterAction action = actions[i];
			action.setChecked(i == currentSyncMode);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.ActionGroup#fillActionBars(org.eclipse.ui.IActionBars)
	 */
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		IToolBarManager toolBar = actionBars.getToolBarManager();
		for (int i = 0; i < actions.length; i++) {
			toolBar.add(actions[i]);
		}
	}

	public int[] getDirectionFilters() {
		return actions[currentSyncMode].getDirectionFilters();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ccvs.syncviews.actions.SyncViewerActionGroup#restore(org.eclipse.ui.IMemento)
	 */
	public void restore(IMemento memento) {
		super.restore(memento);
		Integer i = memento.getInteger(MEMENTO_KEY);
		if (i != null) {
			currentSyncMode = i.intValue();
		}
		activateCurrentMode();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ccvs.syncviews.actions.SyncViewerActionGroup#save(org.eclipse.ui.IMemento)
	 */
	public void save(IMemento memento) {
		super.save(memento);
		memento.putInteger(MEMENTO_KEY, currentSyncMode);
	}

	public SyncViewerActions getRefreshGroup() {
		return refreshGroup;
	}
}
