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

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.subscribers.TeamSubscriber;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.eclipse.team.ui.synchronize.TeamSubscriberParticipant;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.IPageBookViewPage;

public class WorkspaceSynchronizeParticipant extends TeamSubscriberParticipant {

	public final static QualifiedName ID = new QualifiedName("org.eclipse.team.cvs.ui.cvsworkspace-participant", "syncparticipant"); //$NON-NLS-1$ //$NON-NLS-2$
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.ISynchronizeParticipant#init(org.eclipse.team.ui.sync.ISynchronizeView, org.eclipse.team.core.ISaveContext)
	 */
	public void init(QualifiedName id) throws PartInitException {
		TeamSubscriber subscriber = CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber(); 
		setSubscriber(subscriber);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.ISynchronizeParticipant#saveState(org.eclipse.team.core.ISaveContext)
	 */
	public void saveState() {
		// no state to save
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#createPage(org.eclipse.team.ui.synchronize.ISynchronizeView)
	 */
	public IPageBookViewPage createPage(ISynchronizeView view) {
		return new WorkspaceSynchronizePage(this, view, getInput());
	}
}