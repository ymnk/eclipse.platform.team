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
package org.eclipse.team.internal.ui.sync.views;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ui.TeamUIPlugin;

public class SubscriberEventHandler {
	private SyncSetInputFromSubscriber set;
	
	public SubscriberEventHandler(SyncSetInputFromSubscriber set) {
		this.set = set;		
	}
	
	public void handleChange(IResource resource) {
		// recalculate sync state, we don't have progress though? Not ideal
		// since the calculation could be long running...
		try {
			set.collect(resource, (IProgressMonitor)null);
		} catch (TeamException e) {
			TeamUIPlugin.log(e);
		}
	}

	public void removeAllChildren(IResource resource) {
		set.getSyncSet().removeAllChildren(resource);
	}

	public void collectDeeply(IResource[] resources) {
		for (int i = 0; i < resources.length; i++) {
			collectDeeply(resources[i]);
		}
	}
	
	public void collectDeeply(IResource resource) {
		try {
			set.collect(resource, (IProgressMonitor)null);
			if (resource.getType() != IResource.FILE) {
				collectMembers((IContainer) resource);
			}
		} catch (TeamException e) {
			// TODO Auto-generated catch block
			TeamUIPlugin.log(e);
		}
	}
	private void collectMembers(IContainer container) {
		try {
			IResource[] members = set.getSubscriber().members(container);
			for (int i = 0; i < members.length; i++) {
				IResource resource = members[i];
				collectDeeply(resource);
			}
		} catch (TeamException e) {
			// TODO Auto-generated catch block
			TeamUIPlugin.log(e);
		}
	}
}
