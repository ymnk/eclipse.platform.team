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
package org.eclipse.team.internal.ui.synchronize.views;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

public class TreeViewerUtils {
	
	/**
	 * Selects the next (or previous) node of the current selection.
	 * If there is no current selection the first (last) node in the tree is selected.
	 * Wraps around at end or beginning.
	 * Clients may override. 
	 *
	 * @param next if <code>true</code> the next node is selected, otherwise the previous node
	 */
	public static boolean gotoDifference(TreeViewer viewer, boolean next, boolean fireOpen) {	
		return navigate(viewer, next, fireOpen);
	}
	
	/**
	 * Selects the next (or previous) node of the current selection.
	 * If there is no current selection the first (last) node in the tree is selected.
	 * Wraps around at end or beginning.
	 * Clients may override. 
	 *
	 * @param next if <code>true</code> the next node is selected, otherwise the previous node
	 * @return <code>true</code> if at end (or beginning)
	 */
	public static boolean navigate(TreeViewer viewer, boolean next, boolean fireOpen) {
		
		Tree tree = viewer.getTree();
		if (tree == null) return false;

		TreeItem item= null;
		TreeItem children[]= tree.getSelection();
		if (children != null && children.length > 0)
			item= children[0];
		if (item == null) {
			children= tree.getItems();
			if (children != null && children.length > 0) {
				item= children[0];
				if (item != null && item.getItemCount() <= 0) {
					setSelection(viewer, item, fireOpen);	// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=20106
					return false;
				}
			}
		}
		
		while (true) {
			item= findNextPrev(viewer, item, next);
			if (item == null)
				break;
			if (item.getItemCount() <= 0)
				break;
		}
		
		if (item != null) {
			setSelection(viewer, item, fireOpen);	// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=20106
			return false;
		}
		return true;
	}

	private static void setSelection(TreeViewer viewer, TreeItem ti, boolean fireOpen) {
		if (ti != null) {
			Object data= ti.getData();
			if (data != null) {
				// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=20106
				ISelection selection= new StructuredSelection(data);
				viewer.setSelection(selection, true);
				ISelection currentSelection= viewer.getSelection();
				if (fireOpen && currentSelection != null && selection.equals(currentSelection)) {
					if(viewer instanceof ITreeViewerAccessor) {
						((ITreeViewerAccessor)viewer).openSelection();
					}
				}
			}
		}
	}
	
	private static TreeItem findNextPrev(TreeViewer viewer, TreeItem item, boolean next) {
		
		if (item == null || !(viewer instanceof ITreeViewerAccessor))
			return null;
		
		TreeItem children[]= null;
		ITreeViewerAccessor treeAccessor = (ITreeViewerAccessor)viewer;

		if (!next) {
			
			TreeItem parent= item.getParentItem();
			if (parent != null)
				children= parent.getItems();
			else
				children= item.getParent().getItems();
			
			if (children != null && children.length > 0) {
				// goto previous child
				int index= 0;
				for (; index < children.length; index++)
					if (children[index] == item)
						break;
					
				if (index > 0) {
					
					item= children[index-1];
					
					while (true) {
						treeAccessor.createChildren(item);
						int n= item.getItemCount();
						if (n <= 0)
							break;
						
						item.setExpanded(true);
						item= item.getItems()[n-1];
					}

					// previous
					return item;
				}
			}
			
			// go up
			return parent;
			
		} else {
			item.setExpanded(true);
			treeAccessor.createChildren(item);
			
			if (item.getItemCount() > 0) {
				// has children: go down
				children= item.getItems();
				return children[0];
			}
			
			while (item != null) {
				children= null;
				TreeItem parent= item.getParentItem();
				if (parent != null)
					children= parent.getItems();
				else
					children= item.getParent().getItems();
				
				if (children != null && children.length > 0) {
					// goto next child
					int index= 0;
					for (; index < children.length; index++)
						if (children[index] == item)
							break;
						
					if (index < children.length-1) {
						// next
						return children[index+1];
					}
				}
				
				// go up
				item= parent;
			}
		}
		
		return item;
	}
	
}
