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
package org.eclipse.team.internal.ui.synchronize;

import java.util.Iterator;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.NavigationAction;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.actions.INavigableControl;

/**
 * This class provides the navigation support required for keybindings
 */
public class SyncInfoDiffTreeNavigator {
	
	private Action expandAll;
	private NavigationAction nextAction;
	private NavigationAction previousAction;
	
	public interface INavigationTarget {
		void setSelection(TreeItem ti, boolean fireOpen);
		Tree getTree();
		void createChildren(TreeItem item);
	}
	
	INavigationTarget target;
	
	public SyncInfoDiffTreeNavigator(INavigationTarget target) {
		this.target = target;
	}

	/**
	 * Selects the next (or previous) node of the current selection.
	 * If there is no current selection the first (last) node in the tree is selected.
	 * Wraps around at end or beginning.
	 * Clients may override. 
	 *
	 * @param next if <code>true</code> the next node is selected, otherwise the previous node
	 */
	public boolean gotoDifference(int direction) {	
		boolean next = direction == INavigableControl.NEXT ? true : false;
		return navigate(next, false);
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
	public boolean navigate(boolean next, boolean fireOpen) {
		
		Tree tree = getTarget().getTree();
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
					getTarget().setSelection(item, fireOpen);	// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=20106
					return false;
				}
			}
		}
		
		while (true) {
			item= findNextPrev(item, next);
			if (item == null)
				break;
			if (item.getItemCount() <= 0)
				break;
		}
		
		if (item != null) {
			getTarget().setSelection(item, fireOpen);	// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=20106
			return false;
		}
		return true;
	}

	private TreeItem findNextPrev(TreeItem item, boolean next) {
		
		if (item == null)
			return null;
		
		TreeItem children[]= null;

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
						getTarget().createChildren(item);
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
			getTarget().createChildren(item);
			
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
	
	public INavigationTarget getTarget() {
		return target;
	}
	
	public void createToolItems(IToolBarManager tbm) {
		nextAction= new NavigationAction(true);
		tbm.appendToGroup("navigation", nextAction); //$NON-NLS-1$

		previousAction= new NavigationAction(false);
		tbm.appendToGroup("navigation", previousAction); //$NON-NLS-1$		
	}
	
	public void updateCompareEditorInput(CompareEditorInput input) {
		nextAction.setCompareEditorInput(input);
		previousAction.setCompareEditorInput(input);
	}
	
	public void createActions(final StructuredViewer viewer) {
		expandAll = new Action() {
			public void run() {
				expandAllFromSelection(viewer);
			}
		};
		Utils.initAction(expandAll, "action.expandAll."); //$NON-NLS-1$
	}
	
	public void fillContextMenu(StructuredViewer viewer, IMenuManager manager) {
		AbstractTreeViewer tree = getAbstractTreeViewer(viewer);
		if (tree != null) {
			manager.add(expandAll);
		}
	}
	
	protected void expandAllFromSelection(StructuredViewer viewer) {
		AbstractTreeViewer tree = getAbstractTreeViewer(viewer);
		if (tree == null) return;
		ISelection selection = tree.getSelection();
		if(! selection.isEmpty()) {
			Iterator elements = ((IStructuredSelection)selection).iterator();
			while (elements.hasNext()) {
				Object next = elements.next();
				tree.expandToLevel(next, AbstractTreeViewer.ALL_LEVELS);
			}
		}
	}
	
	private AbstractTreeViewer getAbstractTreeViewer(StructuredViewer viewer) {
		if (viewer instanceof AbstractTreeViewer) {
			return (AbstractTreeViewer)viewer;
		}
		return null;
	}
	
	public void reveal(StructuredViewer viewer, Object element) {
		AbstractTreeViewer tree = getAbstractTreeViewer(viewer);
		if (tree == null)return;
		// Double-clicking should expand/collapse containers
		if(tree.isExpandable(element)) {
			tree.setExpandedState(element, ! tree.getExpandedState(element));
		}
	}
}
