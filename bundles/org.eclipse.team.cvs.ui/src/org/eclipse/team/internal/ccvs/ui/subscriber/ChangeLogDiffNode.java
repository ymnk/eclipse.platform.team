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

import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.core.subscribers.MutableSyncInfoSet;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.ui.synchronize.SyncInfoDiffNode;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class ChangeLogDiffNode extends DiffNode implements IAdaptable, IWorkbenchAdapter {

	private String comment;
	private MutableSyncInfoSet set;
	private SyncInfoDiffNode node;

	public ChangeLogDiffNode(String comment) {
		super(SyncInfo.IN_SYNC);
		this.comment = comment;
		set = new MutableSyncInfoSet();
		node = new SyncInfoDiffNode(set, ResourcesPlugin.getWorkspace().getRoot());
	}

	/**
	 * @return Returns the comment.
	 */
	public String getComment() {
		return comment;
	}
	
	public boolean equals(Object other) {
		if(other == this) return true;
		if(! (other instanceof ChangeLogDiffNode)) return false;
		return ((ChangeLogDiffNode)other).getComment().equals(getComment());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object o) {
		return node.getChildren(o);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getImageDescriptor(java.lang.Object)
	 */
	public ImageDescriptor getImageDescriptor(Object object) {
		return CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_PROJECT_VERSION);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
	 */
	public String getLabel(Object o) {
		return comment;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
	 */
	public Object getParent(Object o) {
		return getParent();
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
	
	public void addChild(SyncInfo info) {
		set.add(info);
	}
}
