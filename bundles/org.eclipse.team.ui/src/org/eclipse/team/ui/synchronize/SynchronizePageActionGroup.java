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
package org.eclipse.team.ui.synchronize;

import org.eclipse.jface.action.*;
import org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration;
import org.eclipse.ui.actions.ActionGroup;

/**
 * 
 * @since 3.0
 */
public abstract class SynchronizePageActionGroup extends ActionGroup {

	private ISynchronizePageConfiguration configuration;

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IActionContribution#initialize(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration)
	 */
	public void initialize(ISynchronizePageConfiguration configuration) {
		this.configuration = configuration;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IActionContribution#dispose()
	 */
	public void dispose() {
		super.dispose();
		configuration.removeActionContribution(this);
	}
	
	/**
	 * Helper method to find the group of the given id for the page
	 * associated with the configuration of this action group.
	 * The id of the returned group will not match that of the
	 * provided id since the group must be modified to ensure that
	 * groups are unique accross pages.
	 * @param menu the menu
	 * @param groupId the id of the group being searched for
	 * @return the group for the given id or <code>null</code>
	 */
	protected IContributionItem findGroup(IContributionManager menu, String groupId) {
		return menu.find(((SynchronizePageConfiguration)configuration).getGroupId(groupId));
	}
	
	/**
	 * Helper method to add an action to a group in a menu. The action is only
	 * added to the menu if the group exists in the menu. Calling this method 
	 * also has no effect if either the menu or action are <code>null</code>.
	 * @param manager the menu manager
	 * @param groupId the group to append the action to
	 * @param action the action to add
	 */
	protected void appendToGroup(IContributionManager manager, String groupId, IAction action) {
		if (manager == null || action == null) return;
		IContributionItem group = findGroup(manager, groupId);
		if (group != null) {
			manager.appendToGroup(group.getId(), action);
		}
	}
	
	/**
	 * Helper method to add a contribution item to a group in a menu. The item is only
	 * added to the menu if the group exists in the menu. Calling this method 
	 * also has no effect if either the menu or item are <code>null</code>.
	 * @param manager the menu manager
	 * @param groupId the group to append the action to
	 * @param item the item to add
	 */
	protected void appendToGroup(IContributionManager manager, String groupId, IContributionItem item) {
		if (manager == null || item == null) return;
		IContributionItem group = findGroup(manager, groupId);
		if (group != null) {
			manager.appendToGroup(group.getId(), item);
		}
	}
}
