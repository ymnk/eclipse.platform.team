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
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.util.*;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.team.core.subscribers.WorkingSetFilteredSyncInfoCollector;
import org.eclipse.team.internal.core.Assert;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.actions.ExpandAllAction;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.part.IPageSite;


/**
 * A <code>SynchronizeConfiguration</code> object controls various UI aspects of 
 * synchronization viewers.
 * <p>
 * Clients may use this class as is, or subclass to add new state and behavior.
 * </p>
 * @since 3.0
 */
public class SubscriberConfiguration implements ISynchronizeConfiguration, StructuredViewerAdvisor.IContextMenuListener {
	
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

	private IPageSite site;
	
	// Actions
	private ExpandAllAction expandAllAction;
	
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
		advisor.addContextMenuListener(this);
	}
	
	private void initializeCollector() {
		collector = new WorkingSetFilteredSyncInfoCollector(((SubscriberParticipant)getParticipant()).getSubscriberSyncInfoCollector(), participant.getSubscriber().roots());
		collector.reset();
	}

	public void dispose() {
		collector.dispose();
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
	public void init(IPageSite site) {
		this.site = site;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.StructuredViewerAdvisor.IContextMenuListener#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	public void fillContextMenu(IMenuManager manager) {
		// TODO Auto-generated method stub
		// Fill the context menu
		if (expandAllAction != null) {
			manager.insertAfter(NAVIGATE_MENU, expandAllAction);
		}
	}
	
	private void initializeActions(StructuredViewerAdvisor advisor) {
		StructuredViewer viewer = advisor.getViewer();
		if (viewer instanceof AbstractTreeViewer) {
			expandAllAction = new ExpandAllAction((AbstractTreeViewer) viewer);
			Utils.initAction(expandAllAction, "action.expandAll."); //$NON-NLS-1$
		}
	}
}
