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
package org.eclipse.team.core.subscribers.trees;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ISynchronizer;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.sync.IRemoteResource;
import org.eclipse.team.internal.core.Assert;
import org.eclipse.team.internal.core.Policy;

public abstract class CoreSynchronizerRemoteHandleTree extends RemoteHandleTree {

	private static final byte[] NO_REMOTE = new byte[0];
	private static final String SYNC_KEY_QUALIFIER = "org.eclipse.team.core.remote_handle_tree"; //$NON-NLS-1$
	private QualifiedName syncName;
	private Set changedResources = new HashSet();
	
	public CoreSynchronizerRemoteHandleTree(String id) {
		this.syncName = new QualifiedName(SYNC_KEY_QUALIFIER, id);
		getSynchronizer().add(syncName);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.trees.RemoteHandleTree#dispose()
	 */
	public void dispose() {
		getSynchronizer().remove(getSyncName());
	}
	
	protected ISynchronizer getSynchronizer() {
		return ResourcesPlugin.getWorkspace().getSynchronizer();
	}

	protected QualifiedName getSyncName() {
		return syncName;
	}

	protected void setSyncBytes(IResource resource, byte[] bytes) throws TeamException {
		byte[] oldBytes = getSyncBytes(resource);
		if (oldBytes != null && bytesEqual(oldBytes, bytes)) return;
		try {
			getSynchronizer().setSyncInfo(getSyncName(), resource, bytes);
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}
		changedResources.add(resource);
	}

	protected void removeSyncBytes(IResource resource, int depth, boolean silent) throws TeamException {
		if (resource.exists() || resource.isPhantom()) {
			try {
				getSynchronizer().flushSyncInfo(getSyncName(), resource, depth);
			} catch (CoreException e) {
				throw TeamException.asTeamException(e);
			}
			if(silent == false) {
				changedResources.add(resource);
			}
		}
	}

	private boolean bytesEqual(byte[] syncBytes, byte[] oldBytes) {
		if (syncBytes.length != oldBytes.length) return false;
		for (int i = 0; i < oldBytes.length; i++) {
			if (oldBytes[i] != syncBytes[i]) return false;
		}
		return true;
	}
	
	protected void collectChanges(IResource local, IRemoteResource remote, int depth, IProgressMonitor monitor) throws TeamException {
		byte[] remoteBytes;
		if (remote != null) {
			remoteBytes = toBytes(remote);
		} else {
			remoteBytes = NO_REMOTE;
		}
		setSyncBytes(local, remoteBytes);
		if (depth == IResource.DEPTH_ZERO) return;
		Map children = mergedMembers(local, remote, monitor);	
		for (Iterator it = children.keySet().iterator(); it.hasNext();) {
			IResource localChild = (IResource) it.next();
			IRemoteResource remoteChild = (IRemoteResource)children.get(localChild);
			collectChanges(localChild, remoteChild, 
				depth == IResource.DEPTH_INFINITE ? IResource.DEPTH_INFINITE : IResource.DEPTH_ZERO, 
				monitor);
		}
	}

	protected Map mergedMembers(IResource local, IRemoteResource remote, IProgressMonitor progress) throws TeamException {

		// {IResource -> IRemoteResource}
		Map mergedResources = new HashMap();
	
		IRemoteResource[] remoteChildren = getRemoteChildren(remote, progress);
	
		IResource[] localChildren = getLocalChildren(local);		

		if (remoteChildren.length > 0 || localChildren.length > 0) {
			List syncChildren = new ArrayList(10);
			Set allSet = new HashSet(20);
			Map localSet = null;
			Map remoteSet = null;

			if (localChildren.length > 0) {
				localSet = new HashMap(10);
				for (int i = 0; i < localChildren.length; i++) {
					IResource localChild = localChildren[i];
					String name = localChild.getName();
					localSet.put(name, localChild);
					allSet.add(name);
				}
			}

			if (remoteChildren.length > 0) {
				remoteSet = new HashMap(10);
				for (int i = 0; i < remoteChildren.length; i++) {
					IRemoteResource remoteChild = remoteChildren[i];
					String name = remoteChild.getName();
					remoteSet.put(name, remoteChild);
					allSet.add(name);
				}
			}
	
			Iterator e = allSet.iterator();
			while (e.hasNext()) {
				String keyChildName = (String) e.next();

				if (progress != null) {
					if (progress.isCanceled()) {
						throw new OperationCanceledException();
					}
					// XXX show some progress?
				}

				IResource localChild =
					localSet != null ? (IResource) localSet.get(keyChildName) : null;

				IRemoteResource remoteChild =
					remoteSet != null ? (IRemoteResource) remoteSet.get(keyChildName) : null;
			
				if (localChild == null) {
					// there has to be a remote resource available if we got this far
					Assert.isTrue(remoteChild != null);
					boolean isContainer = remoteChild.isContainer();				
					localChild = getResourceChild(local /* parent */, keyChildName, isContainer);
				}
				mergedResources.put(localChild, remoteChild);				
			}
		}
		return mergedResources;
	}

	private IRemoteResource[] getRemoteChildren(IRemoteResource remote, IProgressMonitor progress) throws TeamException {
		return remote != null ? remote.members(progress) : new IRemoteResource[0];
	}

	protected abstract IResource[] getLocalChildren(IResource local) throws TeamException;

	/*
	 * Returns a handle to a non-existing resource.
	 */
	private IResource getResourceChild(IResource parent, String childName, boolean isContainer) {
		if (parent.getType() == IResource.FILE) {
			return null;
		}
		if (isContainer) {
			return ((IContainer) parent).getFolder(new Path(childName));
		} else {
			return ((IContainer) parent).getFile(new Path(childName));
		}
	}

	/**
	 * Return true if remote bytes for the resource have been fetched during
	 * a refresh. This will also return true for remote resources that do not exist
	 * (i.e. they have no sync bytes but did not exist remotely at the time of the
	 * last refresh.
	 * 
	 * @param resource
	 * @return
	 */
	protected boolean hasRemoteBytesFor(IResource resource) throws CVSException {
		return super.getSyncBytes(resource) != null;
	}

	/**
	 * This method will return null in both cases when the remote has never been fetched
	 * or when the remote does not exist. Use <code>hasRemoteBytesFor</code> to
	 * differentiate these cases.  
	 */
	public byte[] getSyncBytes(IResource resource) throws CVSException {
		byte[] syncBytes = super.getSyncBytes(resource);
		if (syncBytes != null && Util.equals(syncBytes, NO_REMOTE)) {
			// If it is known that there is no remote, return null
			return null;
		}
		return syncBytes;
	}

	public IResource[] getChangedResources() {
		return (IResource[]) changedResources.toArray(new IResource[changedResources.size()]);
	}

	public void resetChanges() {
		changedResources.clear();
	}

	/**
	 * Refreshes the contents of the remote synchronizer and returns the list
	 * of resources whose remote sync state changed.
	 * 
	 * @param resources
	 * @param depth
	 * @param monitor
	 * @return
	 * @throws TeamException
	 */
	public IResource[] refresh(IResource[] resources, int depth, IProgressMonitor monitor) throws TeamException {
		int work = 100 * resources.length;
		monitor.beginTask(null, work);
		resetChanges();
		try {
			for (int i = 0; i < resources.length; i++) {
				IResource resource = resources[i];	
			
				// build the remote tree only if an initial tree hasn't been provided
				ICVSRemoteResource	tree = buildRemoteTree(resource, depth, Policy.subMonitorFor(monitor, 70));
			
				// update the known remote handles 
				IProgressMonitor sub = Policy.infiniteSubMonitorFor(monitor, 30);
				try {
					sub.beginTask(null, 512);
					//removeSyncBytes(resource, IResource.DEPTH_INFINITE);
					collectChanges(resource, tree, depth, sub);
				} finally {
					sub.done();	 
				}
			}
		} finally {
			monitor.done();
		}
		IResource[] changes = getChangedResources();
		resetChanges();
		return changes;
	}

	/**
	 * Build a remote tree for the given parameters.
	 */
	protected ICVSRemoteResource buildRemoteTree(IResource resource, int depth, IProgressMonitor monitor) throws TeamException {
		// TODO: we are currently ignoring the depth parameter because the build remote tree is
		// by default deep!
		return CVSWorkspaceRoot.getRemoteTree(resource, tag, monitor);
	}
	
	protected abstract byte[] toBytes(IRemoteResource remoteResource);
}