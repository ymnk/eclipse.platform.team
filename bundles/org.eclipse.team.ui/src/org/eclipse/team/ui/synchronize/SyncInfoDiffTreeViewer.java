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
package org.eclipse.team.ui.synchronize;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.compare.*;
import org.eclipse.compare.internal.INavigatable;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.synchronize.views.*;
import org.eclipse.team.ui.synchronize.actions.INavigableControl;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.internal.PluginAction;
import org.eclipse.ui.views.navigator.ResourceSorter;

public class SyncInfoDiffTreeViewer extends TreeViewer implements INavigableControl {

	private TeamSubscriberParticipant participant;
	private ISyncInfoSet set;
	private Action expandAll;
	private NavigationAction nextAction;
	private NavigationAction previousAction;
	private boolean acceptParticipantMenuContributions = false;
	private MenuManager menuMgr = null; 
		
	/**
	 * Change the tree layout between using compressed folders and regular folders
	 * when the user setting is changed.
	 */
	private IPropertyChangeListener propertyListener = new IPropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent event) {
			if (event.getProperty().equals(IPreferenceIds.SYNCVIEW_COMPRESS_FOLDERS)) {
				setTreeViewerContentProvider();
			}
		}
	};	
	
	public SyncInfoDiffTreeViewer(Composite parent, TeamSubscriberParticipant participant, ISyncInfoSet set) {
		super(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		this.participant = participant;
		this.set = set;
		GridData data = new GridData(GridData.FILL_BOTH);
		setSorter(new SyncViewerSorter(ResourceSorter.NAME));
		setLabelProvider(new TeamSubscriberParticipantLabelProvider());
		getTree().setLayoutData(data);
		getStore().addPropertyChangeListener(propertyListener);
		setTreeViewerContentProvider();
		
		addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				handleDoubleClick(event);
			}
		});
		
		getTree().setData(CompareUI.COMPARE_VIEWER_TITLE, getTitle());
		
		INavigatable nav= new INavigatable() {
			public boolean gotoDifference(boolean next) {
				// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=20106
				return internalNavigate(next, true);
			}
		};
		getTree().setData(INavigatable.NAVIGATOR_PROPERTY, nav);
		
		createActions();
		hookContextMenu();
		
		createToolBarActions(parent);			
		setInput(getSyncSet());
	}

	protected Object getTitle() {
		return "Synchronization Changes";
	}

	public void setAcceptParticipantMenuContributions(boolean accept) {
		this.acceptParticipantMenuContributions = accept;
		if(acceptParticipantMenuContributions) {
			IWorkbenchPartSite site = Utils.findSite(getControl());
			if(site == null) {
				site = Utils.findSite();
			}
			if(site != null) {
				site.registerContextMenu(participant.getId(), menuMgr, this);
			}
		}
	}
	
	private void createToolBarActions(Composite parent) {
		ToolBarManager tbm= CompareViewerPane.getToolBarManager(parent);
		if (tbm != null) {
			tbm.removeAll();
			
			tbm.add(new Separator("navigation")); //$NON-NLS-1$
			
			createToolItems(tbm);
			tbm.update(true);
		}
	}

	protected void createToolItems(IToolBarManager tbm) {
		nextAction= new NavigationAction(true);
		tbm.appendToGroup("navigation", nextAction); //$NON-NLS-1$

		previousAction= new NavigationAction(false);
		tbm.appendToGroup("navigation", previousAction); //$NON-NLS-1$		
	}
	
	protected ISyncInfoSet getSyncSet() {
		return set;
	}

	private void createActions() {
		expandAll = new Action() {
			public void run() {
				expandAllFromSelection();
			}
		};
		Utils.initAction(expandAll, "action.expandAll."); //$NON-NLS-1$
	}
	
	protected void expandAllFromSelection() {
		ISelection selection = getSelection();
		if(! selection.isEmpty()) {
			Iterator elements = ((IStructuredSelection)selection).iterator();
			while (elements.hasNext()) {
				Object next = elements.next();
				expandToLevel(next, AbstractTreeViewer.ALL_LEVELS);
			}
		}
	}
	
	protected void fillContextMenu(IMenuManager manager) {	
		manager.add(expandAll);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	private void setTreeViewerContentProvider() {
		if (getStore().getBoolean(IPreferenceIds.SYNCVIEW_COMPRESS_FOLDERS)) {
			setContentProvider(new CompressedFolderContentProvider());
		} else {
			setContentProvider(new SyncSetTreeContentProvider());
		}
	}
	
	/**
	 * Return the preference store for this plugin.
	 * @return IPreferenceStore for this plugin
	 */
	private IPreferenceStore getStore() {
		return TeamUIPlugin.getPlugin().getPreferenceStore();
	}

	protected void handleLabelProviderChanged(LabelProviderChangedEvent event) {
		Object[] changed= event.getElements();
		if (changed != null && getInput() != null) {
			ArrayList others= new ArrayList();
			for (int i= 0; i < changed.length; i++) {
				Object curr = changed[i];
				if (curr instanceof IResource) {
					IContentProvider provider = getContentProvider();
					if (provider != null && provider instanceof SyncSetContentProvider) {
						curr = ((SyncSetContentProvider)provider).getModelObject((IResource)curr);
					}
				}
				others.add(curr);
			}
			if (others.isEmpty()) {
				return;
			}
			event= new LabelProviderChangedEvent((IBaseLabelProvider) event.getSource(), others.toArray());
		}
		super.handleLabelProviderChanged(event);
	}

	/**
	 * Cleanup listeners and call super for content provider and label provider disposal.
	 */	
	protected void handleDispose(DisposeEvent event) {
		super.handleDispose(event);
		getStore().removePropertyChangeListener(propertyListener);
	}
	
	/**
	 * Handles a double-click event from the viewer.
	 * Expands or collapses a folder when double-clicked.
	 * 
	 * @param event the double-click event
	 */
	protected void handleDoubleClick(DoubleClickEvent event) {
		IStructuredSelection selection = (IStructuredSelection) event.getSelection();
		Object element = selection.getFirstElement();	
		// Double-clicking should expand/collapse containers
		if(isExpandable(element)) {
			setExpandedState(element, ! getExpandedState(element));
		}
	}
	
	protected void hookContextMenu() {
		menuMgr = new MenuManager(participant.getId()); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(getControl());
		menu.addMenuListener(new MenuListener() {
			public void menuHidden(MenuEvent e) {
			}
			// Hack to allow action contributions to update their
			// state before the menu is shown. This is required when
			// the state of the selection changes and the contributions
			// need to update enablement based on this. 
			public void menuShown(MenuEvent e) {
				IContributionItem[] items = menuMgr.getItems();
				for (int i = 0; i < items.length; i++) {
					IContributionItem item = items[i];
					if(item instanceof ActionContributionItem) {
						IAction actionItem = ((ActionContributionItem)item).getAction();
						if(actionItem instanceof PluginAction) {
							((PluginAction)actionItem).selectionChanged(getSelection());
						}
					}
				}
			}
		});
		getControl().setMenu(menu);
		
		if(acceptParticipantMenuContributions) {
			IWorkbenchPartSite site = Utils.findSite(getControl());
			if(site == null) {
				site = Utils.findSite();
			}
			if(site != null) {
				site.registerContextMenu(participant.getId(), menuMgr, this);
			}
		}
	}
	
	protected void inputChanged(Object in, Object oldInput) {
		super.inputChanged(in, oldInput);		
		if (in != oldInput) {
			initialSelection();
		}
	}
	
	/**
	 * This hook method is called from within <code>inputChanged</code>
	 * after a new input has been set but before any controls are updated.
	 * This default implementation calls <code>navigate(true)</code>
	 * to select and expand the first leaf node.
	 * Clients can override this method and are free to decide whether
	 * they want to call the inherited method.
	 * 
	 * @since 2.0
	 */
	protected void initialSelection() {
		internalNavigate(false, true);
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
		return internalNavigate(next, false);
	}
	
	public void updateCompareEditorInput(CompareEditorInput input) {
		nextAction.setCompareEditorInput(input);
		previousAction.setCompareEditorInput(input);
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
	private boolean internalNavigate(boolean next, boolean fireOpen) {
		
		Control c= getControl();
		if (!(c instanceof Tree))
			return false;
		
		Tree tree= (Tree) c;
		TreeItem item= null;
		TreeItem children[]= tree.getSelection();
		if (children != null && children.length > 0)
			item= children[0];
		if (item == null) {
			children= tree.getItems();
			if (children != null && children.length > 0) {
				item= children[0];
				if (item != null && item.getItemCount() <= 0) {
					internalSetSelection(item, fireOpen);				// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=20106
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
			internalSetSelection(item, fireOpen);	// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=20106
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
						createChildren(item);
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
			createChildren(item);
			
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
	
	private void internalSetSelection(TreeItem ti, boolean fireOpen) {
		if (ti != null) {
			Object data= ti.getData();
			if (data != null) {
				// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=20106
				ISelection selection= new StructuredSelection(data);
				setSelection(selection, true);
				ISelection currentSelection= getSelection();
				if (fireOpen && currentSelection != null && selection.equals(currentSelection)) {
					fireOpen(new OpenEvent(this, selection));
				}
			}
		}
	}

	
	
}
