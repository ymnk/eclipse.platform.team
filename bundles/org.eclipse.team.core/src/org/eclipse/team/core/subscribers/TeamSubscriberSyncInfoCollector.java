/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.core.subscribers;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.core.Assert;
import org.eclipse.team.internal.core.subscribers.SubscriberEventHandler;
import org.eclipse.team.internal.core.subscribers.SyncSetInputFromSubscriber;

/**
 * This collector maintains a {@link SyncInfoSet} for a particular team subscriber keeping
 * it up-to-date with both incoming changes and outgoing changes as they occur for 
 * resources in the workspace.
 * <p>
 * The advantage of this collector is that it processes both resource and team
 * subscriber deltas in a background thread.
 * </p>
 * @since 3.0
 */
public class TeamSubscriberSyncInfoCollector implements IResourceChangeListener, ITeamResourceChangeListener {

	private SyncSetInputFromSubscriber set;
	private SubscriberEventHandler eventHandler;
	private TeamSubscriber subscriber;

	public TeamSubscriberSyncInfoCollector(TeamSubscriber subscriber) {
		this.subscriber = subscriber;
		Assert.isNotNull(subscriber);
		set = new SyncSetInputFromSubscriber(subscriber);
		eventHandler = new SubscriberEventHandler(set);

		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
		subscriber.addListener(this);
	}

	public SyncInfoSet getSyncInfoSet() {
		return set.getSyncSet();
	}

	public void waitForCollector() {
		try {
			eventHandler.getEventHandlerJob().join();
		} catch (InterruptedException e) {
			// continue
		}
	}
	
	/**
	 * Clears this collector's <code>SyncInfoSet</code> and causes it to be recreated from the
	 * associated <code>TeamSubscriber</code>. 
	 * @param monitor
	 * @throws TeamException
	 */
	public void reset(IProgressMonitor monitor) throws TeamException {
		set.reset(monitor);
		eventHandler.initialize();
	}

	/**
	 * Returns the <code>TeamSubscriber</code> associated with this collector.
	 * 
	 * @return the <code>TeamSubscriber</code> associated with this collector.
	 */
	public TeamSubscriber getTeamSubscriber() {
		return subscriber;
	}

	/**
	 * Disposes of the background job associated with this collector and deregisters
	 * all it's listeners. This method must be called when the collector is no longer
	 * referenced and could be garbage collected.
	 */
	public void dispose() {
		eventHandler.shutdown();

		set.disconnect();

		getTeamSubscriber().removeListener(this);		
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
	}

	/**
	 * Process the resource delta and posts all necessary events to the background
	 * event handler.
	 * 
	 * @param delta the resource delta to analyse
	 */
	private void processDelta(IResourceDelta delta) {
		IResource resource = delta.getResource();
		int kind = delta.getKind();

		if (resource.getType() == IResource.PROJECT) {
			// Handle a deleted project
			if (((kind & IResourceDelta.REMOVED) != 0)) {
				eventHandler.remove(resource);
				return;
			}
			// Handle a closed project
			if ((delta.getFlags() & IResourceDelta.OPEN) != 0 && !((IProject) resource).isOpen()) {
				eventHandler.remove(resource);
				return;
			}
			// Only interested in projects mapped to the provider
			if (!isVisibleProject((IProject) resource)) {
				// If the project has any entries in the sync set, remove them
				if (getSyncInfoSet().hasMembers(resource)) {
					eventHandler.remove(resource);
				}
				return;
			}
		}

		// If the resource has changed type, remove the old resource handle
		// and add the new one
		if ((delta.getFlags() & IResourceDelta.TYPE) != 0) {
			eventHandler.remove(resource);
			eventHandler.change(resource, IResource.DEPTH_INFINITE);
		}

		// Check the flags for changes the SyncSet cares about.
		// Notice we don't care about MARKERS currently.
		int changeFlags = delta.getFlags();
		if ((changeFlags & (IResourceDelta.OPEN | IResourceDelta.CONTENT)) != 0) {
			eventHandler.change(resource, IResource.DEPTH_ZERO);
		}

		// Check the kind and deal with those we care about
		if ((delta.getKind() & (IResourceDelta.REMOVED | IResourceDelta.ADDED)) != 0) {
			eventHandler.change(resource, IResource.DEPTH_ZERO);
		}

		// Handle changed children .
		IResourceDelta[] affectedChildren = delta.getAffectedChildren(IResourceDelta.CHANGED | IResourceDelta.REMOVED | IResourceDelta.ADDED);
		for (int i = 0; i < affectedChildren.length; i++) {
			processDelta(affectedChildren[i]);
		}
	}

	private boolean isVisibleProject(IProject project) {
		IResource[] roots = getTeamSubscriber().roots();
		for (int i = 0; i < roots.length; i++) {
			IResource resource = roots[i];
			if (project.getFullPath().isPrefixOf(resource.getFullPath())) {
				return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		processDelta(event.getDelta());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.core.sync.ITeamResourceChangeListener#teamResourceChanged(org.eclipse.team.core.sync.TeamDelta[])
	 */
	public void teamResourceChanged(TeamDelta[] deltas) {
		for (int i = 0; i < deltas.length; i++) {
			switch (deltas[i].getFlags()) {
				case TeamDelta.SYNC_CHANGED :
					eventHandler.change(deltas[i].getResource(), IResource.DEPTH_ZERO);
					break;
				case TeamDelta.PROVIDER_DECONFIGURED :
					eventHandler.remove(deltas[i].getResource());
					break;
				case TeamDelta.PROVIDER_CONFIGURED :
					eventHandler.change(deltas[i].getResource(), IResource.DEPTH_INFINITE);
					break;
			}
		}
	}
}
