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

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.core.sync.IRemoteResource;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.synchronize.compare.LocalResourceTypedElement;
import org.eclipse.team.internal.ui.synchronize.compare.RemoteResourceTypedElement;

public class SyncInfoDiffNode extends DiffNode implements IAdaptable {
	
	private IResource resource;
	private SyncInfoSet input;
	private SyncInfo info;
		
	/**
	 * Create an ITypedElement for the given local resource. The returned ITypedElement
	 * will prevent editing of outgoing deletions.
	 */
	public static ITypedElement createTypeElement(IResource resource, final int kind) {
		if(resource != null && resource.exists()) {
			return new LocalResourceTypedElement(resource) {
				public boolean isEditable() {
						if(SyncInfo.getDirection(kind) == SyncInfo.OUTGOING && SyncInfo.getChange(kind) == SyncInfo.DELETION) {
							return false;
						}
						return super.isEditable();
					}
				};
		}
		return null;
	}
	
	/**
	 * Create an ITypedElement for the given remote resource. The contents for the remote resource
	 * will be retrieved from the given IStorage which is a local cache used to buffer the remote contents
	 */
	public static ITypedElement createTypeElement(IRemoteResource remoteResource) {
		return new RemoteResourceTypedElement(remoteResource);
	}
	
	/**
	 * Creates a new diff node.
	 */	
	public SyncInfoDiffNode(ITypedElement base, ITypedElement local, ITypedElement remote, int syncKind) {
		super(syncKind, base, local, remote);
	}

	/**
	 * Construct a SynchromizeViewNode
	 * @param input The SubscriberInput for the node.
	 * @param resource The resource for the node
	 */
	public SyncInfoDiffNode(SyncInfoSet input, IResource resource) {
		this(createBaseTypeElement(input, resource), createLocalTypeElement(input, resource), createRemoteTypeElement(input, resource), getSyncKind(input, resource));
		this.input = input;	
		this.resource = resource;
		this.info = null;
	}
	
	/**
	 * Construct a SynchromizeViewNode
	 * @param input The SubscriberInput for the node.
	 * @param resource The resource for the node
	 */
	public SyncInfoDiffNode(SyncInfo info) {
		this(createBaseTypeElement(info), createLocalTypeElement(info), createRemoteTypeElement(info), info.getKind());
		this.info = info;
		this.input = null;	
		this.resource = info.getLocal();
	}

	private static ITypedElement createRemoteTypeElement(SyncInfoSet set, IResource resource) {
		return createRemoteTypeElement(set.getSyncInfo(resource));
	}

	private static ITypedElement createLocalTypeElement(SyncInfoSet set, IResource resource) {
		return createLocalTypeElement(set.getSyncInfo(resource));
	}

	private static ITypedElement createBaseTypeElement(SyncInfoSet set, IResource resource) {
		return createBaseTypeElement(set.getSyncInfo(resource));
	}

	private static ITypedElement createRemoteTypeElement(SyncInfo info) {
		if(info != null && info.getRemote() != null) {
			return createTypeElement(info.getRemote());
		}
		return null;
	}

	private static ITypedElement createLocalTypeElement(SyncInfo info) {
		if(info != null && info.getLocal() != null) {
			return createTypeElement(info.getLocal(), info.getKind());
		}
		return null;
	}

	private static ITypedElement createBaseTypeElement(SyncInfo info) {
		if(info != null && info.getBase() != null) {
			return createTypeElement(info.getBase());
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == SyncInfo.class) {
			return getSyncInfo();
		} 
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.ISynchronizeViewNode#getSyncInfo()
	 */
	public SyncInfo getSyncInfo() {
		if(info != null) {
			return info;
		} else if(input != null && resource != null) {
			return input.getSyncInfo(resource);
		}
		return null;
	}
	
	protected static int getSyncKind(SyncInfoSet set, IResource resource) {
		SyncInfo info = set.getSyncInfo(resource);
		if(info != null) {
			return info.getKind();
		}
		return SyncInfo.IN_SYNC;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.ISynchronizeViewNode#getChildSyncInfos()
	 */
	public SyncInfo[] getChildSyncInfos() {
		if(input != null) {
			return input.getOutOfSyncDescendants(resource);
		} else if(info != null) {
			return new SyncInfo[] {info};
		}
		return new SyncInfo[0];
	}
	
	/**
	 * Return true if the receiver's TeamSubscriber and Resource are equal to that of object.
	 * @param object The object to test
	 * @return true has the same subsriber and resource
	 */
	public boolean equals(Object object) {
		if (object instanceof SyncInfoDiffNode) {
			SyncInfoDiffNode syncViewNode = (SyncInfoDiffNode) object;
			return getResource().equals(syncViewNode.getResource());
		}
		return super.equals(object);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getResource().hashCode();
	}

	/**
	 * @return IResource The receiver's resource
	 */
	public IResource getResource() {
		return resource;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "SynchronizeViewNode for " + getResource().getFullPath().toString(); //$NON-NLS-1$
	}
	
	/**
	 * Cache the contents for the base and remote.
	 * @param monitor
	 */
	public void cacheContents(IProgressMonitor monitor) throws TeamException {
		ITypedElement base = getAncestor();
		ITypedElement remote = getRight();
		int work = Math.min((remote== null ? 0 : 50) + (base == null ? 0 : 50), 10);
		monitor.beginTask(null, work);
		try {
			if (base != null && base instanceof RemoteResourceTypedElement) {
				((RemoteResourceTypedElement)base).cacheContents(Policy.subMonitorFor(monitor, 50));
			}
			if (remote != null && remote instanceof RemoteResourceTypedElement) {
				((RemoteResourceTypedElement)remote).cacheContents(Policy.subMonitorFor(monitor, 50));
			}
		} finally {
			monitor.done();
		}
	}
	
	public SyncInfoSet getSyncInfoSet() {
		return input;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.structuremergeviewer.IDiffContainer#hasChildren()
	 */
	public boolean hasChildren() {
		if(input != null) {		
			if(getResource().getType() == IResource.ROOT) return true;
			SyncInfo[] info = input.getOutOfSyncDescendants(getResource());
			if(info != null && info.length > 0)
				return true;
		}
		return false;
	}
}