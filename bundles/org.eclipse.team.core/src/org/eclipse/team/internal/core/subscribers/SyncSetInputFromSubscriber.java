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
package org.eclipse.team.internal.core.subscribers;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;

/**
 * Records resource synchronization changes from a Team subscriber. The actual changes 
 * are calculated via the SubscriberEventHandler and stored in this input.
 */
public class SyncSetInputFromSubscriber extends SyncSetInput  {

	private Subscriber subscriber;

	public SyncSetInputFromSubscriber(Subscriber subscriber, SubscriberEventHandler handler) {
		super(handler);
		this.subscriber = subscriber;
	}
		
	public void disconnect() {
	}
	
	public Subscriber getSubscriber() {
		return subscriber;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.sync.views.SyncSetInput#fetchInput(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void fetchInput(IProgressMonitor monitor) throws TeamException {
		// don't calculate changes. The SubscriberEventHandler will fetch the
		// input in a job and update this sync set when the changes are 
		// calculated. 
	}

	/**
	 * Handle an error that occurred while populating the receiver's set.
	 * This error should be propogated to any set listeners.
	 * @param e the error
	 * @param resource the resource
	 */
	public void handleError(CoreException e, IResource resource, IProgressMonitor monitor) {
		getSyncSet().handleErrorEvent(new SubscriberErrorEvent(e, resource), monitor);
	}
}
