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

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.ui.synchronize.sets.SyncSet;

public interface ISyncInfoSetChangeEvent {
	public abstract void removedRoot(IResource root);
	public abstract void addedRoot(IResource parent);
	public abstract SyncInfo[] getAddedResources();
	public abstract IResource[] getAddedRoots();
	public abstract SyncInfo[] getChangedResources();
	public abstract IResource[] getRemovedResources();
	public abstract IResource[] getRemovedRoots();
	public abstract SyncSet getSet();
	public abstract void reset();
	public abstract boolean isReset();
	public abstract boolean isEmpty();
}