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
package org.eclipse.team.internal.ui.synchronize.actions;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.team.internal.ui.jobs.RefreshUserNotificationPolicy;
import org.eclipse.team.ui.synchronize.TeamSubscriberParticipant;
import org.eclipse.team.ui.synchronize.actions.RefreshAction;

/**
 * A specialized RefreshAction that extracts the required components from a 
 * particpant.
 */
public class TeamParticipantRefreshAction extends RefreshAction {
	
	public TeamParticipantRefreshAction(ISelectionProvider provider, TeamSubscriberParticipant participant, boolean refreshAll) {
		super(provider, participant.getName(), participant.getSyncInfoCollector(), new RefreshUserNotificationPolicy(participant), refreshAll);
	}
	
	public static void run(IResource[] resources, TeamSubscriberParticipant participant) {
		run(participant.getName(), resources, participant.getSyncInfoCollector(), new RefreshUserNotificationPolicy(participant));
	}
}