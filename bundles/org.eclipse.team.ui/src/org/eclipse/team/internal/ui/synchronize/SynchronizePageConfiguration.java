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
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.util.*;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.IActionBars;

/**
 * Concrete implementation of the ISynchronizePageConfiguration.
 */
public abstract class SynchronizePageConfiguration implements ISynchronizePageConfiguration, IActionContribution {

	private ISynchronizeParticipant participant;
	private ISynchronizePageSite site;
	private ListenerList propertyChangeListeners = new ListenerList();
	private ListenerList actionContributions = new ListenerList();
	private Map properties = new HashMap();
	private boolean actionsInitialized = false;
	
	/**
	 * Create a configuration for creating a page from the given particpant.
	 * @param participant the particpant whose page is being configured
	 */
	public SynchronizePageConfiguration(ISynchronizeParticipant participant) {
		this.participant = participant;
		setProperty(P_OBJECT_CONTRIBUTION_ID, participant.getId());
		setProperty(P_CONTEXT_MENU, DEFAULT_CONTEXT_MENU);
		setProperty(P_TOOLBAR_MENU, DEFAULT_TOOLBAR_MENU);
		setProperty(P_LABEL_PROVIDER, new SynchronizeModelElementLabelProvider());
		addActionContribution(new SynchronizePageActions());
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
		if (actionsInitialized) {
			contribution.initialize(this);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration#removeActionContribution(org.eclipse.team.ui.synchronize.IActionContribution)
	 */
	public void removeActionContribution(IActionContribution contribution) {
		synchronized(actionContributions) {
			actionContributions.remove(contribution);
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IActionContribution#initialize(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration)
	 */
	public void initialize(final ISynchronizePageConfiguration configuration) {
		final Object[] listeners = actionContributions.getListeners();
		for (int i= 0; i < listeners.length; i++) {
			final IActionContribution contribution = (IActionContribution)listeners[i];
			Platform.run(new ISafeRunnable() {
				public void handleException(Throwable exception) {
					// Logged by Platform
				}
				public void run() throws Exception {
					contribution.initialize(configuration);
				}
			});
		}
		actionsInitialized = true;
	}
	
	/**
	 * Callback invoked from the advisor each time the context menu is
	 * about to be shown.
	 * @param manager the context menu manager
	 */
	public void fillContextMenu(final IMenuManager manager) {
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

	/**
	 * Callback invoked from the page to fil the action bars.
	 * @param actionBars the action bars of the view
	 */
	public void setActionBars(final IActionBars actionBars) {
		if (!actionsInitialized) {
			initialize(this);
		}
		final Object[] listeners = actionContributions.getListeners();
		for (int i= 0; i < listeners.length; i++) {
			final IActionContribution contribution = (IActionContribution)listeners[i];
			Platform.run(new ISafeRunnable() {
				public void handleException(Throwable exception) {
					// Logged by Platform
				}
				public void run() throws Exception {
					contribution.setActionBars(actionBars);
				}
			});
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IActionContribution#dispose()
	 */
	public void dispose() {
		final Object[] listeners = actionContributions.getListeners();
		for (int i= 0; i < listeners.length; i++) {
			final IActionContribution contribution = (IActionContribution)listeners[i];
			Platform.run(new ISafeRunnable() {
				public void handleException(Throwable exception) {
					// Logged by Platform
				}
				public void run() throws Exception {
					contribution.dispose();
				}
			});
		}
	}
}
