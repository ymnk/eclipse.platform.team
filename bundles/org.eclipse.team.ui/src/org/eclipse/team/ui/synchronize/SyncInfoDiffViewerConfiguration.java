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

import java.util.Iterator;

import org.eclipse.compare.*;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.team.core.subscribers.SyncInfoSet;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.synchronize.views.*;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.internal.PluginAction;
import org.eclipse.ui.views.navigator.ResourceSorter;

/**
 * This class provides the configurability of SyncInfo diff viewers
 */
public class SyncInfoDiffViewerConfiguration {
	
	private SyncInfoSet set;
	
	private StructuredViewer viewer;
	private MenuManager menuMgr = null;
	private String menuId;
	private Action expandAll;
	private NavigationAction nextAction;
	private NavigationAction previousAction;
	
	private boolean acceptParticipantMenuContributions = false;
	
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
	
	public SyncInfoDiffViewerConfiguration(String menuId, SyncInfoSet set) {
		this.menuId = menuId;
		this.set = set;
	}
	
	public SyncInfoDiffTreeViewer createDiffTreeViewer(Composite parent) {
		final SyncInfoDiffTreeViewer treeViewer = new SyncInfoDiffTreeViewer(parent, this);
		this.viewer = treeViewer;
		GridData data = new GridData(GridData.FILL_BOTH);
		treeViewer.setSorter(new SyncViewerSorter(ResourceSorter.NAME));
		treeViewer.setLabelProvider(new TeamSubscriberParticipantLabelProvider());
		treeViewer.getTree().setLayoutData(data);
		getStore().addPropertyChangeListener(propertyListener);
		setTreeViewerContentProvider();
		
		initializeListeners();
		
		treeViewer.getTree().setData(CompareUI.COMPARE_VIEWER_TITLE, getTitle());
		treeViewer.initializeNavigation();
		createActions();
		hookContextMenu();
		
		createToolBarActions(parent);			
		treeViewer.setInput(new SyncInfoDiffNode(getSyncSet(), ResourcesPlugin.getWorkspace().getRoot()));
		return treeViewer;
	}
	
	public SyncInfoDiffTreeViewer createDiffCheckboxTreeViewer(Composite parent) {
		final SyncInfoDiffTreeViewer treeViewer = new SyncInfoDiffTreeViewer(parent, this);
		this.viewer = treeViewer;
		GridData data = new GridData(GridData.FILL_BOTH);
		treeViewer.setSorter(new SyncViewerSorter(ResourceSorter.NAME));
		treeViewer.setLabelProvider(new TeamSubscriberParticipantLabelProvider());
		treeViewer.getTree().setLayoutData(data);
		getStore().addPropertyChangeListener(propertyListener);
		setTreeViewerContentProvider();
		
		initializeListeners();
		
		treeViewer.getTree().setData(CompareUI.COMPARE_VIEWER_TITLE, getTitle());
		treeViewer.initializeNavigation();
		createActions();
		hookContextMenu();
		
		createToolBarActions(parent);			
		treeViewer.setInput(new SyncInfoDiffNode(getSyncSet(), ResourcesPlugin.getWorkspace().getRoot()));
		return treeViewer;
	}
	
	protected void initializeListeners() {
		getViewer().addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				handleDoubleClick(event);
			}
		});
	}

	public SyncInfoSet getSyncSet() {
		return set;
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
	
	/**
	 * Return the preference store for this plugin.
	 * @return IPreferenceStore for this plugin
	 */
	private IPreferenceStore getStore() {
		return TeamUIPlugin.getPlugin().getPreferenceStore();
	}
	
	private void setTreeViewerContentProvider() {
		if (getStore().getBoolean(IPreferenceIds.SYNCVIEW_COMPRESS_FOLDERS)) {
			getViewer().setContentProvider(new CompressedFolderContentProvider());
		} else {
			getViewer().setContentProvider(new SyncSetTreeContentProvider());
		}
	}
	
	public StructuredViewer getViewer() {
		return viewer;
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
		AbstractTreeViewer tree = getAbstractTreeViewer();
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
	
	protected void hookContextMenu() {
		menuMgr = new MenuManager(menuId); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
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
							((PluginAction)actionItem).selectionChanged(viewer.getSelection());
						}
					}
				}
			}
		});
		viewer.getControl().setMenu(menu);
		
		if(acceptParticipantMenuContributions) {
			IWorkbenchPartSite site = Utils.findSite(viewer.getControl());
			if(site == null) {
				site = Utils.findSite();
			}
			if(site != null) {
				site.registerContextMenu(menuId, menuMgr, viewer);
			}
		}
	}
	
	protected void fillContextMenu(IMenuManager manager) {
		if (getAbstractTreeViewer() != null) {
			manager.add(expandAll);
		}
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private AbstractTreeViewer getAbstractTreeViewer() {
		if (viewer instanceof AbstractTreeViewer) {
			return (AbstractTreeViewer)viewer;
		}
		return null;
	}

	protected Object getTitle() {
		return "Synchronization Changes";
	}
	
	/**
	 * Cleanup listeners
	 */	
	protected void dispose() {
		getStore().removePropertyChangeListener(propertyListener);
	}
	
	public void setAcceptParticipantMenuContributions(boolean accept) {
		this.acceptParticipantMenuContributions = accept;
		if(acceptParticipantMenuContributions) {
			IWorkbenchPartSite site = Utils.findSite(getViewer().getControl());
			if(site == null) {
				site = Utils.findSite();
			}
			if(site != null) {
				site.registerContextMenu(menuId, menuMgr, getViewer());
			}
		}
	}
	
	public void updateCompareEditorInput(CompareEditorInput input) {
		nextAction.setCompareEditorInput(input);
		previousAction.setCompareEditorInput(input);
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
		AbstractTreeViewer tree = getAbstractTreeViewer();
		if(tree != null && tree.isExpandable(element)) {
			tree.setExpandedState(element, ! tree.getExpandedState(element));
		}
	}
	
}
