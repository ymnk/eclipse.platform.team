/*
 * Created on Dec 14, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.team.ui.synchronize;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.ui.synchronize.sets.SyncInfoStatistics;

/**
 * @author Jean-Michel Lemieux
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public interface ISyncInfoSet {
	/**
	 * Add a change listener
	 * @param provider
	 */
	public abstract void addSyncSetChangedListener(ISyncSetChangedListener listener);
	/**
	 * Remove a change listener
	 * @param provider
	 */
	public abstract void removeSyncSetChangedListener(ISyncSetChangedListener listener);
	public abstract void add(SyncInfo info);
	/**
	 * Reset the sync set so it is empty
	 */
	public abstract void reset();
	/**
	 * Return the children of the given container who are either out-of-sync or contain
	 * out-of-sync resources.
	 * 
	 * @param container
	 * @return
	 */
	public abstract IResource[] members(IResource resource);
	/**
	 * Return the out-of-sync descendants of the given resource. If the given resource
	 * is out of sync, it will be included in the result.
	 * 
	 * @param container
	 * @return
	 */
	public abstract SyncInfo[] getOutOfSyncDescendants(IResource resource);
	/**
	 * Return an array of all the resources that are known to be out-of-sync
	 * @return
	 */
	public abstract SyncInfo[] allMembers();
	public abstract SyncInfo getSyncInfo(IResource resource);
	public abstract int size();
	public abstract SyncInfoStatistics getStatistics();
	/**
	 * Return wether the given resource has any children in the sync set
	 * @param resource
	 * @return
	 */
	public abstract boolean hasMembers(IResource resource);
}