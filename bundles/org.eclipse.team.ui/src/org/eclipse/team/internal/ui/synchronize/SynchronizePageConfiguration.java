/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.util.*;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.internal.PluginAction;

/**
 * Concrete implementation of the ISynchronizePageConfiguration.
 */
public abstract class SynchronizePageConfiguration implements ISynchronizePageConfiguration {

	ISynchronizeParticipant participant;
	ISynchronizePageSite site;
	ListenerList propertyChangeListeners = new ListenerList();
	ListenerList actionContributions = new ListenerList();
	Map properties = new HashMap();
	
	/**
	 * Create a configuration for creating a page from the given particpant.
	 * @param participant the particpant whose page is being configured
	 */
	public SynchronizePageConfiguration(ISynchronizeParticipant participant) {
		this.participant = participant;
		setProperty(P_OBJECT_CONTRIBUTION_ID, participant.getId());
		setProperty(P_CONTEXT_MENU, DEFAULT_CONTEXT_MENU);
		setProperty(P_TOOLBAR_MENU, DEFAULT_TOOLBAR_MENU);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration#getParticipant()
	 */
	public ISynchronizeParticipant getParticipant() {
		return participant;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration#getSite()
	 */
	public ISynchronizePageSite getSite() {
		return site;
	}
	
	/**
	 * Set the site that is associated with the page that was 
	 * configured using this configuration.
	 * @param site a synchronize page site
	 */
	public void setSite(ISynchronizePageSite site) {
		this.site = site;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration#addPropertyChangeListener(org.eclipse.jface.util.IPropertyChangeListener)
	 */
	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		synchronized(propertyChangeListeners) {
			propertyChangeListeners.add(listener);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration#removePropertyChangeListener(org.eclipse.jface.util.IPropertyChangeListener)
	 */
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		synchronized(propertyChangeListeners) {
			propertyChangeListeners.remove(listener);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration#setProperty(java.lang.String, java.lang.Object)
	 */
	public void setProperty(String key, Object newValue) {
		Object oldValue = properties.get(key);
		properties.put(key, newValue);
		if (oldValue == null || !oldValue.equals(newValue))
			firePropertyChange(key, oldValue, newValue);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration#getProperty(java.lang.String)
	 */
	public Object getProperty(String key) {
		return properties.get(key);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration#addActionContribution(org.eclipse.team.ui.synchronize.IActionContribution)
	 */
	public void addActionContribution(IActionContribution contribution) {
		synchronized(actionContributions) {
			actionContributions.add(contribution);
		}
	}
	
	private void firePropertyChange(String key, Object oldValue, Object newValue) {
		Object[] listeners;
		synchronized(propertyChangeListeners) {
			listeners = propertyChangeListeners.getListeners();
		}
		final PropertyChangeEvent event = new PropertyChangeEvent(this, key, oldValue, newValue);
		for (int i = 0; i < listeners.length; i++) {
			final IPropertyChangeListener listener = (IPropertyChangeListener)listeners[i];
			Platform.run(new ISafeRunnable() {
				public void handleException(Throwable exception) {
					// Error is logged by platform
				}
				public void run() throws Exception {
					listener.propertyChange(event);
				}
			});
		}
	}
	
	public void initialize(StructuredViewerAdvisor advisor) {
		initializeViewer(advisor.getViewer());
	}
	
	private void initializeViewer(StructuredViewer viewer) {
		hookContextMenu(viewer);
	}
	
	/**
	 * Method invoked from <code>initializeViewer(StructuredViewer)</code>
	 * in order to configure the viewer to call <code>fillContextMenu(StructuredViewer, IMenuManager)</code>
	 * when a context menu is being displayed in viewer.
	 * 
	 * @param viewer the viewer being initialized
	 * @see fillContextMenu(StructuredViewer, IMenuManager)
	 */
	private void hookContextMenu(final StructuredViewer viewer) {
		String targetID;
		Object o = getProperty(P_OBJECT_CONTRIBUTION_ID);
		if (o instanceof String) {
			targetID = (String)o;
		} else {
			targetID = null;
		}
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
			if (ws == null) 
				ws = Utils.findSite();
			if (ws != null) {
				ws.registerContextMenu(targetID, menuMgr, viewer);
			} else {
				TeamUIPlugin.log(IStatus.ERROR, "Cannot add menu contributions because the site cannot be found: " + targetID, null); //$NON-NLS-1$
			}
		}
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
	private void fillContextMenu(StructuredViewer viewer, final IMenuManager manager) {
		// Populate the menu with the configured groups
		Object o = getProperty(P_CONTEXT_MENU);
		if (!(o instanceof String[])) {
			o = DEFAULT_CONTEXT_MENU;
		}
		String[] groups = (String[])o;
		for (int i = 0; i < groups.length; i++) {
			String group = groups[i];
			manager.add(new Separator(group));
		}
		// Ask contributions to fill the menu
		final Object[] listeners = actionContributions.getListeners();
		for (int i= 0; i < listeners.length; i++) {
			final IActionContribution contribution = (IActionContribution)listeners[i];
			Platform.run(new ISafeRunnable() {
				public void handleException(Throwable exception) {
					// Logged by Platform
				}
				public void run() throws Exception {
					contribution.fillContextMenu(manager);
				}
			});
		}
	}
}
