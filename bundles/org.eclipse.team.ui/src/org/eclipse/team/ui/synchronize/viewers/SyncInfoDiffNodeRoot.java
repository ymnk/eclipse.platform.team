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
package org.eclipse.team.ui.synchronize.viewers;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.team.core.subscribers.SyncInfoSet;

public class SyncInfoDiffNodeRoot extends SyncInfoDiffNode {

	SyncInfoDiffNodeBuilder builder;
	
	/**
	 * @param set
	 * @param resource
	 */
	public SyncInfoDiffNodeRoot(SyncInfoSet set) {
		super(null, set, ResourcesPlugin.getWorkspace().getRoot());
		builder = createBuilder();
		builder.buildTree();
	}

	/**
	 * @return
	 */
	protected SyncInfoDiffNodeBuilder createBuilder() {
		return new SyncInfoDiffNodeBuilder(this);
	}

	/**
	 * 
	 */
	public void dispose() {
		builder.dispose();
	}

	/**
	 * @param viewer
	 */
	public void setViewer(AbstractTreeViewer viewer) {
		builder.setViewer(viewer);
	}

	public SyncInfoDiffNodeSorter getSorter() {
		return new SyncInfoDiffNodeSorter();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.structuremergeviewer.DiffContainer#hasChildren()
	 */
	public boolean hasChildren() {
		// This is required to allow the sync framework to be used in wizards
		// where the input is not populated until after the compare input is created
		// (i.e. the compare input will only create the diff viewer if the input has children
		return true;
	}
}
