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
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.ccvs.core.CVSSyncInfo;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ui.synchronize.views.SyncSetTreeContentProvider;
import org.eclipse.team.ui.synchronize.SyncInfoDiffNode;
import org.eclipse.team.ui.synchronize.SyncInfoSet;

public class ChangeLogContentProvider extends SyncSetTreeContentProvider {
	
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
		Map commentRoots = new HashMap();
		for (int i = 0; i < infos.length; i++) {
			CVSSyncInfo info = (CVSSyncInfo)infos[i];
			ICVSRemoteResource remote = (ICVSRemoteResource)info.getRemote();
			try {
				String comment = remote.getComment();
				ChangeLogDiffNode changeRoot = (ChangeLogDiffNode)commentRoots.get(comment);
				if(changeRoot == null) {
					changeRoot = new ChangeLogDiffNode(comment);
					commentRoots.put(comment,changeRoot);
				}
				changeRoot.add(new SyncInfoDiffNode(info));
			} catch (TeamException e) {
			}
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
		// TODO Auto-generated method stub
		return super.getParent(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	public boolean hasChildren(Object element) {
		// TODO Auto-generated method stub
		return super.hasChildren(element);
	}
}
