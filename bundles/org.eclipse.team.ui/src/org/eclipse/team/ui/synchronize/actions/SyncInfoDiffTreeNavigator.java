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

import java.util.Iterator;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.NavigationAction;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.SyncInfoDiffNode;

/**
 * This class provides the tree navigation support for tree based diff viewers
 * created using a <code>SyncInfoSetCompareConfiguration</code>. This includes
 * menu actions for expanding the tree and opening the selected item in the
 * text compare pane as well as up and down navigation actions that are added
 * to the toolbas and bound to the navigation keys.
 */
public class SyncInfoDiffTreeNavigator {
	
	/**
	 * Direction to navigate
	 */
	final public static int NEXT = 1;
	final public static int PREVIOUS = 2;
	
	private Action expandAll;
	private Action open;
	private NavigationAction nextAction;
	private NavigationAction previousAction;
	
	INavigableTree target;
	boolean showOpenAction = true;
	
	public SyncInfoDiffTreeNavigator(INavigableTree target) {
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
		boolean next = direction == SyncInfoDiffTreeNavigator.NEXT ? true : false;
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
					setSelection(item, fireOpen);	// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=20106
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
			setSelection(item, fireOpen);	// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=20106
			return false;
		}
		return true;
	}

	private void setSelection(TreeItem ti, boolean fireOpen) {
		if (ti != null) {
			Object data= ti.getData();
			if (data != null) {
				// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=20106
				ISelection selection= new StructuredSelection(data);
				getTarget().setSelection(selection, true);
				ISelection currentSelection= getTarget().getSelection();
				if (fireOpen && currentSelection != null && selection.equals(currentSelection)) {
					getTarget().openSelection();
				}
			}
		}
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
	
	public INavigableTree getTarget() {
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
			// TODO: needs to be invoked when the selection changes
			public void update() {
				ISelection selection = target.getSelection();
				if (selection instanceof IStructuredSelection) {
					IStructuredSelection ss = (IStructuredSelection)selection;
					setEnabled(!ss.isEmpty());
				}
				setEnabled(false);
			}
		};
		Utils.initAction(expandAll, "action.expandAll."); //$NON-NLS-1$
		
		open = new Action() {
			public void run() {
				target.openSelection();
			}
			// TODO: needs to be invoked when the selection changes
			public void update() {
				ISelection selection = target.getSelection();
				if (selection instanceof IStructuredSelection) {
					IStructuredSelection ss = (IStructuredSelection)selection;
					if (ss.size() == 1) {
						Object element = ss.getFirstElement();
						if (element instanceof SyncInfoDiffNode) {
							IResource resource = ((SyncInfoDiffNode)element).getResource();
							setEnabled(resource != null && resource.getType() == IResource.FILE);
							return;
						}
					}
				}
				setEnabled(false);
			}
		};
		Utils.initAction(open, "action.open."); //$NON-NLS-1$
	}
	
	public void fillContextMenu(StructuredViewer viewer, IMenuManager manager) {
		AbstractTreeViewer tree = getAbstractTreeViewer(viewer);
		if (isShowOpenAction()) {
			manager.add(open);
		}
		if (tree != null) {
			if (isShowOpenAction()) {
				manager.add(new Separator());
			}
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
	
	/**
	 * @return Returns the showOpenAction.
	 */
	public boolean isShowOpenAction() {
		return showOpenAction;
	}
	
	/**
	 * @param showOpenAction The showOpenAction to set.
	 */
	public void setShowOpenAction(boolean showOpenAction) {
		this.showOpenAction = showOpenAction;
	}
}
