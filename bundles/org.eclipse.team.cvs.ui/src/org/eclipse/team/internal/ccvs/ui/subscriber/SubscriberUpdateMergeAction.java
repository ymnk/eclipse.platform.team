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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.core.subscribers.SyncTreeSubscriber;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSMergeSubscriber;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Update;
import org.eclipse.team.internal.ccvs.ui.repo.RepositoryManager;
import org.eclipse.team.internal.ui.sync.views.SyncResource;
import org.eclipse.team.ui.sync.SyncInfoDirectionFilter;
import org.eclipse.team.ui.sync.SyncInfoFilter;

/**
 * This action performs a "cvs update -j start -j end ..." to merge changes
 * into the local workspace.
 */
public class SubscriberUpdateMergeAction extends SubscriberUpdateAction {

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.SubscriberAction#getSyncInfoFilter()
	 */
	protected SyncInfoFilter getSyncInfoFilter() {
		return new SyncInfoDirectionFilter(new int[] {SyncInfo.CONFLICTING, SyncInfo.INCOMING});
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.subscriber.SubscriberUpdateAction#runUpdateShallow(org.eclipse.team.internal.ui.sync.views.SyncResource[], org.eclipse.team.internal.ccvs.ui.repo.RepositoryManager, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void runUpdateShallow(SyncResource[] nodes, RepositoryManager manager, IProgressMonitor monitor) throws TeamException {
		mergeWithLocal(nodes, manager, true /* backups */, monitor);
	}
	
	protected void mergeWithLocal(SyncResource[] nodes, RepositoryManager manager, boolean createBackup, IProgressMonitor monitor) throws TeamException {
		SyncTreeSubscriber subscriber = getSubscriber();
		if (!(subscriber instanceof CVSMergeSubscriber)) {
			throw new CVSException("Invalid subscriber: " + subscriber.getId());
		}
		CVSTag startTag = ((CVSMergeSubscriber)subscriber).getStartTag();
		CVSTag endTag = ((CVSMergeSubscriber)subscriber).getEndTag();
		
		Command.LocalOption[] options = new Command.LocalOption[] {
			Command.DO_NOT_RECURSE,
			Update.makeArgumentOption(Update.JOIN, startTag.getName()),
			Update.makeArgumentOption(Update.JOIN, endTag.getName()) };

		// run a join update using the start and end tags and the join points
		manager.update(getIResourcesFrom(nodes), options, createBackup, monitor);
	}

}
