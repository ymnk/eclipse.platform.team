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

import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;

public interface DiffNodeController {

	public abstract void setViewer(AbstractTreeViewer viewer);

	public abstract ViewerSorter getViewerSorter();

	/**
	 * Called to initialize this controller and returns the input created by this controller. 
	 * @param monitor
	 * @return
	 */
	public abstract DiffNode prepareInput(IProgressMonitor monitor);

	public abstract void dispose();
	
	/**
	 * Returns the input created by this controller or <code>null</code> if 
	 * {@link #prepareInput(IProgressMonitor)} hasn't been called on this object yet.
	 * @return
	 */
	public DiffNode getInput();
}