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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.TeamSubscriber;
import org.eclipse.team.internal.ccvs.core.CVSMergeSubscriber;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.core.SaveContext;
import org.eclipse.team.internal.core.SaveContextXMLWriter;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.sync.sets.SubscriberInput;
import org.eclipse.team.ui.Utilities;
import org.eclipse.team.ui.sync.actions.DirectionFilterActionGroup;
import org.eclipse.team.ui.sync.actions.RemoveSynchronizeViewPageAction;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PartInitException;

public class CVSMergeSynchronizeParticipant extends CVSSynchronizeParticipant {
	
	private RemoveSynchronizeViewPageAction removeAction;
	private DirectionFilterActionGroup modes;
	private Action updateAdapter;
	private final static String PARTICIPANT_ID = "org.eclipse.team.cvs.ui.cvsmerge_syncparticipant";
	
	public CVSMergeSynchronizeParticipant(CVSMergeSubscriber subscriber) {
		super();
		setSubscriber(subscriber);
		setName(subscriber.getName());
		setId(PARTICIPANT_ID);
		setImageDescriptor(CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_PROJECT_VERSION));
	}
			
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.TeamSubscriberParticipant#setSubscriber(org.eclipse.team.core.subscribers.TeamSubscriber)
	 */
	protected void setSubscriber(TeamSubscriber subscriber) {
		super.setSubscriber(subscriber);
		setInstanceId(subscriber.getId().toString());
		makeActions();
	}
	
	private void makeActions() {
		removeAction = new RemoveSynchronizeViewPageAction(this);
		modes = new DirectionFilterActionGroup(this, INCOMING_MODE | CONFLICTING_MODE | BOTH_MODE);
		updateAdapter = new CVSActionDelegate(new WorkspaceUpdateAction(), this);
		Utilities.initAction(updateAdapter, "action.SynchronizeViewUpdate.", Policy.getBundle());
		setMode(INCOMING_MODE);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.SubscriberPage#setActionsBars(org.eclipse.ui.IActionBars)
	 */
	public void setActionsBars(IActionBars actionBars, IToolBarManager detailsToolbar) {		
		if(actionBars != null) {
			IToolBarManager toolbar = actionBars.getToolBarManager();
			toolbar.add(new Separator());
			toolbar.add(new Separator());
			actionBars.getToolBarManager().add(updateAdapter);
			actionBars.getToolBarManager().add(removeAction);
		}
		if(detailsToolbar != null) {
			modes.fillToolBar(detailsToolbar);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.ISynchronizeParticipant#init(org.eclipse.team.ui.sync.ISynchronizeView, org.eclipse.team.core.ISaveContext)
	 */
	public void init(String instance_id) throws PartInitException {
		SaveContext ctx; //$NON-NLS-1$
		try {
			ctx = SaveContextXMLWriter.readXMLPluginMetaFile(CVSUIPlugin.getPlugin(), getMetaFileName(instance_id));
			setSubscriber(CVSMergeSubscriber.restore(instance_id, ctx));
		} catch (TeamException e) {
			TeamUIPlugin.log(e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.ISynchronizeParticipant#saveState(org.eclipse.team.core.ISaveContext)
	 */
	public void saveState() {
		SubscriberInput input = getInput();
		CVSMergeSubscriber s =(CVSMergeSubscriber)input.getSubscriber(); 
		SaveContext ctx = s.saveState();
		try {
			SaveContextXMLWriter.writeXMLPluginMetaFile(CVSUIPlugin.getPlugin(), getMetaFileName(getId()), ctx); //$NON-NLS-1$		
		} catch (TeamException e) {
			TeamUIPlugin.log(e);
		}
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.AbstractSynchronizeParticipant#dispose()
	 */
	protected void dispose() {
		super.dispose();
		SaveContextXMLWriter.deleteXMLPluginMetaFile(CVSUIPlugin.getPlugin(), getMetaFileName(getId())); //$NON-NLS-1$
	}
	
	private String getMetaFileName(String id) {
		return "mergeSyncPartners" + id + ".xml";
	}
}