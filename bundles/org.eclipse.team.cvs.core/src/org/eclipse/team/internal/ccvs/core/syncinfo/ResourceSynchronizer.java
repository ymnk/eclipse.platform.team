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
package org.eclipse.team.internal.ccvs.core.syncinfo;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSRunnable;
import org.eclipse.team.internal.ccvs.core.Policy;

/**
 * A resource synchronizer is responsible for managing synchronization information for
 * CVS resources.
 */
public abstract class ResourceSynchronizer {

	// TODO: Initially we are using a single lock for all CVS sync operations.
	// This may be overly restrictive so we may want to investigate using separate
	// locks.
	private static ReentrantLock lock = new ReentrantLock();

	/**
	 * Begins a batch of operations.
	 * 
	 * @param monitor the progress monitor, may be null
	 */
	public void beginOperation(IProgressMonitor monitor) throws CVSException {
		lock.acquire();		
	}

	/**
	 * Ends a batch of operations.  Pending changes are committed only when
	 * the number of calls to endOperation() balances those to beginOperation().
	 * <p>
	 * Progress cancellation is ignored while writting the cache to disk. This
	 * is to ensure cache to disk consistency.
	 * </p>
	 * 
	 * @param monitor the progress monitor, may be null
	 * @exception CVSException with a status with code <code>COMMITTING_SYNC_INFO_FAILED</code>
	 * if all the CVS sync information could not be written to disk.
	 */
	public void endOperation(IProgressMonitor monitor) throws CVSException {		
		lock.release();
	}
	
	/**
	 * Return true if the synchronizer is in the outer most operation of a set of nested
	 * operations
	 * @return
	 */
	protected boolean isOuterOperation() {
		return lock.getNestingCount() == 1;
	}
	
	/**
	 * Register the given thread as a thread that should be restricted to having 
	 * read-only access. If a thread is not registered, it is expected that they 
	 * obtain the workspace lock before accessing any CVS sync information.
	 * @param thread
	 */
	public void addReadOnlyThread(Thread thread) {
		lock.addReadOnlyThread(thread);
	}
	
	/**
	 * If this method return false, the caller should not perform any workspace modification
	 * operations. The danger of performing such an operation is deadlock.
	 * 
	 * @return boolean
	 */
	public boolean isWorkspaceModifiable() {
		return !lock.isReadOnly();
	}

	/**
	 * Obtain the CVS sync lock while running the given ICVSRunnable.
	 * @param job
	 * @param monitor
	 * @throws CVSException
	 */
	public void run(ICVSRunnable job, IProgressMonitor monitor) throws CVSException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(null, 100);
		try {
			beginOperation(Policy.subMonitorFor(monitor, 5));
			job.run(Policy.subMonitorFor(monitor, 60));
		} finally {
			endOperation(Policy.subMonitorFor(monitor, 35));
			monitor.done();
		}
	}
	
	public abstract byte[] getSyncBytes(IResource resource) throws CVSException;
}
