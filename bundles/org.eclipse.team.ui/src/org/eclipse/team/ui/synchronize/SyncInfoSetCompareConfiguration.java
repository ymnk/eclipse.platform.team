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

import org.eclipse.compare.*;
import org.eclipse.compare.internal.INavigatable;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.SyncInfoSet;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.registry.LogicalViewRegistry;
import org.eclipse.team.internal.ui.synchronize.actions.LogicalViewActionGroup;
import org.eclipse.team.internal.ui.synchronize.views.*;
import org.eclipse.team.ui.ITeamUIConstants;
import org.eclipse.team.ui.synchronize.actions.INavigableTree;
import org.eclipse.team.ui.synchronize.actions.SyncInfoDiffTreeNavigator;
import org.eclipse.team.ui.synchronize.content.*;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.internal.PluginAction;
import org.eclipse.ui.views.navigator.ResourceSorter;

/**
 * This class provides the configurability of SyncInfo diff viewers.
 * A configuration can only be used with one viewer.
 * TODO: Should we make it reusable? If not, should assert on second call to createViewer
 * 
 * @since 3.0
 */
public class SyncInfoSetCompareConfiguration {
	
	private static LogicalViewRegistry logicalViewRegistry;
	
	private SyncInfoSet set;
	private String menuId;
	private boolean acceptParticipantMenuContributions = false;
	
	// fields that are tied to a single viewer
	private SyncInfoDiffTreeNavigator navigator;
	private IPropertyChangeListener propertyListener;
	
	private LogicalViewActionGroup logicalViews;
	
	private static synchronized LogicalViewRegistry getLogicalViewRegistry() {
		if (logicalViewRegistry == null) {
			logicalViewRegistry = new LogicalViewRegistry();
			logicalViewRegistry.readRegistry(Platform.getPluginRegistry(), TeamUIPlugin.ID, ITeamUIConstants.PT_LOGICAL_VIEWS);
		}
		return logicalViewRegistry;
	}
	
	/**
	 * Return all the logical views that have been registered with Team.
	 * The providers associated with the views will not be instantiated until
	 * they are accessed so this list can be retrieved without fear of loading
	 * client plugins.
	 * @return the regisitered logical views
	 */
	public static ILogicalView[] getLogicalViews() {
		return getLogicalViewRegistry().getLogicalViews();
	}
	
	/**
	 * Return the registered view with the given id or <code>null</code> if no view
	 * exists for the given id. The provider associated with the view will not be instantiated until
	 * it is accessed so the logical view can be retrieved without fear of loading
	 * client plugins.
	 * @param id the id of the logical view
	 * @return the logical view with the given id or <code>null</code>
	 */
	public static ILogicalView getLogicalView(String id) {
		return getLogicalViewRegistry().getLogicalView(id);
	}
	
	/**
	 * Create a <code>SyncInfoSetCompareConfiguration</code> for the given sync set
	 * and menuId. If the menuId is <code>null</code>, then no contributed menus will be shown
	 * in the diff viewer created from this configuration.
	 * @param menuId the id of menu objectContributions
	 * @param set the <code>SyncInfoSet</code> to be displayed in the resulting diff viewer
	 */
	public SyncInfoSetCompareConfiguration(String menuId, SyncInfoSet set) {
		this.menuId = menuId;
		this.set = set;
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
		ILogicalView view = getDefaultLogicalView();
		setLogicalViewProvider(viewer, view);
		logicalViews = new LogicalViewActionGroup();
		logicalViews.setSelectedView(view);
		
		GridData data = new GridData(GridData.FILL_BOTH);
		viewer.getControl().setLayoutData(data);
		propertyListener = getPropertyListener(viewer);
		getStore().addPropertyChangeListener(propertyListener);
		
		initializeListeners(viewer);
		
		viewer.getControl().setData(CompareUI.COMPARE_VIEWER_TITLE, getTitle());
		if (viewer instanceof INavigableTree) {
			initializeNavigation(viewer.getControl(), (INavigableTree)viewer);
			navigator.createActions(viewer);
		}
		hookContextMenu(viewer);
		
		createToolBarActions(parent);
		// TODO: This relates to the content provider (i.e. the use of SyncInfoDiffNode)
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
	 * Subclass may override but any created label provider should wrap the default one provided
	 * by this method.
	 * @return a label provider
	 */
	protected ILabelProvider getLabelProvider(SyncInfoLabelProvider logicalProvider) {
		return new TeamSubscriberParticipantLabelProvider(logicalProvider);
	}

	protected void initializeNavigation(Control tree, INavigableTree target) {
		this.navigator = new SyncInfoDiffTreeNavigator(target);
		INavigatable nav= new INavigatable() {
			public boolean gotoDifference(boolean next) {
				// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=20106
				return navigator.navigate(next, true);
			}
		};
		tree.setData(INavigatable.NAVIGATOR_PROPERTY, nav);
	}
	
	protected void initializeListeners(final StructuredViewer viewer) {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				handleDoubleClick(viewer, event);
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
			
			navigator.createToolItems(tbm);
			tbm.update(true);
		}
	}
	
	/**
	 * Change the tree layout between using compressed folders and regular folders
	 * when the user setting is changed.
	 */
	private IPropertyChangeListener getPropertyListener(final StructuredViewer viewer) {
		return new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty().equals(IPreferenceIds.SYNCVIEW_COMPRESS_FOLDERS)) {
					// TODO: 
					//viewer.setContentProvider(getContentProvider());
				}
			}
		};
	}

	/**
	 * Return the preference store for this plugin.
	 * @return IPreferenceStore for this plugin
	 */
	private IPreferenceStore getStore() {
		return TeamUIPlugin.getPlugin().getPreferenceStore();
	}
	
	/**
	 * Get the content provider that will be assigned to the viewer initialized by this configuration.
	 * Subclass may override but should return a subclass of <code>SyncSetContentProvider</code>.
	 * @return a content provider
	 */
	protected ILogicalView getDefaultLogicalView() {
		if (getStore().getBoolean(IPreferenceIds.SYNCVIEW_COMPRESS_FOLDERS)) {
			return getLogicalView(CompressFolderView.ID);
		} else {
			return null;
		}
	}

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
	
	protected void fillContextMenu(final StructuredViewer viewer, IMenuManager manager) {
		navigator.fillContextMenu(viewer, manager);
		addLogicalViewSelection(viewer, manager);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	protected void addLogicalViewSelection(final StructuredViewer viewer, IMenuManager manager) {
		logicalViews.fillContextMenu(manager);
		logicalViews.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {
				if (event.getProperty().equals(LogicalViewActionGroup.SELECTED_VIEW)) {
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							ILogicalView view = (ILogicalView)event.getNewValue();
							setLogicalViewProvider(viewer, view);
						}
					});
				}
			}
		});
	}

	protected void setLogicalViewProvider(StructuredViewer viewer, ILogicalView view) {
		if (view != null) {
			try {
				LogicalViewProvider provider = view.getLogicalViewProvider();
				viewer.setContentProvider(provider.getContentProvider());
				viewer.setLabelProvider(getLabelProvider(provider.getLabelProvider()));
				viewer.setSorter(provider.getSorter());
			} catch (CoreException e) {
				TeamUIPlugin.log(e);
				view = null;
			}
		}
		if (view == null) {
			viewer.setContentProvider(new SyncSetTreeContentProvider());
			viewer.setLabelProvider(getLabelProvider(new SyncInfoLabelProvider()));
			viewer.setSorter(new SyncViewerSorter(ResourceSorter.NAME));
		}
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
	
	public void updateCompareEditorInput(CompareEditorInput input) {
		navigator.updateCompareEditorInput(input);
	}
	
	/**
	 * Handles a double-click event from the viewer.
	 * Expands or collapses a folder when double-clicked.
	 * 
	 * @param event the double-click event
	 */
	protected void handleDoubleClick(StructuredViewer viewer, DoubleClickEvent event) {
		IStructuredSelection selection = (IStructuredSelection) event.getSelection();
		Object element = selection.getFirstElement();	
		navigator.reveal(viewer, element);
	}

	public SyncInfoDiffTreeNavigator getNavigator() {
		return navigator;
	}
	
	protected Object[] asModelObjects(StructuredViewer viewer, Object[] changed) {
		if (changed != null && viewer.getInput() != null) {
			ArrayList others= new ArrayList();
			for (int i= 0; i < changed.length; i++) {
				Object curr = changed[i];
				if (curr instanceof IResource) {
					IContentProvider provider = viewer.getContentProvider();
					if (provider != null && provider instanceof SyncSetContentProvider) {
						curr = ((SyncSetContentProvider)provider).getModelObject((IResource)curr);
					}
				}
				others.add(curr);
			}
			return others.toArray();
		}
		return null;
	}
	
	/**
	 * @return Returns the menuId.
	 */
	public String getMenuId() {
		return menuId;
	}

	protected boolean allowParticipantMenuContributions() {
		return getMenuId() != null;
	}
	
	/**
	 * Prepare the input that is to be shown in the diff viewer of the configuration's
	 * compare input. This method may be overridden by sublcass but should only be
	 * invoked by the compare input
	 * @param monitor a progress monitor
	 * @return the input ot the compare input's diff viewer
	 * @throws TeamException
	 */
	public SyncInfoDiffNode prepareInput(IProgressMonitor monitor) throws TeamException {
		return getInput();
	}
}
