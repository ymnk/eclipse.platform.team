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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.TeamSubscriber;
import org.eclipse.team.internal.ccvs.core.CVSMergeSubscriber;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.core.SaveContext;
import org.eclipse.team.internal.core.SaveContextXMLWriter;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.synchronize.sets.SubscriberInput;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.eclipse.team.ui.synchronize.TeamSubscriberParticipant;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.IPageBookViewPage;

public class MergeSynchronizeParticipant extends TeamSubscriberParticipant {
	
	public MergeSynchronizeParticipant() {
		super();
	}
	
	public MergeSynchronizeParticipant(CVSMergeSubscriber subscriber) {
		super();
		setSubscriber(subscriber);
	}
			
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.TeamSubscriberParticipant#setSubscriber(org.eclipse.team.core.subscribers.TeamSubscriber)
	 */
	protected void setSubscriber(TeamSubscriber subscriber) {
		super.setSubscriber(subscriber);
		String id = CVSMergeSubscriber.QUALIFIED_NAME;
		try {
			setInitializationData(TeamUI.getSynchronizeManager().getParticipantDescriptor(id).getConfigurationElement(), id, null);
		} catch (CoreException e) {
			CVSUIPlugin.log(e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.ISynchronizeParticipant#init(org.eclipse.team.ui.sync.ISynchronizeView, org.eclipse.team.core.ISaveContext)
	 */
	public void init(String instanceid) throws PartInitException {
		SaveContext ctx; //$NON-NLS-1$
		try {
			ctx = SaveContextXMLWriter.readXMLPluginMetaFile(CVSUIPlugin.getPlugin(), getMetaFileName(id.getLocalName()));
			setSubscriber(CVSMergeSubscriber.restore(id, ctx));
		} catch (TeamException e) {
			TeamUIPlugin.log(e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.ISynchronizeParticipant#saveState(org.eclipse.team.core.ISaveContext)
	 */
	public void saveState(int instanceid) {
		SubscriberInput input = getInput();
		CVSMergeSubscriber s =(CVSMergeSubscriber)input.getSubscriber(); 
		SaveContext ctx = s.saveState();
		try {
			SaveContextXMLWriter.writeXMLPluginMetaFile(CVSUIPlugin.getPlugin(), getMetaFileName(getId().getLocalName()), ctx); //$NON-NLS-1$		
		} catch (TeamException e) {
			TeamUIPlugin.log(e);
		}
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.AbstractSynchronizeParticipant#dispose()
	 */
	protected void dispose() {
		super.dispose();
		((CVSMergeSubscriber)getInput().getSubscriber()).cancel();
		SaveContextXMLWriter.deleteXMLPluginMetaFile(CVSUIPlugin.getPlugin(), getMetaFileName(getId().getLocalName())); //$NON-NLS-1$
	}
	
	private String getMetaFileName(String id) {
		return "mergeSyncPartners" + id + ".xml"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#createPage(org.eclipse.team.ui.synchronize.ISynchronizeView)
	 */
	public IPageBookViewPage createPage(ISynchronizeView view) {		
		return new MergeSynchronizePage(this, view, getInput());
	}
}