/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.mapping;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.CommonActionProviderConfig;

/**
 * An action group that can be used by models to contribute actions
 * to a team synchronization viewer.
 * <p>
 * This class is not intended to be subclasses by clients
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is a guarantee neither that this API will
 * work nor that it will remain the same. Please do not use this API without
 * consulting with the Platform/Team team.
 * </p>
 * 
 * @since 3.2
 */
public class SynchronizationActionGroup extends CommonActionProvider {
	
	/**
	 * Action id constant for the merge action.
	 * @see #registerHandler(String, IHandler)
	 */
	public static final String MERGE_ACTION_ID = "org.eclipse.team.ui.mergeAction"; //$NON-NLS-1$
	
	/**
	 * Action id constant for the merge action.
	 * @see #registerHandler(String, IHandler)
	 */
	public static final String OVERWRITE_ACTION_ID = "org.eclipse.team.ui.overwriteAction"; //$NON-NLS-1$
	
	/**
	 * Action id constant for the mark-as-merge action.
	 * @see #registerHandler(String, IHandler)
	 */
	public static final String MARK_AS_MERGE_ACTION_ID = "org.eclipse.team.ui.markAsMergeAction"; //$NON-NLS-1$

	private CommonActionProviderConfig config;
	private Map handlers = new HashMap();

	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.CommonActionProvider#init(org.eclipse.ui.navigator.CommonActionProviderConfig)
	 */
	public void init(CommonActionProviderConfig aConfig) {
		config = aConfig;
	}

	/**
	 * Return the configuration for the common viewer.
	 * @return the configuration from the common viewer
	 */
	public final CommonActionProviderConfig getCommonConfiguration() {
		return config;
	}
	
	/**
	 * Return the configuration from the synchronize page that contains
	 * the common viewer.
	 * @return the configuration from the synchronize page that contains
	 * the common viewer
	 */
	public final ISynchronizePageConfiguration getSynchronizePageConfiguration() {
		return (ISynchronizePageConfiguration)config.getExtensionStateModel().getProperty(TeamUI.SYNCHRONIZATION_PAGE_CONFIGURATION);
	}
	
	/**
	 * Register the handler as the handler for the given action id when
	 * a merge action is performed on elements that match this groups 
	 * enablement.
	 * @param actionId the id of the merge action
	 * @param handler the handler for elements of the model that provided this group
	 */
	protected void registerHandler(String actionId, IHandler handler) {
		handlers.put(actionId, handler);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.ActionGroup#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		// TODO: Register the handlers
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.ActionGroup#fillActionBars(org.eclipse.ui.IActionBars)
	 */
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		// TODO: Register the handlers
	}
	

}
