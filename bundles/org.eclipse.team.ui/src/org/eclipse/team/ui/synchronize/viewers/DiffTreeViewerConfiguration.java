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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfoTree;
import org.eclipse.team.internal.core.Assert;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.synchronize.actions.ExpandAllAction;
import org.eclipse.team.internal.ui.synchronize.views.TreeViewerUtils;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.internal.PluginAction;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;

/**
 * A <code>DiffTreeViewerConfiguration</code> object controls various UI
 * aspects of sync info viewers like the context menu, toolbar, content
 * provider, and label provider. A configuration is created to display
 * {@link SyncInfo}objects contained in the provided {@link SyncInfoSet}.
 * <p>
 * This configuration allows viewer contributions made in a plug-in manifest to
 * be scoped to a particular unique id. As a result the context menu for the
 * viewer can be configured to show object contributions for random id schemes.
 * To enable declarative action contributions for a configuration there are two
 * steps required:
 * <ul>
 * <li>Create a viewer contribution with a <code>targetID</code> that groups
 * sets of actions that are related. A common pratice for synchronize view
 * configurations is to use the participant id as the targetID.
 * 
 * <pre>
 *  &lt;viewerContribution
 *  id=&quot;org.eclipse.team.ccvs.ui.CVSCompareSubscriberContributions&quot;
 *  targetID=&quot;org.eclipse.team.cvs.ui.compare-participant&quot;&gt;
 *  ...
 * </pre>
 * 
 * <li>Create a configuration instance with a <code>menuID</code> that
 * matches the targetID in the viewer contribution.
 * </ul>
 * <p>
 * Clients may use this class as is, or subclass to add new state and behavior.
 * The default behavior is to show sync info in a tree
 * </p>
 * @since 3.0
 */
public class DiffTreeViewerConfiguration implements IPropertyChangeListener {

	private SyncInfoTree set;
	private String menuID;
	private AbstractTreeViewer viewer;
	private ExpandAllAction expandAllAction;
	private SyncInfoSetViewerInput viewerInput;

	/**
	 * Create a <code>SyncInfoSetCompareConfiguration</code> for the given
	 * sync set.
	 * @param set
	 *            the <code>SyncInfoSet</code> to be displayed in the
	 *            resulting diff viewer.
	 */
	public DiffTreeViewerConfiguration(SyncInfoTree set) {
		this(null, set);
	}

	/**
	 * Create a <code>SyncInfoSetCompareConfiguration</code> for the given
	 * sync set and menuId. If the menuId is <code>null</code>, then no
	 * contributed menus will be shown in the diff viewer created from this
	 * configuration.
	 * @param menuId
	 *            the id of <code>targetID</code> specified in <code>viewerContribution</code>
	 *            extension points.
	 * @param set
	 *            the <code>SyncInfoSet</code> to be displayed in the
	 *            resulting diff viewer
	 */
	public DiffTreeViewerConfiguration(String menuID, SyncInfoTree set) {
		this.menuID = menuID;
		this.set = set;
		TeamUIPlugin.getPlugin().getPreferenceStore().addPropertyChangeListener(this);
	}

	/**
	 * Initialize the viewer with the elements of this configuration, including
	 * content and label providers, sorter, input and menus. This method is
	 * invoked from the constructor of <code>SyncInfoDiffTreeViewer</code> to
	 * initialize the viewers. A configuration instance may only be used with
	 * one viewer.
	 * @param parent
	 *            the parent composite
	 * @param viewer
	 *            the viewer being initialized
	 */
	public void initializeViewer(Composite parent, AbstractTreeViewer viewer) {
		Assert.isTrue(this.viewer == null, "A DiffTreeViewerConfiguration can only be used with a single viewer."); //$NON-NLS-1$
		this.viewer = viewer;
		GridData data = new GridData(GridData.FILL_BOTH);
		viewer.getControl().setLayoutData(data);
		initializeListeners(viewer);
		hookContextMenu(viewer);
		initializeActions(viewer);
		viewer.setLabelProvider(getLabelProvider());
		viewer.setContentProvider(getContentProvider());
		
		// The input may of been set already. In that case, don't change it and
		// simply assign it to the view.
		if(viewerInput == null) {
			viewerInput = (SyncInfoSetViewerInput)getInput();
			viewerInput.prepareInput(null);
		}
		setInput(viewer);
	}

	/**
	 * @param viewer
	 */
	private void setInput(AbstractTreeViewer viewer) {
		viewerInput.setViewer(viewer);
		viewer.setSorter(viewerInput.getViewerSorter());
		viewer.setInput(viewerInput);
	}

	/**
	 * Creates the input for this view and initializes it. At the time this method
	 * is called the viewer may not of been created yet. 
	 * 
	 * @param monitor shows progress while preparing the input
	 * @return the input that can be shown in a viewer
	 */
	public Object prepareInput(IProgressMonitor monitor)throws TeamException {
		if(viewerInput != null) {
			viewerInput.dispose();
		}
		viewerInput = (SyncInfoSetViewerInput) getInput();		
		viewerInput.prepareInput(monitor);
		return viewerInput;
	}

	/**
	 * Get the input that will be assigned to the viewer initialized by this
	 * configuration. Subclass may override.
	 * @return the viewer input
	 */
	protected Object getInput() {
		if (getShowCompressedFolders()) {
			return new CompressedFolderViewerInput(getSyncInfoTree());
		}
		return new SyncInfoSetViewerInput(getSyncInfoTree());
	}

	/**
	 * Get the label provider that will be assigned to the viewer initialized
	 * by this configuration. Subclass may override but should either wrap the
	 * default one provided by this method or subclass <code>TeamSubscriberParticipantLabelProvider</code>.
	 * In the later case, the logical label provider should still be assigned
	 * to the subclass of <code>TeamSubscriberParticipantLabelProvider</code>.
	 * @param logicalProvider
	 *            the label provider for the selected logical view
	 * @return a label provider
	 * @see SyncInfoLabelProvider
	 */
	protected ILabelProvider getLabelProvider() {
		return new SyncInfoLabelProvider();
	}

	protected IStructuredContentProvider getContentProvider() {
		return new BaseWorkbenchContentProvider();
	}

	/**
	 * Method invoked from <code>initializeViewer(Composite, StructuredViewer)</code>
	 * in order to initialize any listeners for the viewer.
	 * @param viewer
	 *            the viewer being initialize
	 */
	protected void initializeListeners(final StructuredViewer viewer) {
		viewer.addDoubleClickListener(new IDoubleClickListener() {

			public void doubleClick(DoubleClickEvent event) {
				handleDoubleClick(viewer, event);
			}
		});
	}

	/**
	 * Method invoked from <code>initializeViewer(Composite, StructuredViewer)</code>
	 * in order to initialize any actions for the viewer. It is invoked before
	 * the input is set on the viewer in order to allow actions to be
	 * initialized before there is any reaction to the input being set (e.g.
	 * selecting and opening the first element).
	 * <p>
	 * The default behavior is to add the up and down navigation nuttons to the
	 * toolbar. Subclasses can override.
	 * @param viewer
	 *            the viewer being initialize
	 */
	protected void initializeActions(StructuredViewer viewer) {
		expandAllAction = new ExpandAllAction((AbstractTreeViewer) viewer);
		Utils.initAction(expandAllAction, "action.expandAll."); //$NON-NLS-1$
	}

	/**
	 * Return the <code>SyncInfoSet</code> being shown by the viewer
	 * associated with this configuration.
	 * @return a <code>SyncInfoSet</code>
	 */
	public SyncInfoTree getSyncInfoTree() {
		return set;
	}

	/**
	 * Method invoked from <code>initializeViewer(Composite, StructuredViewer)</code>
	 * in order to configure the viewer to call <code>fillContextMenu(StructuredViewer, IMenuManager)</code>
	 * when a context menu is being displayed in the diff tree viewer.
	 * @param viewer
	 *            the viewer being initialized
	 * @see fillContextMenu(StructuredViewer, IMenuManager)
	 */
	protected void hookContextMenu(final StructuredViewer viewer) {
		final MenuManager menuMgr = new MenuManager(menuID); //$NON-NLS-1$
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
					if (item instanceof ActionContributionItem) {
						IAction actionItem = ((ActionContributionItem) item).getAction();
						if (actionItem instanceof PluginAction) {
							((PluginAction) actionItem).selectionChanged(viewer.getSelection());
						}
					}
				}
			}
		});
		viewer.getControl().setMenu(menu);
		if (allowParticipantMenuContributions()) {
			IWorkbenchPartSite site = Utils.findSite(viewer.getControl());
			if (site == null) {
				site = Utils.findSite();
			}
			if (site != null) {
				site.registerContextMenu(menuID, menuMgr, viewer);
			}
		}
	}

	/**
	 * Callback that is invoked when a context menu is about to be shown in the
	 * diff viewer.
	 * @param viewer
	 *            the viewer
	 * @param manager
	 *            the menu manager
	 */
	protected void fillContextMenu(final StructuredViewer viewer, IMenuManager manager) {
		manager.add(expandAllAction);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	protected AbstractTreeViewer getViewer() {
		return viewer;
	}

	/**
	 * Cleanup listeners
	 */
	public void dispose() {
		if(viewerInput != null) {
			viewerInput.dispose();
		}
		TeamUIPlugin.getPlugin().getPreferenceStore().removePropertyChangeListener(this);
	}

	/**
	 * Handles a double-click event from the viewer. Expands or collapses a
	 * folder when double-clicked.
	 * @param viewer
	 *            the viewer
	 * @param event
	 *            the double-click event
	 */
	protected void handleDoubleClick(StructuredViewer viewer, DoubleClickEvent event) {
		IStructuredSelection selection = (IStructuredSelection) event.getSelection();
		Object element = selection.getFirstElement();
		if (viewer instanceof TreeViewer) {
			AbstractTreeViewer treeViewer = ((AbstractTreeViewer) viewer);
			if (treeViewer.getExpandedState(element)) {
				treeViewer.collapseToLevel(element, AbstractTreeViewer.ALL_LEVELS);
			} else {
				TreeViewerUtils.navigate((TreeViewer) viewer, true /* next */, false /* no-open */, true /* only-expand */);
			}
		}
	}

	/**
	 * Return the menu id that is used to obtain context menu items from the
	 * workbench.
	 * @return the menuId.
	 */
	public String getMenuId() {
		return menuID;
	}

	/**
	 * Returns whether workbench menu items whould be included in the context
	 * menu. By default, this returns <code>true</code> if there is a menu id
	 * and <code>false</code> otherwise
	 * @return whether to include workbench context menu items
	 */
	protected boolean allowParticipantMenuContributions() {
		return getMenuId() != null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (viewer != null && event.getProperty().equals(IPreferenceIds.SYNCVIEW_COMPRESS_FOLDERS)) {
			try {
				prepareInput(null);
				setInput(getViewer());
			} catch (TeamException e) {
				TeamUIPlugin.log(e);
			}
		}
	}

	private boolean getShowCompressedFolders() {
		return TeamUIPlugin.getPlugin().getPreferenceStore().getBoolean(IPreferenceIds.SYNCVIEW_COMPRESS_FOLDERS);
	}

	protected void aSyncExec(Runnable r) {
		final Control ctrl = viewer.getControl();
		if (ctrl != null && !ctrl.isDisposed()) {
			ctrl.getDisplay().asyncExec(r);
		}
	}
}