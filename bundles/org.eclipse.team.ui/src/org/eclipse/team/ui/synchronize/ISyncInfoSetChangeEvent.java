/*
 * Created on Dec 14, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.team.ui.synchronize;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.ui.synchronize.sets.SyncSet;

/**
 * @author Jean-Michel Lemieux
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
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