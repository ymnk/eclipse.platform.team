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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.internal.ccvs.core.Policy;
import org.eclipse.team.internal.ccvs.core.util.Assert;

/**
 * Provides a per-thread nested locking mechanism. A thread can acquire a
 * lock on a specific resource by calling acquire(). Subsequently, acquire() can be called
 * multiple times on the resource or any of its children from within the same thread
 * without blocking. Other threads that try
 * and acquire the lock on those same resources will be blocked until the first 
 * thread releases all it's nested locks.
 * <p>
 * The locking is managed by the platform via scheduling rules. This class simply 
 * provides the nesting mechnism in order to allow the client to determine when
 * the lock for the thread has been released. Therefore, this lock will block if
 * another thread already locks the same resource.</p>
 */
public class ReentrantLock {

	private final static boolean DEBUG = Policy.DEBUG_THREADING;
	
	private Map nestingCounts = new HashMap();
	
	
	public ReentrantLock() {
	}
	
	public synchronized void acquire(IResource resource) {
		lock(resource);	
		incrementNestingCount(resource);
	}
	
	private void incrementNestingCount(IResource resource) {
		Thread thisThread = Thread.currentThread();
		Integer wrapped = (Integer)nestingCounts.get(thisThread);
		int nestingCount ;
		if (wrapped == null) {
			nestingCount = 1;
			if(DEBUG) System.out.println("[" + thisThread.getName() + "] acquired CVS lock on " + resource.getFullPath()); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			nestingCount = (wrapped.intValue());
			nestingCount++;
		}
		nestingCounts.put(thisThread, new Integer(nestingCount));
	}
	
	private void lock(IResource resource) {
		// The scheduling rule is either the project or the resource's parent
		ISchedulingRule rule;
		if (resource.getType() == IResource.ROOT) {
			// Never lock the whole workspace
			rule = new ISchedulingRule() {
				public boolean contains(ISchedulingRule rule) {
					return false;
				}
				public boolean isConflicting(ISchedulingRule rule) {
					return false;
				}
			};
		} else  if (resource.getType() == IResource.PROJECT) {
			rule = resource;
		} else {
			rule = resource.getParent();
		}
		Platform.getJobManager().beginRule(rule);
	}

	private void unlock() {
		Platform.getJobManager().endRule();
	}
	
	/**
	 * Release the lock held on any resources by this thread. Return true
	 * if the thread no longer holds the lock (i.e. nesting count is 0).
	 */
	public synchronized boolean release() {
		Thread thisThread = Thread.currentThread();
		Integer wrapped = (Integer)nestingCounts.get(thisThread);
		Assert.isNotNull(wrapped, "Unmatched acquire/release."); //$NON-NLS-1$
		int nestingCount = (wrapped.intValue());
		Assert.isTrue(nestingCount > 0, "Unmatched acquire/release."); //$NON-NLS-1$
		if (--nestingCount == 0) {
			if(DEBUG) System.out.println("[" + thisThread.getName() + "] released CVS lock"); //$NON-NLS-1$ //$NON-NLS-2$
			nestingCounts.remove(thisThread);
			unlock();
			return true;
		} else {
			nestingCounts.put(thisThread, new Integer(nestingCount));
			return false;
		}
	}
}
