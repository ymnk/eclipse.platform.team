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
package org.eclipse.team.ui.synchronize.subscribers;

import java.util.HashMap;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.*;
import org.eclipse.jface.util.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.team.core.subscribers.WorkingSetFilteredSyncInfoCollector;
import org.eclipse.team.internal.core.Assert;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.synchronize.SyncInfoModelElement;
import org.eclipse.team.internal.ui.synchronize.actions.*;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.*;
import org.eclipse.ui.internal.PluginAction;


/**
 * A <code>SynchronizeConfiguration</code> object controls various UI aspects of 
 * synchronization viewers.
 * <p>
 * Clients may use this class as is, or subclass to add new state and behavior.
 * </p>
 * @since 3.0
 */
public class SubscriberConfiguration implements ISynchronizeConfiguration {
	
	public interface IContextMenuListener {
		// Contants used to identify customizable menu areas
		public static final String FILE_MENU = "File"; //$NON-NLS-1$
		public static final String EDIT_MENU = "Edit"; //$NON-NLS-1$
		public static final String NAVIGATE_MENU = "Navigate"; //$NON-NLS-1$
		public void fillContextMenu(IMenuManager manager);
	}
	
	/**
	 * Property constant indicating the mode of a page has changed. 
	 */
	public static final String P_SYNCVIEWPAGE_WORKINGSET = TeamUIPlugin.ID  + ".P_SYNCVIEWPAGE_WORKINGSET";	 //$NON-NLS-1$
	
	/**
	 * Property constant indicating the mode of a page has changed. 
	 */
	public static final String P_SYNCVIEWPAGE_MODE = TeamUIPlugin.ID  + ".P_SYNCVIEWPAGE_MODE";	 //$NON-NLS-1$
	
	/**
	 * Modes are direction filters for the view
	 */
	public final static int INCOMING_MODE = 0x1;
	public final static int OUTGOING_MODE = 0x2;
	public final static int BOTH_MODE = 0x4;
	public final static int CONFLICTING_MODE = 0x8;
	public final static int ALL_MODES = INCOMING_MODE | OUTGOING_MODE | CONFLICTING_MODE | BOTH_MODE;
	
	private SubscriberParticipant participant;
	
	private ListenerList fListeners= new ListenerList();
	private HashMap fProperties= new HashMap();

	/**
	 * Filters out-of-sync resources by working set and mode
	 */
	private WorkingSetFilteredSyncInfoCollector collector;
	
	// Properties
	private int mode;
	private int supportedModes;
	private IWorkingSet workingSet;

	private ISynchronizePageSite site;
	
	// Actions
	private OpenWithActionGroup openWithActions;
	private RefactorActionGroup refactorActions;
	private Action refreshSelectionAction;
	private ExpandAllAction expandAllAction;
	
	private ListenerList contextMenuListeners;
	
	public SubscriberConfiguration(SubscriberParticipant participant) {
		Assert.isNotNull(participant);
		this.participant = participant;
	}
	
	public ISynchronizeParticipant getParticipant() {
		return participant;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeConfiguration#initialize(org.eclipse.team.ui.synchronize.StructuredViewerAdvisor)
	 */
	public void initialize(StructuredViewerAdvisor advisor) {
		initializeCollector();
		initializeActions(advisor);
		addContextMenuListener(new IContextMenuListener() {
			public void fillContextMenu(IMenuManager manager) {
				if (openWithActions != null) {
					openWithActions.fillContextMenu(manager, FILE_MENU);
				}
				if (refactorActions != null) {
					refactorActions.fillContextMenu(manager, EDIT_MENU);
				}
				if (refreshSelectionAction != null) {
					manager.insertAfter(EDIT_MENU, refreshSelectionAction);
				}
				if (expandAllAction != null) {
					manager.insertAfter(NAVIGATE_MENU, expandAllAction);
				}
			}
		});
		initializeListeners(advisor.getViewer());
	}
	
	private void initializeCollector() {
		collector = new WorkingSetFilteredSyncInfoCollector(((SubscriberParticipant)getParticipant()).getSubscriberSyncInfoCollector(), participant.getSubscriber().roots());
		collector.reset();
	}

	public void dispose() {
		collector.dispose();
	}
	
	/**
	 * Add a context menu listener that will get invoked when the context
	 * menu is shown for the viewer associated with this advisor.
	 * @param listener a context menu listener
	 */
	public void addContextMenuListener(IContextMenuListener listener) {
		if (contextMenuListeners == null) {
			contextMenuListeners = new ListenerList();
		}
		contextMenuListeners.add(listener);
	}
	
	/**
	 * Remove a previously registered content menu listener.
	 * Removing a listener that is not registered has no effect.
	 * @param listener a context menu listener
	 */
	public void removeContextMenuListener(IContextMenuListener listener) {
		if (contextMenuListeners != null) {
			contextMenuListeners.remove(listener);
		}
	}
	
	/**
	 * Fires a <code>PropertyChangeEvent</code> to registered listeners.
	 *
	 * @param propertyName the name of the property that has changed
	 * @param oldValue the property's old value
	 * @param newValue the property's new value
	 */
	private void fireChange(String propertyName, Object oldValue, Object newValue) {
		PropertyChangeEvent event= null;
		Object[] listeners= fListeners.getListeners();
		if (listeners != null) {
			for (int i= 0; i < listeners.length; i++) {
				IPropertyChangeListener l= (IPropertyChangeListener) listeners[i];
				if (event == null)
					event= new PropertyChangeEvent(this, propertyName, oldValue, newValue);
				l.propertyChange(event);
			}
		}
	}

	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		fListeners.add(listener);
	}

	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		fListeners.remove(listener);
	}

	/**
	 * Sets the property with the given name.
	 * If the new value differs from the old a <code>PropertyChangeEvent</code>
	 * is sent to registered listeners.
	 *
	 * @param propertyName the name of the property to set
	 * @param value the new value of the property
	 */
	public void setProperty(String key, Object newValue) {
		Object oldValue= fProperties.get(key);
		fProperties.put(key, newValue);
		if (oldValue == null || !oldValue.equals(newValue))
			fireChange(key, oldValue, newValue);
	}

	/**
	 * Returns the property with the given name, or <code>null</code>
	 * if no such property exists.
	 *
	 * @param propertyName the name of the property to retrieve
	 * @return the property with the given name, or <code>null</code> if not found
	 */
	public Object getProperty(String key) {
		return fProperties.get(key);
	}
	/**
	 * @return Returns the mode.
	 */
	public int getMode() {
		return mode;
	}
	/**
	 * @param mode The mode to set.
	 */
	public void setMode(int mode) {
		int oldMode = getMode();
		if(oldMode == mode) return;
		this.mode = mode;
		fireChange(P_SYNCVIEWPAGE_MODE, new Integer(oldMode), new Integer(mode));
	}
	/**
	 * @return Returns the workingSet.
	 */
	public IWorkingSet getWorkingSet() {
		return workingSet;
	}
	/**
	 * @param workingSet The workingSet to set.
	 */
	public void setWorkingSet(IWorkingSet workingSet) {
		IWorkingSet oldSet = workingSet;
		this.workingSet = workingSet;
		fireChange(P_SYNCVIEWPAGE_WORKINGSET, oldSet, workingSet);
	}
	
	/**
	 * @return Returns the supportedModes.
	 */
	public int getSupportedModes() {
		return supportedModes;
	}
	/**
	 * @param supportedModes The supportedModes to set.
	 */
	public void setSupportedModes(int supportedModes) {
		this.supportedModes = supportedModes;
	}

	/**
	 * This method is invoked when the page using this configuration
	 * is initiatialized with a page site
	 * @param site the page site displaying the page using this configuration
	 */
	public void init(ISynchronizePageSite site) {
		this.site = site;
	}

	/**
	 * Method invoked from <code>initializeViewer(StructuredViewer)</code>
	 * in order to configure the viewer to call <code>fillContextMenu(StructuredViewer, IMenuManager)</code>
	 * when a context menu is being displayed in viewer.
	 * 
	 * @param viewer the viewer being initialized
	 * @see fillContextMenu(StructuredViewer, IMenuManager)
	 */
	protected final void hookContextMenu(final StructuredViewer viewer) {
		String targetID = getTargetID();
		final MenuManager menuMgr = new MenuManager(targetID); //$NON-NLS-1$
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
		if (targetID != null) {
			IWorkbenchSite workbenchSite = site.getWorkbenchSite();
			IWorkbenchPartSite ws = null;
			if (workbenchSite instanceof IWorkbenchPartSite)
				ws = (IWorkbenchPartSite)workbenchSite;
			if(ws == null)
				// TODO: Why is this not assigned?
				Utils.findSite(viewer.getControl());
			if (ws == null) 
				ws = Utils.findSite();
			if (ws != null) {
				ws.registerContextMenu(targetID, menuMgr, viewer);
			} else {
				TeamUIPlugin.log(IStatus.ERROR, "Cannot add menu contributions because the site cannot be found: " + targetID, null);
			}
		}
	}
	
	/**
	 * @return
	 */
	private String getTargetID() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Callback that is invoked when a context menu is about to be shown in the
	 * viewer. Subsclasses must implement to contribute menus. Also, menus can
	 * contributed by creating a viewer contribution with a <code>targetID</code> 
	 * that groups sets of actions that are related.
	 * 
	 * @param viewer the viewer in which the context menu is being shown.
	 * @param manager the menu manager to which actions can be added.
	 */
	protected void fillContextMenu(StructuredViewer viewer, IMenuManager manager) {
		manager.add(new Separator(IContextMenuListener.FILE_MENU));
		manager.add(new Separator(IContextMenuListener.EDIT_MENU));
		manager.add(new Separator(IContextMenuListener.NAVIGATE_MENU));
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		if (contextMenuListeners != null) {
			Object[] l= contextMenuListeners.getListeners();
			for (int i= 0; i < l.length; i++) {
				((IContextMenuListener) l[i]).fillContextMenu(manager);
			}
		}
	}
	
	private void initializeActions(StructuredViewerAdvisor advisor) {
		final StructuredViewer viewer = advisor.getViewer();
		if (viewer instanceof AbstractTreeViewer) {
			expandAllAction = new ExpandAllAction((AbstractTreeViewer) viewer);
			Utils.initAction(expandAllAction, "action.expandAll."); //$NON-NLS-1$
		}
		if (site.getType() == ISynchronizePageSite.VIEW) {
			openWithActions = new OpenWithActionGroup(site, participant.getName());
			refactorActions = new RefactorActionGroup(site);
		}
		refreshSelectionAction = new Action() {
			public void run() {
				if(viewer != null && ! viewer.getControl().isDisposed()) {
					IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
					IResource[] resources = Utils.getResources(selection.toArray());
					participant.refresh(resources, participant.getRefreshListeners().createSynchronizeViewListener(participant), Policy.bind("Participant.synchronizing"), site.getWorkbenchSite()); //$NON-NLS-1$
				}
			}
		};
		Utils.initAction(refreshSelectionAction, "action.refreshWithRemote."); //$NON-NLS-1$

	}

	private void initializeListeners(final StructuredViewer viewer) {
		if (site.getType() == ISynchronizePageSite.VIEW) {
			viewer.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					updateStatusLine((IStructuredSelection) event.getSelection());
				}
			});
			viewer.addOpenListener(new IOpenListener() {
				public void open(OpenEvent event) {
					handleOpen();
				}
			});
			viewer.addDoubleClickListener(new IDoubleClickListener() {
				public void doubleClick(DoubleClickEvent event) {
					handleDoubleClick(viewer, event);
				}
			});
		}
	}
	
	private void handleDoubleClick(StructuredViewer viewer, DoubleClickEvent event) {
		IStructuredSelection selection = (IStructuredSelection) event.getSelection();
		DiffNode node = (DiffNode) selection.getFirstElement();
		if (node != null && node instanceof SyncInfoModelElement) {
			SyncInfoModelElement syncNode = (SyncInfoModelElement) node;
			IResource resource = syncNode.getResource();
			if (syncNode != null && resource != null && resource.getType() == IResource.FILE) {
				openWithActions.openInCompareEditor();
				return;
			}
		}
	}
	
	private void handleOpen() {
		openWithActions.openInCompareEditor();
	}
	
	private void updateStatusLine(IStructuredSelection selection) {
		IWorkbenchSite ws = site.getWorkbenchSite();
		if (ws != null && ws instanceof IViewSite) {
			String msg = getStatusLineMessage(selection);
			((IViewSite)ws).getActionBars().getStatusLineManager().setMessage(msg);
		}
	}
	
	private String getStatusLineMessage(IStructuredSelection selection) {
		if (selection.size() == 1) {
			Object first = selection.getFirstElement();
			if (first instanceof SyncInfoModelElement) {
				SyncInfoModelElement node = (SyncInfoModelElement) first;
				IResource resource = node.getResource();
				if (resource == null) {
					return node.getName();
				} else {
					return resource.getFullPath().makeRelative().toString();
				}
			}
		}
		if (selection.size() > 1) {
			return selection.size() + Policy.bind("SynchronizeView.13"); //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
	}
}
