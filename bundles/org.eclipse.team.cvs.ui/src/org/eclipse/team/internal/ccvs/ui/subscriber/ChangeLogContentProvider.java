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
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.internal.ccvs.core.CVSSyncInfo;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.ui.synchronize.SyncInfoDiffNode;
import org.eclipse.team.ui.synchronize.views.SyncInfoSetTreeContentProvider;

/**
 * Seeing change comments makes sense it two ways.
 */
public class ChangeLogContentProvider extends SyncInfoSetTreeContentProvider {
	
	private Map commentRoots = new HashMap();
//	private SyncInfoSetContentProvider oldProvider;
	
	public ChangeLogContentProvider() {
//		this.oldProvider = provider;
	}
	
	public Object[] getChildren(Object element) {
		IResource resource = getResource(element);
		if(element instanceof SyncInfoDiffNode && resource != null && resource.getType() == IResource.ROOT){
			return calculateRoots(((SyncInfoDiffNode)element).getSyncInfoSet());
		}
		return super.getChildren(element);
	}

	private Object[] calculateRoots(SyncInfoSet set) {
		SyncInfo[] infos = set.members();
		commentRoots.clear();
		for (int i = 0; i < infos.length; i++) {
			String comment = getSyncInfoComment((CVSSyncInfo) infos[i]);
			ChangeLogDiffNode changeRoot = (ChangeLogDiffNode) commentRoots.get(comment);
			if (changeRoot == null) {
				changeRoot = new ChangeLogDiffNode(comment);
				commentRoots.put(comment, changeRoot);
			}
			changeRoot.addChild(infos[i]);
		}		
		return (ChangeLogDiffNode[]) commentRoots.values().toArray(new ChangeLogDiffNode[commentRoots.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.views.SyncSetContentProvider#handleResourceAdditions(org.eclipse.team.ui.synchronize.ISyncInfoSetChangeEvent)
	 */
	public void handleResourceAdditions(ISyncInfoSetChangeEvent event) {
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
	public void handleResourceRemovals(ISyncInfoSetChangeEvent event) {
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
