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
import org.eclipse.compare.ResourceNode;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.synchronize.compare.LocalResourceTypedElement;
import org.eclipse.team.internal.ui.synchronize.compare.RemoteResourceTypedElement;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class SyncInfoDiffNode extends DiffNode implements IAdaptable, IWorkbenchAdapter {
	
	private SyncInfoSet syncSet;
	private SyncInfo    info;
		
	/**
	 * Create an ITypedElement for the given local resource. The returned ITypedElement
	 * will prevent editing of outgoing deletions.
	 */
	private static ITypedElement createTypeElement(IResource resource, final int kind) {
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
	private static ITypedElement createTypeElement(ISubscriberResource remoteResource) {
		return new RemoteResourceTypedElement(remoteResource);
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
	
	private static int getSyncKind(SyncInfoSet set, IResource resource) {
		SyncInfo info = set.getSyncInfo(resource);
		if(info != null) {
			return info.getKind();
		}
		return SyncInfo.IN_SYNC;
	}
	
	/**
	 * Creates a new diff node.
	 */	
	private SyncInfoDiffNode(IDiffContainer parent, ITypedElement base, ITypedElement local, ITypedElement remote, int syncKind) {
		super(parent, syncKind, base, local, remote);
	}
	
	/**
	 * Construct a <code>SyncInfoDiffNode</code> for a resource for use in a diff tree viewer.
	 * @param set The set associated with the diff tree veiwer
	 * @param resource The resource for the node
	 */
	public SyncInfoDiffNode(IDiffContainer parent, SyncInfoSet set, IResource resource) {
		this(parent, createBaseTypeElement(set, resource), createLocalTypeElement(set, resource), createRemoteTypeElement(set, resource), getSyncKind(set, resource));
		this.syncSet = set;
	}
	
	/**
	 * Construct a <code>SyncInfoDiffNode</code> for use in a compare input that does not 
	 * make use of a diff tree viewer.
	 * TODO: Create subclass for SyncInfoCompareInput
	 * @param info The <code>SyncInfo</code> for a resource
	 */
	public SyncInfoDiffNode(SyncInfo info) {
		this(null, createBaseTypeElement(info), createLocalTypeElement(info), createRemoteTypeElement(info), info.getKind());
		this.info = info;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if(adapter == IWorkbenchAdapter.class) {
			return this;
		}
		return null;
	}
	
	/**
	 * Return the <code>SyncInfoSet</code> from which this diff node was derived.
	 * @return a <code>SyncInfoSet</code>
	 */
	public SyncInfoSet getSyncInfoSet() {
		return syncSet;
	}
	
	/**
	 * Helper method that returns the resource associated with this node. A node is not
	 * required to have an associated local resource.
	 * @return the resource associated with this node or <code>null</code> if the local
	 * contributor is not a resource.
	 */
	public IResource getResource() {
		ITypedElement element = getLeft();
		if(element instanceof ResourceNode) {
			return ((ResourceNode)element).getResource();
		}
		return null;
	}
	
	/**
	 * Return the <code>SyncInfo</code> for all visible out-of-sync resources
	 * that are descendants of this node in the diff viewer.
	 */
	public SyncInfo[] getDescendantSyncInfos() {
		SyncInfoSet input = getSyncInfoSet();
		IResource resource = getResource();
		SyncInfo info = getSyncInfo();
		if(input != null && resource != null) {
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
		return this==object;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		IResource resource = getResource();
		if (resource == null) {
			return super.hashCode();
		}
		return resource.hashCode();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getResource() != null ? getResource().getFullPath().toString() : getName();
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
	
	/**
	 * Return whether this diff node has descendant conflicts in the view in which it appears.
	 * @return whether the node has descendant conflicts
	 */
	public boolean hasDecendantConflicts() {
		IResource resource = getResource();
		// If this node has no resource, we can't tell
		// The subclass which created the node with no resource should have overridden this method
		if (resource != null && resource.getType() == IResource.FILE) return false;
		// If the set has no conflicts then the node doesn't either
		if (getSyncInfoSet().countFor(SyncInfo.CONFLICTING, SyncInfo.DIRECTION_MASK) == 0) {
			return false;
		}
		SyncInfo[] infos = getDescendantSyncInfos();
		for (int i = 0; i < infos.length; i++) {
			SyncInfo info = infos[i];
			if ((info.getKind() & SyncInfo.DIRECTION_MASK) == SyncInfo.CONFLICTING) {
				return true;
			}
		}
		return false;
	}

	protected SyncInfo getSyncInfo() {
		return info;
	}
		
	/** WorkbenchAdapter methods **/
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object o) {
		return getChildren();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getImageDescriptor(java.lang.Object)
	 */
	public ImageDescriptor getImageDescriptor(Object object) {
		IResource resource = getResource();
		if (resource == null) {
			return null;
		}
		IWorkbenchAdapter adapter = (IWorkbenchAdapter)((IAdaptable) resource).getAdapter(IWorkbenchAdapter.class);
		return adapter.getImageDescriptor(resource);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
	 */
	public String getLabel(Object o) {
		return getName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
	 */
	public Object getParent(Object o) {
		return getParent();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.structuremergeviewer.DiffNode#getName()
	 */
	public String getName() {
		IResource resource = getResource();
		if (resource != null) {
			return resource.getName();
		}
		return super.getName();
	}
}