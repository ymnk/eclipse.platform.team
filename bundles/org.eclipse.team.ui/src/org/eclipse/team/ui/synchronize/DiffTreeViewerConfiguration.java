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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.team.core.subscribers.SyncInfoSet;
import org.eclipse.team.internal.core.Assert;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.ui.synchronize.actions.ExpandAllAction;
import org.eclipse.team.ui.synchronize.views.*;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.internal.PluginAction;
import org.eclipse.ui.views.navigator.ResourceSorter;

/**
 * Configures the following behavior of a SyncInfoDiffTreeViewer:
 * <ul>
 * <li>context menus
 * <li>
 * @since 3.0
 */
public class DiffTreeViewerConfiguration implements IPropertyChangeListener {
	
	private SyncInfoSet set;
	private String menuId;
	private StructuredViewer viewer;
	
	private ExpandAllAction expandAllAction;
	
	/**
	 * Create a <code>SyncInfoSetCompareConfiguration</code> for the given sync set
	 * and menuId. If the menuId is <code>null</code>, then no contributed menus will be shown
	 * in the diff viewer created from this configuration.
	 * @param menuId the id of menu objectContributions
	 * @param set the <code>SyncInfoSet</code> to be displayed in the resulting diff viewer
	 */
	public DiffTreeViewerConfiguration(SyncInfoSet set) {
		this(null, set);
	}
	
	/**
	 * Create a <code>SyncInfoSetCompareConfiguration</code> for the given sync set
	 * and menuId. If the menuId is <code>null</code>, then no contributed menus will be shown
	 * in the diff viewer created from this configuration.
	 * @param menuId the id of menu objectContributions
	 * @param set the <code>SyncInfoSet</code> to be displayed in the resulting diff viewer
	 */
	public DiffTreeViewerConfiguration(String menuId, SyncInfoSet set) {
		this.menuId = menuId;
		this.set = set;
		TeamUIPlugin.getPlugin().getPreferenceStore().addPropertyChangeListener(this);
	}

	/**
	 * Initialize the viewer with the elements of this configuration, including
	 * content and label providers, sorter, input and menus. This method is invoked from the
	 * constructor of <code>SyncInfoDiffTreeViewer</code> to initialize the viewers. A
	 * configuration instance may only be used with one viewer.
	 * @param parent the parent composite
	 * @param viewer the viewer being initialized
	 */
	public void initializeViewer(Composite parent, final StructuredViewer viewer) {
		Assert.isTrue(this.viewer == null, "A SyncInfoSetCompareConfiguration can only be used with a single viewer."); //$NON-NLS-1$
		this.viewer = viewer;
				
		GridData data = new GridData(GridData.FILL_BOTH);
		viewer.getControl().setLayoutData(data);
		
		initializeListeners(viewer);
		hookContextMenu(viewer);
		initializeActions(viewer);
		
		viewer.setLabelProvider(getLabelProvider());
		viewer.setSorter(getViewSorter());
		viewer.setContentProvider(getContentProvider());
		viewer.setInput(getInput());
	}

	
	/**
	 * Get the input that will be assigned to the viewer initialized by this configuration.
	 * Subclass may override.
	 * @return the viewer input
	 */
	protected SyncInfoDiffNode getInput() {
		return new SyncInfoDiffNode(getSyncSet(), ResourcesPlugin.getWorkspace().getRoot());
	}

	/**
	 * Get the label provider that will be assigned to the viewer initialized by this configuration.
	 * Subclass may override but should either wrap the default one provided
	 * by this method or subclass <code>TeamSubscriberParticipantLabelProvider</code>.
	 * In the later case, the logical label provider should still be assigned to the
	 * subclass of <code>TeamSubscriberParticipantLabelProvider</code>.
	 * @param logicalProvider the label provider for the selected logical view
	 * @return a label provider
	 * @see SyncInfoLabelDecorator
	 */
	protected ILabelProvider getLabelProvider() {
		return new SyncInfoLabelDecorator();
	}
	
	protected IStructuredContentProvider getContentProvider() {
		if(getShowCompressedFolders()) {
			return new CompressedFolderContentProvider();
		}
		return new SyncInfoSetTreeContentProvider();
	}
		
	protected ViewerSorter getViewSorter() {
		return new SyncViewerSorter(ResourceSorter.NAME);
	}
	
	/**
	 * Method invoked from <code>initializeViewer(Composite, StructuredViewer)</code> in order
	 * to initialize any listeners for the viewer.
	 * @param viewer the viewer being initialize
	 */
	protected void initializeListeners(final StructuredViewer viewer) {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				handleDoubleClick(viewer, event);
			}
		});
	}

	/**
	 * Method invoked from <code>initializeViewer(Composite, StructuredViewer)</code> in order
	 * to initialize any actions for the viewer. It is invoked before the input is set on
	 * the viewer in order to allow actions to be initialized before there is any reaction 
	 * to the input being set (e.g. selecting and opening the first element).
	 * <p>
	 * The default behavior is to add the up and down navigation nuttons to the toolbar.
	 * Subclasses can override.
	 * @param viewer the viewer being initialize
	 */
	protected void initializeActions(StructuredViewer viewer) {
		expandAllAction = new ExpandAllAction((AbstractTreeViewer)viewer);
		Utils.initAction(expandAllAction, "action.expandAll."); //$NON-NLS-1$
	}
	
	/**
	 * Return the <code>SyncInfoSet</code> being shown by the viewer associated with
	 * this configuration.
	 * @return a <code>SyncInfoSet</code>
	 */
	public SyncInfoSet getSyncSet() {
		return set;
	}

	/**
	 * Method invoked from <code>initializeViewer(Composite, StructuredViewer)</code> in order
	 * to configure the viewer to call <code>fillContextMenu(StructuredViewer, IMenuManager)</code>
	 * when a context menu is being displayed in the diff tree viewer.
	 * @param viewer the viewer being initialized
	 * @see fillContextMenu(StructuredViewer, IMenuManager)
	 */
	protected void hookContextMenu(final StructuredViewer viewer) {
		final MenuManager menuMgr = new MenuManager(menuId); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(viewer, manager);
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
		
		if(allowParticipantMenuContributions()) {
			IWorkbenchPartSite site = Utils.findSite(viewer.getControl());
			if(site == null) {
				site = Utils.findSite();
			}
			if(site != null) {
				site.registerContextMenu(menuId, menuMgr, viewer);
			}
		}
	}
	
	/**
	 * Callback that is invoked when a context menu is about to be shown in the diff viewer.
	 * @param viewer the viewer
	 * @param manager the menu manager
	 */
	protected void fillContextMenu(final StructuredViewer viewer, IMenuManager manager) {
		manager.add(expandAllAction);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	protected StructuredViewer getViewer() {
		return viewer;
	}
	
	/**
	 * Cleanup listeners
	 */	
	public void dispose() {
		TeamUIPlugin.getPlugin().getPreferenceStore().removePropertyChangeListener(this);
	}
	
	/**
	 * Handles a double-click event from the viewer.
	 * Expands or collapses a folder when double-clicked.
	 * 
	 * @param viewer the viewer
	 * @param event the double-click event
	 */
	protected void handleDoubleClick(StructuredViewer viewer, DoubleClickEvent event) {
		IStructuredSelection selection = (IStructuredSelection) event.getSelection();
		Object element = selection.getFirstElement();
		if(viewer instanceof AbstractTreeViewer) {
			AbstractTreeViewer treeViewer = ((AbstractTreeViewer)viewer); 
			if(treeViewer.getExpandedState(element)) {
				treeViewer.collapseToLevel(element, AbstractTreeViewer.ALL_LEVELS);
			} else {
				treeViewer.expandToLevel(element, AbstractTreeViewer.ALL_LEVELS);
			}
		}
	}
	
	/**
	 * Callback that is used to convert resource label change events into 
	 * label change events on the model objects (namely <code>SyncInfoDiffNode</code>
	 * instances. Any provided objects that cannot be converted to resources are 
	 * returned as-is in the resulting array.
	 * @param viewer the viewer
	 * @param changed the changed objects
	 * @return the changed objects converted to diff model objects if possible
	 */
	protected Object[] asModelObjects(StructuredViewer viewer, Object[] changed) {
		if (changed != null && viewer.getInput() != null) {
			ArrayList others= new ArrayList();
			for (int i= 0; i < changed.length; i++) {
				Object curr = changed[i];
				if (curr instanceof IResource) {
					IContentProvider provider = viewer.getContentProvider();
					if (provider != null && provider instanceof SyncInfoSetContentProvider) {
						curr = ((SyncInfoSetContentProvider)provider).getModelObject((IResource)curr);
					}
				}
				others.add(curr);
			}
			return others.toArray();
		}
		return null;
	}
	
	/**
	 * Return the menu id that is used to obtain context menu items from the workbench.
	 * @return the menuId.
	 */
	public String getMenuId() {
		return menuId;
	}

	/**
	 * Returns whether workbench menu items whould be included in the context menu.
	 * By default, this returns <code>true</code> if there is a menu id and <code>false</code>
	 * otherwise
	 * @return whether to include workbench context menu items
	 */
	protected boolean allowParticipantMenuContributions() {
		return getMenuId() != null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {	
		if (viewer != null && event.getProperty().equals(IPreferenceIds.SYNCVIEW_COMPRESS_FOLDERS)) {
			viewer.setContentProvider(getContentProvider());
		}
	}
		
	private boolean getShowCompressedFolders() {
		return TeamUIPlugin.getPlugin().getPreferenceStore().getBoolean(IPreferenceIds.SYNCVIEW_COMPRESS_FOLDERS);
	}
}