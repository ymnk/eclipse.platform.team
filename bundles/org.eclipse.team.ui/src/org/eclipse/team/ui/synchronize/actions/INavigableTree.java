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
package org.eclipse.team.ui.synchronize.actions;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * This interface is used by the <code>SyncInfoDiffTreeNavigator</code>. Any
 * tree widget that is used as the diff viewer associated with a 
 * <code>SyncInfoSetCompareConfiguration</code> should implement this interface
 * in order to get menu, toolbar and keyboard navigation.
 */
public interface INavigableTree {
	
	/**
	 * Open the currently selected item. For a <code>TreeViewer</code>,
	 * this can be accomplished using the expression
	 * <code>fireOpen(new OpenEvent(this, getSelection()))</code>.
	 */
	void openSelection();
	
	/**
	 * Get the <code>Tree</code> that is the control for this viewer.
	 * This method is already defined on <code>TreeViewer</code>.
	 * @return a <code>Tree</code> control
	 */
	Tree getTree();
	
	/**
	 * Create the children of the given <code>TreeItem</code> if they have not
	 * already been created. 
	 * This method is already defined on <code>TreeViewer</code> but is protected
	 * so an implementor must override it to make it public.
	 * @param item the parent tree item
	 */
	void createChildren(TreeItem item);
	
	/**
	 * Get the currently selected item.
	 * This method is already defined on <code>TreeViewer</code>.
	 * @return the selection
	 */
	ISelection getSelection();
	
	/**
	 * Set the viewers selection to the given selection.
	 * This method is already defined on <code>TreeViewer</code>.
	 * @param selection the new selection
	 * @param fireOpen open selection as well
	 */
	void setSelection(ISelection selection, boolean fireOpen);
}