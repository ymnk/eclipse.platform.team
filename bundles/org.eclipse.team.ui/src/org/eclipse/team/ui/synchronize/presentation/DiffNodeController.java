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
package org.eclipse.team.ui.synchronize.presentation;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;

/**
 * This class is reponsible for creating and maintaining model of
 * DiffNodes that can be shown in a viewer.
 * 
 * @since 3.0
 */
public abstract class DiffNodeController {

	/**
	 * Called to initialize this controller and returns the input created by this controller. 
	 * @param monitor
	 * @return
	 */
	public abstract AdaptableDiffNode prepareInput(IProgressMonitor monitor);
	
	/**
	 * Returns the input created by this controller or <code>null</code> if 
	 * {@link #prepareInput(IProgressMonitor)} hasn't been called on this object yet.
	 * @return
	 */
	public abstract AdaptableDiffNode getInput();
	
	public abstract void setViewer(AbstractTreeViewer viewer);

	public abstract ViewerSorter getViewerSorter();

	public abstract void dispose();

}