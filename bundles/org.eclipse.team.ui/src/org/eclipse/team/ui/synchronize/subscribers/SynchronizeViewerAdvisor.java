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
package org.eclipse.team.ui.synchronize.subscribers;

import org.eclipse.team.core.synchronize.SyncInfoTree;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.eclipse.team.ui.synchronize.TreeViewerAdvisor;

/**
 * Overrides the SyncInfoDiffViewerConfiguration to configure the diff viewer
 * for the synchroniza view
 */
public class SynchronizeViewerAdvisor extends TreeViewerAdvisor {

	private ISynchronizeView view;
	private SubscriberParticipant participant;
	private SubscriberPageConfiguration configuration;

	public SynchronizeViewerAdvisor(SubscriberPageConfiguration configuration, SyncInfoTree syncInfoTree) {
		super(configuration.getParticipant().getId(), configuration.getPart().getSite(), syncInfoTree);
		this.configuration = configuration;
		this.view = view;
		this.participant = (SubscriberParticipant)configuration.getParticipant();
	}

	protected SubscriberParticipant getParticipant() {
		return participant;
	}
	
	protected SubscriberPageConfiguration getConfiguration() {
		return configuration;
	}

	protected ISynchronizeView getSynchronizeView() {
		return view;
	}

}
