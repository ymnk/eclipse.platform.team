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
package org.eclipse.team.internal.ccvs.ui.subscriber;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.ccvs.core.CVSSyncInfo;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ui.synchronize.views.SyncSetTreeContentProvider;
import org.eclipse.team.ui.synchronize.*;

/**
 * Seeing change comments makes sense it two ways.
 */
public class ChangeLogContentProvider extends SyncSetTreeContentProvider {
	
	private Map commentRoots = new HashMap();
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object element) {
		IResource resource = getResource(element);
		if(element instanceof SyncInfoSet){
			// initialize case
			return calculateRoots();
		} else if(element instanceof ChangeLogDiffNode) {
			return ((ChangeLogDiffNode)element).getChildren();
		}
		return new Object[0];
	}

	/**
	 * @return
	 */
	private Object[] calculateRoots() {
		SyncInfoSet set = getSyncSet();
		SyncInfo[] infos = set.members();
		commentRoots.clear();
		for (int i = 0; i < infos.length; i++) {
			String comment = getSyncInfoComment((CVSSyncInfo) infos[i]);
			ChangeLogDiffNode changeRoot = (ChangeLogDiffNode) commentRoots.get(comment);
			if (changeRoot == null) {
				changeRoot = new ChangeLogDiffNode(comment);
				commentRoots.put(comment, changeRoot);
			}
			changeRoot.add(new SyncInfoDiffNode(infos[i]));
		}
		return (ChangeLogDiffNode[]) commentRoots.values().toArray(new ChangeLogDiffNode[commentRoots.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.views.SyncSetTreeContentProvider#getModelParent(org.eclipse.core.resources.IResource)
	 */
	protected Object getModelParent(IResource resource) {
		// TODO Auto-generated method stub
		return super.getModelParent(resource);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.views.SyncSetContentProvider#getParent(java.lang.Object)
	 */
	public Object getParent(Object element) {
		return super.getParent(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	public boolean hasChildren(Object element) {
		// TODO Auto-generated method stub
		return super.hasChildren(element);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.views.SyncSetContentProvider#handleResourceAdditions(org.eclipse.team.ui.synchronize.ISyncInfoSetChangeEvent)
	 */
	protected void handleResourceAdditions(ISyncInfoSetChangeEvent event) {
		AbstractTreeViewer tree = getTreeViewer();
		if (tree != null) {
			SyncInfo[] added = event.getAddedResources();
			// TODO: Should group added roots by their parent
			for (int i = 0; i < added.length; i++) {
				SyncInfo info = added[i];
				ChangeLogDiffNode parent = (ChangeLogDiffNode)commentRoots.get(info);
				if(parent != null) {
					SyncInfoDiffNode node = new SyncInfoDiffNode(info);
					parent.add(node);
					tree.add(parent, node);
				}
			}
		} else {
			super.handleResourceAdditions(event);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.views.SyncSetContentProvider#handleResourceRemovals(org.eclipse.team.ui.synchronize.ISyncInfoSetChangeEvent)
	 */
	protected void handleResourceRemovals(ISyncInfoSetChangeEvent event) {
		AbstractTreeViewer tree = getTreeViewer();
		if (tree != null) {
			IResource[] roots = event.getRemovedResources();
			if (roots.length == 0) return;
			Object[] modelRoots = new Object[roots.length];
			for (int i = 0; i < modelRoots.length; i++) {
				modelRoots[i] = getModelObject(roots[i]);
			}
			tree.remove(modelRoots);
		} else {
			super.handleResourceRemovals(event);
		}
	}
	
	private String getSyncInfoComment(CVSSyncInfo info) {
		try {
			ICVSRemoteResource remote = (ICVSRemoteResource)info.getRemote();
			return remote.getComment();
		} catch (TeamException e) {
			return e.getMessage();
		}
	}
}
