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
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.ISaveContext;
import org.eclipse.team.core.subscribers.TeamSubscriber;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSMergeSubscriber;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ui.synchronize.sets.SubscriberInput;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.eclipse.team.ui.synchronize.TeamSubscriberParticipant;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.IPageBookViewPage;

public class MergeSynchronizeParticipant extends TeamSubscriberParticipant {
	
	private final static String CTX_QUALIFIER = "qualifier";
	private final static String CTX_LOCALNAME = "localname";
	
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
	public void restoreState(ISaveContext context) throws PartInitException {
		String qualifier = context.getAttribute(CTX_QUALIFIER);
		String localname = context.getAttribute(CTX_LOCALNAME);
		if(qualifier == null || localname == null) {
			throw new PartInitException("Missing id initializing cvs merge participant");
		}
		try {
			setSubscriber(CVSMergeSubscriber.restore(new QualifiedName(qualifier, localname), context));
		} catch (CVSException e) {
			throw new PartInitException("Unable to initialize cvs merge subscriber", e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.ISynchronizeParticipant#saveState(org.eclipse.team.core.ISaveContext)
	 */
	public void saveState(ISaveContext context) {
		SubscriberInput input = getInput();
		CVSMergeSubscriber s = (CVSMergeSubscriber)input.getSubscriber();
		QualifiedName sId = s.getId();
		context.addAttribute(CTX_QUALIFIER, sId.getQualifier());
		context.addAttribute(CTX_LOCALNAME, sId.getLocalName());
		s.saveState(context);
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.AbstractSynchronizeParticipant#dispose()
	 */
	protected void dispose() {
		super.dispose();
		((CVSMergeSubscriber)getInput().getSubscriber()).cancel();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#createPage(org.eclipse.team.ui.synchronize.ISynchronizeView)
	 */
	public IPageBookViewPage createPage(ISynchronizeView view) {		
		return new MergeSynchronizePage(this, view, getInput());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#getName()
	 */
	public String getName() {		
		return ((CVSMergeSubscriber)getInput().getSubscriber()).getName();
	}
}