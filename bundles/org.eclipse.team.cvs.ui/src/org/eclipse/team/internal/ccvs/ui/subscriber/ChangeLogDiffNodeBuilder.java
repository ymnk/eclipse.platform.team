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

import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.core.subscribers.SyncInfoSet;
import org.eclipse.team.internal.ccvs.core.CVSSyncInfo;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.ui.synchronize.SyncInfoDiffNodeRoot;
import org.eclipse.team.ui.synchronize.views.SyncInfoDiffNodeBuilder;

public class ChangeLogDiffNodeBuilder extends SyncInfoDiffNodeBuilder {
	
	private Map commentRoots = new HashMap();
	
	public ChangeLogDiffNodeBuilder(SyncInfoDiffNodeRoot root) {
		super(root);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.views.SyncInfoDiffNodeBuilder#buildTree(org.eclipse.compare.structuremergeviewer.DiffNode)
	 */
	protected IDiffElement[] buildTree(DiffNode node) {
		if(node == getRoot()) {
			DiffNode[] nodes = calculateRoots(getRoot().getSyncInfoSet());
			for (int i = 0; i < nodes.length; i++) {
				super.buildTree(nodes[i]);				
			}
		} else {
			return super.buildTree(node);
		}
		return new IDiffElement[0];
	}

	private DiffNode[] calculateRoots(SyncInfoSet set) {
		SyncInfo[] infos = set.members();
		for (int i = 0; i < infos.length; i++) {
			String comment = getSyncInfoComment((CVSSyncInfo) infos[i]);
			ChangeLogDiffNode changeRoot = (ChangeLogDiffNode) commentRoots.get(comment);
			if (changeRoot == null) {
				changeRoot = new ChangeLogDiffNode(getRoot(), comment);
				commentRoots.put(comment, changeRoot);
			}
			changeRoot.add(infos[i]);
		}		
		return (ChangeLogDiffNode[]) commentRoots.values().toArray(new ChangeLogDiffNode[commentRoots.size()]);
	}
	
	private String getSyncInfoComment(CVSSyncInfo info) {
		try {
			ICVSRemoteResource remote = (ICVSRemoteResource)info.getRemote();
			return remote.getComment();
		} catch (TeamException e) {
			return e.getMessage();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.views.SyncInfoDiffNodeBuilder#syncSetChanged(org.eclipse.team.core.subscribers.ISyncInfoSetChangeEvent)
	 */
	protected void syncSetChanged(ISyncInfoSetChangeEvent event) {
	   commentRoots.clear();
       reset();
	}
}
