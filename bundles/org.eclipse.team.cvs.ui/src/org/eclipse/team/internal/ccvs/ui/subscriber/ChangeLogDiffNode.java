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
import org.eclipse.team.internal.ccvs.core.ILogEntry;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.ui.synchronize.SyncInfoDiffNode;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class ChangeLogDiffNode extends SyncInfoDiffNode implements IAdaptable, IWorkbenchAdapter {

	private ILogEntry logEntry;

	public ChangeLogDiffNode(DiffNode parent, ILogEntry logEntry) {
		super(parent, new MutableSyncInfoSet(), ResourcesPlugin.getWorkspace().getRoot());
		this.logEntry = logEntry;
	}

	public ILogEntry getComment() {
		return logEntry;
	}
	
	public boolean equals(Object other) {
		if(other == this) return true;
		if(! (other instanceof ChangeLogDiffNode)) return false;
		return ((ChangeLogDiffNode)other).getComment().equals(getComment());
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
		return logEntry.getComment() + "(" + logEntry.getAuthor() +")";
	}

	public void add(SyncInfo info) {
		((MutableSyncInfoSet)getSyncInfoSet()).add(info);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncInfoDiffNode#toString()
	 */
	public String toString() {
		return getLabel(null);
	}
}
