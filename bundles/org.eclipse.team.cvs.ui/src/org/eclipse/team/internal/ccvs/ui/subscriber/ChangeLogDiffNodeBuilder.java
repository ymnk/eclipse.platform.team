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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFile;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.ui.synchronize.SyncInfoDiffNodeRoot;
import org.eclipse.team.ui.synchronize.views.SyncInfoDiffNodeBuilder;

/**
 * It would be very useful to support showing changes grouped logically
 * instead of grouped physically. This could be used for showing incoming
 * changes and also for showing the results of comparisons.
 * 
 * Some problems with this:
 * 1. you have to fetch the log entries to extract useful information by
 * which to classify changes. this is expensive and can't be done in the
 * ui thread.
 * 
 * 2. how to support logical groupins based on any of the information in
 * the log entry?
 */
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
		commentRoots.clear();
		SyncInfo[] infos = set.members();
		for (int i = 0; i < infos.length; i++) {
			ILogEntry logEntry = getSyncInfoComment((CVSSyncInfo) infos[i]);
			if(logEntry != null) {
				String comment = logEntry.getComment();
				ChangeLogDiffNode changeRoot = (ChangeLogDiffNode) commentRoots.get(comment);
				if (changeRoot == null) {
					changeRoot = new ChangeLogDiffNode(getRoot(), logEntry);
					commentRoots.put(comment, changeRoot);
				}
				changeRoot.add(infos[i]);
			}
		}		
		return (ChangeLogDiffNode[]) commentRoots.values().toArray(new ChangeLogDiffNode[commentRoots.size()]);
	}
	
	private ILogEntry getSyncInfoComment(CVSSyncInfo info) {
		try {
			ICVSRemoteResource remote = (ICVSRemoteResource)info.getRemote();
			if(remote instanceof RemoteFile) {
				return ((RemoteFile)remote).getLogEntry(new NullProgressMonitor());
			}
			return null;
		} catch (CVSException e) {
			CVSUIPlugin.log(e);
			return null;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.views.SyncInfoDiffNodeBuilder#syncSetChanged(org.eclipse.team.core.subscribers.ISyncInfoSetChangeEvent)
	 */
	protected void syncSetChanged(ISyncInfoSetChangeEvent event) {
		reset();
	}
}
