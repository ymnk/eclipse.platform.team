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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.sync.SyncInfo;

/**
 * This event keeps track of the changes in a sync set
 */
public class SyncSetChangedEvent {
	
	private SyncSet set;
	
	// List that accumulate changes
	// SyncInfo
	private Set changedResources = new HashSet();
	private Set removedResources = new HashSet();
	private Set addedResources = new HashSet();
	
	// IResources
	private Set removedRoots = new HashSet();
	private Set addedRoots = new HashSet();
	
	private boolean reset = false;
	
	/**
	 * 
	 */
	public SyncSetChangedEvent(SyncSet set) {
		super();
		this.set = set;
	}

	/* package */ void added(SyncInfo info) {
		addedResources.add(info);
	}
	
	/* package */ void removed(SyncInfo info) {
		removedResources.add(info);
	}
	
	/* package */ void changed(SyncInfo info) {
		changedResources.add(info);
	}
	
	/**
	 * @param parent
	 */
	public void removedRoot(IResource parent) {
		if (addedRoots.contains(parent)) {
			// The root was added and removed which is a no-op
			addedRoots.remove(parent);
		} else {
			// TODO: handle children in removedRoots
			removedRoots.add(parent);
		}
	}
	/**
	 * @param parent
	 */
	public void addedRoot(IResource parent) {
		if (removedRoots.contains(parent)) {
			// The root was re-added which is a no-op
			removedRoots.remove(parent);
		} else {
			// TODO: May be added underneath another added root
			addedRoots.add(parent);
		}
		
	}
	/**
	 * @return
	 */
	public SyncInfo[] getAddedResources() {
		return (SyncInfo[]) addedResources.toArray(new SyncInfo[addedResources.size()]);
	}

	/**
	 * @return
	 */
	public IResource[] getAddedRoots() {
		return (IResource[]) addedRoots.toArray(new IResource[addedRoots.size()]);
	}

	/**
	 * @return
	 */
	public SyncInfo[] getChangedResources() {
		return (SyncInfo[]) changedResources.toArray(new SyncInfo[changedResources.size()]);
	}

	/**
	 * @return
	 */
	public SyncInfo[] getRemovedResources() {
		return (SyncInfo[]) removedResources.toArray(new SyncInfo[removedResources.size()]);
	}

	/**
	 * @return
	 */
	public IResource[] getRemovedRoots() {
		return (IResource[]) removedRoots.toArray(new IResource[removedRoots.size()]);
	}
		
	/**
	 * @return
	 */
	public SyncSet getSet() {
		return set;
	}

	/**
	 * 
	 */
	public void reset() {
		reset = true;
	}
	
	public boolean isReset() {
		return reset;
	}

}
