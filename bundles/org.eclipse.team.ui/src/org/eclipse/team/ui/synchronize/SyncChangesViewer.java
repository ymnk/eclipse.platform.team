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
package org.eclipse.team.ui.synchronize;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.team.ui.synchronize.actions.INavigableControl;

public abstract class SyncChangesViewer implements INavigableControl {

	private TeamSubscriberParticipant participant;
	private ISynchronizeView view;
	
	public SyncChangesViewer(TeamSubscriberParticipant participant, ISynchronizeView view) {
		this.view = view;
		this.participant = participant;		
	}
	
	public abstract StructuredViewer getViewer();
	public abstract void dispose();
}