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

import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.team.internal.core.Policy;
import org.eclipse.team.internal.core.subscribers.SyncSetChangedEvent;

public class MutableSyncInfoSet extends SyncInfoSet {

	private SyncSetChangedEvent changes = new SyncSetChangedEvent(this);
	private ILock lock = Platform.getJobManager().newLock();
	private Set listeners = Collections.synchronizedSet(new HashSet());
	
	public MutableSyncInfoSet() {
	}
	
	public MutableSyncInfoSet(SyncInfo[] infos) {
		super(infos);
	}
	
	/**
	 * Registers the given listener for sync info set notifications. Has
	 * no effect if an identical listener is already registered.
	 * 
	 * @param listener listener to register
	 */
	public void addSyncSetChangedListener(ISyncSetChangedListener listener) {
		synchronized(listeners) {
			listeners.add(listener);
		}
	}

	/**
	 * Deregisters the given listener for participant notifications. Has
	 * no effect if listener is not already registered.
	 * 
	 * @param listener listener to deregister
	 */
	public void removeSyncSetChangedListener(ISyncSetChangedListener listener) {
		synchronized(listeners) {
			listeners.remove(listener);
		}
	}
	
	/**
	 * Add the given <code>SyncInfo</code> to the set. An change event will
	 * be generated unless the call to this method is nested in between calls
	 * to <code>beginInput()</code> and <code>endInput(IProgressMonitor)</code>
	 * in which case the event for this addition and any other sync set
	 * change will be fired in a batched event when <code>endInput</code>
	 * is invoked.
	 * Invoking this method outside of the above mentioned block will result
	 * in the <code>endInput(IProgressMonitor)</code> being invoked with a null
	 * progress monitor. If responsiveness is required, the client should always
	 * nest sync set modifications.
	 * @param info
	 */
	public void add(SyncInfo info) {
		try {
			beginInput();
			internalAdd(info);
			changes.added(info);
		} finally {
			endInput(null);
		}
	}
	
	public void addAll(SyncInfoSet set) {
		try {
			beginInput();
			SyncInfo[] infos = set.members();
			for (int i = 0; i < infos.length; i++) {
				add(infos[i]);
			}
		} finally {
			endInput(null);
		}
	}
	
	public void changed(SyncInfo info) {
		try {
			beginInput();
			internalChange(info);
			changes.changed(info);
		} finally {
			endInput(null);
		}
	}

	public void remove(IResource local) {
		try {
			beginInput();
			SyncInfo info = internalRemove(local);
			changes.removed(local, info);
		} finally {
			endInput(null);
		}

	}

	/**
	 * Reset the sync set so it is empty
	 */
	public void clear() {
		try {
			beginInput();
			super.clear();
			changes.reset();
		} finally {
			endInput(null);
		}
	}

	public void remove(IResource resource, boolean recurse) {
		try {
			beginInput();
			if (getSyncInfo(resource) != null) {
				remove(resource);
			}
			if (recurse) {
				IResource [] removed = internalGetDeepSyncInfo(resource);
				for (int i = 0; i < removed.length; i++) {
					remove(removed[i]);
				}
			}
		} finally {
			endInput(null);
		}
	}

	/**
	 * This method is invoked by a SyncSetInput provider when the 
	 * provider is starting to provide new input to the SyncSet
	 */
	public void beginInput() {
		lock.acquire();
	}

	/**
	 * This method is invoked by a SyncSetInput provider when the 
	 * provider is done providing new input to the SyncSet
	 */
	public void endInput(IProgressMonitor monitor) {
		if (lock.getDepth() == 1) {
			// Remain locked while firing the events so the handlers 
			// can expect the set to remain constant while they process the events
			fireChanges(Policy.monitorFor(monitor));
		}
		lock.release();
	}
	
	private void resetChanges() {
		changes = new SyncSetChangedEvent(this);
	}

	/**
	 * Fire an event to all listeners containing the events (add, remove, change)
	 * accumulated so far. 
	 * @param monitor the progress monitor
	 */
	private void fireChanges(final IProgressMonitor monitor) {
		// Use a synchronized block to ensure that the event we send is static
		final SyncSetChangedEvent event;
		synchronized(this) {
			event = changes;
			resetChanges();
		}
		// Ensure that the list of listeners is not changed while events are fired.
		// Copy the listeners so that addition/removal is not blocked by event listeners
		if(event.isEmpty() && ! event.isReset()) return;
		ISyncSetChangedListener[] allListeners;
		synchronized(listeners) {
			allListeners = (ISyncSetChangedListener[]) listeners.toArray(new ISyncSetChangedListener[listeners.size()]);
		}
		// Fire the events using an ISafeRunnable
		monitor.beginTask(null, 100 * allListeners.length);
		for (int i = 0; i < allListeners.length; i++) {
			final ISyncSetChangedListener listener = allListeners[i];
			Platform.run(new ISafeRunnable() {
				public void handleException(Throwable exception) {
					// don't log the exception....it is already being logged in Platform#run
				}
				public void run() throws Exception {
					listener.syncSetChanged(event, Policy.subMonitorFor(monitor, 100));
	
				}
			});
		}
		monitor.done();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.SyncInfoSet#internalAddedSubtreeRoot(org.eclipse.core.resources.IResource)
	 */
	protected void internalAddedSubtreeRoot(IResource parent) {
		changes.addedSubtreeRoot(parent);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.SyncInfoSet#internalRemovedSubtreeRoot(org.eclipse.core.resources.IResource)
	 */
	protected void internalRemovedSubtreeRoot(IResource parent) {
		changes.removedSubtreeRoot(parent);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.SyncInfoSet#run(org.eclipse.core.resources.IWorkspaceRunnable, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IWorkspaceRunnable runnable, IProgressMonitor monitor) throws CoreException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(null, 100);
		try {
			beginInput();
			super.run(runnable, Policy.subMonitorFor(monitor, 80));
		} finally {
			endInput(Policy.subMonitorFor(monitor, 20));
		}
		
	}

}
