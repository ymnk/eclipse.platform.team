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
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.core.Assert;
import org.eclipse.team.internal.core.Policy;
import org.eclipse.team.internal.core.subscribers.SubscriberEventHandler;
import org.eclipse.team.internal.core.subscribers.SyncSetInputFromSubscriber;

/**
 * This collector maintains a {@link SyncInfoSet} for a particular team subscriber keeping
 * it up-to-date with both incoming changes and outgoing changes as they occur for 
 * resources in the workspace. The collector can be configured to consider all the subscriber's
 * roots or only a subset.
 * <p>
 * The advantage of this collector is that it processes both resource and team
 * subscriber deltas in a background thread.
 * </p>
 * @since 3.0
 */
public final class SubscriberSyncInfoCollector implements IResourceChangeListener, ISubscriberChangeListener {

	private SyncSetInputFromSubscriber set;
	private SubscriberEventHandler eventHandler;
	private Subscriber subscriber;
	private IResource[] roots;

	/**
	 * Create a collector on the subscriber that collects out-of-sync resources
	 * for all roots of the subscriber.
	 * @param subscriber the Subscriber
	 */
	public SubscriberSyncInfoCollector(Subscriber subscriber) {
		this(subscriber, null /* use the subscriber roots */);
	}
	
	/**
	 * Create a collector that collects out-of-sync resources that are children of
	 * the given roots. If the roots are <code>null</code>, then all out-of-sync resources
	 * from the subscriber are collected. An empty array of roots will cause no resources
	 * to be collected.
	 * @param subscriber the Subscriber
	 * @param roots the roots of the out-of-sync resources to be collected
	 */
	public SubscriberSyncInfoCollector(Subscriber subscriber, IResource[] roots) {
		this.roots = roots;
		this.subscriber = subscriber;
		Assert.isNotNull(subscriber);
		set = new SyncSetInputFromSubscriber(subscriber);
		eventHandler = new SubscriberEventHandler(set);

		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
		subscriber.addListener(this);
	}
	
	/**
	 * Return the set that provides access to the out-of-sync resources for the collector's
	 * subscriber. The set will contain only those resources that are children of the roots
	 * of the collector unless the roots of the colletor has been set to <code>null</code>
	 * in which case all out-of-sync resources from the subscriber are collected.
	 * @return a SyncInfoSet containing out-of-sync resources
	 */
	public SyncInfoSet getSyncInfoSet() {
		return set.getSyncSet();
	}

	/**
	 * This causes the calling thread to wait any background collection of out-of-sync resources
	 * to stop before returning.
	 * @param monitor a progress monitor
	 */
	public void waitForCollector(IProgressMonitor monitor) {
		monitor.worked(1);
		// wait for the event handler to process changes.
		while(eventHandler.getEventHandlerJob().getState() != Job.NONE) {
			monitor.worked(1);
			try {
				Thread.sleep(10);		
			} catch (InterruptedException e) {
			}
		}
		monitor.worked(1);
	}
	
	/**
	 * Clears this collector's <code>SyncInfoSet</code> and causes it to be recreated from the
	 * associated <code>Subscriber</code>. The reset may occur in the background. If the
	 * caller wishes to wait for the reset to complete, they should call \
	 * {@link waitForCollector(IProgressMonitor)}.
	 * @param monitor a progress monitor
	 * @throws TeamException
	 */
	public void reset(IProgressMonitor monitor) throws TeamException {
		monitor.beginTask(null, 100);
		set.reset(Policy.subMonitorFor(monitor, 100));
		// TODO: This is a problem
		eventHandler.initialize(getRoots());
		monitor.done();
	}

	/**
	 * Returns the <code>Subscriber</code> associated with this collector.
	 * 
	 * @return the <code>Subscriber</code> associated with this collector.
	 */
	public Subscriber getTeamSubscriber() {
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
			if (!isAncestorOfRoot(resource)) {
				// If the project has any entries in the sync set, remove them
				if (getSyncInfoSet().hasMembers(resource)) {
					eventHandler.remove(resource);
				}
				return;
			}
		}

		boolean visitChildren = false;
		if (isDescendantOfRoot(resource)) {
			visitChildren = true;
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
		}

		// Handle changed children
		if (visitChildren || isAncestorOfRoot(resource)) {
			IResourceDelta[] affectedChildren = delta.getAffectedChildren(IResourceDelta.CHANGED | IResourceDelta.REMOVED | IResourceDelta.ADDED);
			for (int i = 0; i < affectedChildren.length; i++) {
				processDelta(affectedChildren[i]);
			}
		}
	}

	private boolean isAncestorOfRoot(IResource parent) {
		// Always traverse into projects in case a root was removed
		if (parent.getType() == IResource.ROOT) return true;
		IResource[] roots = getRoots();
		for (int i = 0; i < roots.length; i++) {
			IResource resource = roots[i];
			if (parent.getFullPath().isPrefixOf(resource.getFullPath())) {
				return true;
			}
		}
		return false;
	}

	private boolean isDescendantOfRoot(IResource resource) {
		IResource[] roots = getRoots();
		for (int i = 0; i < roots.length; i++) {
			IResource root = roots[i];
			if (root.getFullPath().isPrefixOf(resource.getFullPath())) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Return the roots that are being considered by this collector.
	 * By default, the collector is interested in the roots of its
	 * subscriber. However, the set can be reduced using {@link setRoots(IResource)).
	 * @return
	 */
	public IResource[] getRoots() {
		if (roots == null) {
			return getTeamSubscriber().roots();
		} else {
			return roots;
		}
	}
	
	/*
	 * Returns whether the collector is configured to collect for
	 * all roots of the subscriber or not
	 * @return <code>true</code> if the collector is considering all 
	 * roots of the subscriber and <code>false</code> otherwise
	 */
	private boolean isAllRootsIncluded() {
		return roots == null;
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
	public void teamResourceChanged(SubscriberChangeEvent[] deltas) {
		for (int i = 0; i < deltas.length; i++) {
			switch (deltas[i].getFlags()) {
				case ISubscriberChangeEvent.SYNC_CHANGED :
					if (isAllRootsIncluded() || isDescendantOfRoot(deltas[i].getResource())) {
						eventHandler.change(deltas[i].getResource(), IResource.DEPTH_ZERO);
					}
					break;
				case ISubscriberChangeEvent.ROOT_REMOVED :
					eventHandler.remove(deltas[i].getResource());
					break;
				case ISubscriberChangeEvent.ROOT_ADDED :
					if (isAllRootsIncluded() || isDescendantOfRoot(deltas[i].getResource())) {
						eventHandler.change(deltas[i].getResource(), IResource.DEPTH_INFINITE);
					}
					break;
			}
		}
	}
	
	/**
	 * Set the roots that are to be considered by the collector. The provided
	 * resources should be either a subset of the roots of the collector's subscriber
	 * of children of those roots. Other resources can be provided but will be ignored.
	 * Setting the roots to <code>null</code> will cause the roots of the subscriber
	 * to be used
	 * @param roots The roots to be considered or <code>null</code>.
	 */
	public void setRoots(IResource[] roots) {
		this.roots = roots;
	}
}
