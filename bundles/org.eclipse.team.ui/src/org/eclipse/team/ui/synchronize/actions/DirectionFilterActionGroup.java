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
package org.eclipse.team.ui.synchronize.actions;

import java.util.*;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.actions.TextToolbarManager;
import org.eclipse.team.internal.ui.synchronize.sets.*;
import org.eclipse.team.ui.synchronize.TeamSubscriberParticipant;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionGroup;

/**
 * This ActionGroup provides filtering of a sync set by change direction.
 * The actions are presented to the user as toolbar buttons where only one
 * button is active at a time
 */
public class DirectionFilterActionGroup extends ActionGroup implements IPropertyChangeListener, ISyncSetChangedListener {
	
	// An array of the selection actions for the modes (indexed by mode constant)	
	private List actions = new ArrayList(3);
	
	private DirectionFilterAction incomingMode;					
	private DirectionFilterAction outgoingMode;
	private DirectionFilterAction bothMode;
	private DirectionFilterAction conflictsMode;
	private TeamSubscriberParticipant page;
	
	private int supportedModes;
	
	class DirectionFilterAction extends Action {
		private int modeId;
		
		public DirectionFilterAction(String prefix,String commandId, int modeId) {
			super("", AS_RADIO_BUTTON); //$NON-NLS-1$
			this.modeId = modeId;
			Utils.initAction(this, prefix);
			Action a = new Action() {
				public void run() {
					DirectionFilterAction.this.run();
				}
			};
			//IKeyBindingService kbs = site.getKeyBindingService();
			//Utils.registerAction(kbs, a, commandId);	//$NON-NLS-1$
		}
		public void run() {
			// checkMode() is called because programatic checking of radio buttons doesn't 
			// consider radio buttons, hence breaks the radio-button behavior. As a workaround
			// we have to manually check/uncheck the set instead.
			checkMode(modeId);
			page.setMode(modeId);
		}
		public int getModeId() {
			return modeId;
		}
	}
	
	public DirectionFilterActionGroup(TeamSubscriberParticipant page, int supportedModes) {		
		this.supportedModes = supportedModes;
		this.page = page;
		createActions();
		page.addPropertyChangeListener(this);
		page.getInput().registerListeners(this);
		checkMode(page.getMode());
		//updateStats();
	}
	
	/**
	 * Sets up the sync modes and the actions for switching between them.
	 */
	private void createActions() {
		// Create the actions
		if((supportedModes & TeamSubscriberParticipant.INCOMING_MODE) != 0) {
			incomingMode = new DirectionFilterAction("action.directionFilterIncoming.", "org.eclipse.team.ui.syncview.incomingFilter",  TeamSubscriberParticipant.INCOMING_MODE); //$NON-NLS-1$ //$NON-NLS-2$
			actions.add(incomingMode);
		}
		
		if((supportedModes & TeamSubscriberParticipant.OUTGOING_MODE) != 0) {
			outgoingMode = new DirectionFilterAction("action.directionFilterOutgoing.", "org.eclipse.team.ui.syncview.outgoingFilter",  TeamSubscriberParticipant.OUTGOING_MODE); //$NON-NLS-1$ //$NON-NLS-2$
			actions.add(outgoingMode);
		}
		
		if((supportedModes & TeamSubscriberParticipant.BOTH_MODE) != 0) {
			bothMode = new DirectionFilterAction("action.directionFilterBoth.", "org.eclipse.team.ui.syncview.bothFilter", TeamSubscriberParticipant.BOTH_MODE); //$NON-NLS-1$ //$NON-NLS-2$
			actions.add(bothMode);
		}
		
		if((supportedModes & TeamSubscriberParticipant.CONFLICTING_MODE) != 0) {
			conflictsMode = new DirectionFilterAction("action.directionFilterConflicts.", "org.eclipse.team.ui.syncview.conflictsFilter", TeamSubscriberParticipant.CONFLICTING_MODE); //$NON-NLS-1$ //$NON-NLS-2$
			actions.add(conflictsMode);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.ActionGroup#fillActionBars(org.eclipse.ui.IActionBars)
	 */
	public void fillActionBars(IActionBars actionBars, String group) {
		super.fillActionBars(actionBars);
		IToolBarManager toolBar = actionBars.getToolBarManager();
		for (Iterator it = actions.iterator(); it.hasNext();) {
			DirectionFilterAction action = (DirectionFilterAction) it.next();
			if(group != null) {
				toolBar.appendToGroup(group, action);
			} else {
				toolBar.add(action);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.ActionGroup#fillActionBars(org.eclipse.ui.IActionBars)
	 */
	public void fillToolBar(IToolBarManager toolBar) {
		boolean custom = false;
		if(toolBar instanceof TextToolbarManager) {
			custom = true;
		}
		for (Iterator it = actions.iterator(); it.hasNext();) {
			DirectionFilterAction action = (DirectionFilterAction) it.next();
			if(custom) {
				((TextToolbarManager)toolBar).add(action, 150);
			} else {
				toolBar.add(action);
			}
		}
	}
	
	private void checkMode(int mode) {
		for (Iterator it = actions.iterator(); it.hasNext();) {
			DirectionFilterAction action = (DirectionFilterAction)it.next();
			if(action.getModeId() == mode) {
				action.setChecked(true);
			} else {
				action.setChecked(false);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if(event.getProperty().equals(TeamSubscriberParticipant.P_SYNCVIEWPAGE_MODE)) {
			Integer mode = (Integer)event.getNewValue();
			checkMode(mode.intValue());
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.ActionGroup#dispose()
	 */
	public void dispose() {
		// TODO Auto-generated method stub
		super.dispose();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.sync.sets.ISyncSetChangedListener#syncSetChanged(org.eclipse.team.internal.ui.sync.sets.SyncSetChangedEvent)
	 */
	public void syncSetChanged(SyncSetChangedEvent event) {
		//updateStats();		
	}
	
	private void updateStats() {
		SubscriberInput input = page.getInput();
		SyncInfoStatistics workspaceSetStats = input.getSubscriberSyncSet().getStatistics();
		SyncInfoStatistics workingSetSetStats = input.getWorkingSetSyncSet().getStatistics();
		
		int workspaceConflicting = (int)workspaceSetStats.countFor(SyncInfo.CONFLICTING, SyncInfo.DIRECTION_MASK);
		int workspaceOutgoing = (int)workspaceSetStats.countFor(SyncInfo.OUTGOING, SyncInfo.DIRECTION_MASK);
		int workspaceIncoming = (int)workspaceSetStats.countFor(SyncInfo.INCOMING, SyncInfo.DIRECTION_MASK);
		int workingSetConflicting = (int)workingSetSetStats.countFor(SyncInfo.CONFLICTING, SyncInfo.DIRECTION_MASK);
		int workingSetOutgoing = (int)workingSetSetStats.countFor(SyncInfo.OUTGOING, SyncInfo.DIRECTION_MASK);
		int workingSetIncoming = (int)workingSetSetStats.countFor(SyncInfo.INCOMING, SyncInfo.DIRECTION_MASK);
		
		if(bothMode != null)
		bothMode.setText(new Integer(input.getWorkingSetSyncSet().size()).toString());
		if(input.getWorkingSet() != null) {
			if(conflictsMode != null)
				conflictsMode.setText(padString(Policy.bind("StatisticsPanel.changeNumbers", new Integer(workingSetConflicting).toString(), new Integer(workspaceConflicting).toString()))); //$NON-NLS-1$
			if(incomingMode != null)
				incomingMode.setText(padString(Policy.bind("StatisticsPanel.changeNumbers", new Integer(workingSetIncoming).toString(), new Integer(workspaceIncoming).toString()))); //$NON-NLS-1$
			if(outgoingMode != null)
				outgoingMode.setText(padString(Policy.bind("StatisticsPanel.changeNumbers", new Integer(workingSetOutgoing).toString(), new Integer(workspaceOutgoing).toString()))); //$NON-NLS-1$
		} else {
			if(conflictsMode != null)
				conflictsMode.setText(padString(new Integer(workspaceConflicting).toString())); //$NON-NLS-1$
			if(incomingMode != null)
				incomingMode.setText(padString(new Integer(workspaceIncoming).toString())); //$NON-NLS-1$
			if(outgoingMode != null)
				outgoingMode.setText(padString(new Integer(workspaceOutgoing).toString())); //$NON-NLS-1$
		}								
	}
	
	private String padString(String s) {		
		return s;		
	}
}