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

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.ui.IWorkbenchActionConstants;

public interface ISynchronizePageConfiguration {

	/**
	 * The configuration property that 
	 * represents the model being displayed by the page. The value 
	 * is an instance of <code>ISynchronizeModelElement</code>.
	 * This property can be queried by clients but should not be
	 * set.
	 */
	public static final String P_MODEL = TeamUIPlugin.ID  + ".P_MODEL"; //$NON-NLS-1$
	
	/**
	 * The configuration property that defines
	 * the groups in the toolbar menu of the page. The value for this
	 * property should be a string array.
	 */
	public static final String P_TOOLBAR_MENU = TeamUIPlugin.ID + ".P_TOOLBAR_MENU"; //$NON-NLS-1$

	/**
	 * The configuration property that defines
	 * the groups in the context menu of the page. The value for this
	 * property should be a string array.
	 */
	public static final String P_CONTEXT_MENU = TeamUIPlugin.ID + ".P_CONTEXT_MENU"; //$NON-NLS-1$
	
	/**
	 * The configuration property that defines the filter id that
	 * determines which object contribution actions appear in the 
	 * context menu for the page. This defaults to the id of the
	 * participant but can be set to another id or <code>null</code>
	 */
	public static final String P_OBJECT_CONTRIBUTION_ID = TeamUIPlugin.ID +  ".P_OBJECT_CONTRIBUTION_ID"; //$NON-NLS-1$
	
	/**
	 * The id of the synchronize group the determines where the synchronize 
	 * actions appear.
	 */
	public static final String SYNCHRONIZE_GROUP = "synchronize"; //$NON-NLS-1$

	/**
	 * The id of the navigate group that determines where the navigation
	 * actions appear
	 */
	public static final String NAVIGATE_GROUP = "navigate"; //$NON-NLS-1$

	/**
	 * The id of the mode group that determines where the mode selection
	 * actions appear
	 */
	public static final String MODE_GROUP = "modes"; //$NON-NLS-1$

	/**
	 * The id of the file group that determines where the file
	 * actions appear. File actions include the open actions.
	 */
	public static final String FILE_GROUP = "file"; //$NON-NLS-1$

	/**
	 * The id of the edit group that determines where the edit
	 * actions appear (e.g. move and delete).
	 */
	public static final String EDIT_GROUP = "edit"; //$NON-NLS-1$

	/**
	 * The id of the group that determines where workbench object contributions
	 * should appear. This group will only be used if there is an
	 * OBJECT_CONTRIBUTION_ID set in the configuration
	 */
	public static final String OBJECT_CONTRIBUTIONS_GROUP = IWorkbenchActionConstants.MB_ADDITIONS;

	/**
	 * This is the default group ordering used for the context menu of a page.
	 */
	public static final String[] DEFAULT_CONTEXT_MENU = new String[] { FILE_GROUP,  EDIT_GROUP, SYNCHRONIZE_GROUP, NAVIGATE_GROUP, OBJECT_CONTRIBUTIONS_GROUP};

	/**
	 * This is the default group ordering used for the toobar of a page.
	 */
	public static final String[] DEFAULT_TOOLBAR_MENU = new String[] { SYNCHRONIZE_GROUP,  NAVIGATE_GROUP, MODE_GROUP };

	public abstract ISynchronizeParticipant getParticipant();
	
	public abstract ISynchronizePageSite getSite();

	public abstract void addPropertyChangeListener(IPropertyChangeListener listener);

	public abstract void removePropertyChangeListener(IPropertyChangeListener listener);

	/**
	 * Sets the property with the given name.
	 * If the new value differs from the old a <code>PropertyChangeEvent</code>
	 * is sent to registered listeners.
	 *
	 * @param propertyName the name of the property to set
	 * @param value the new value of the property
	 */
	public abstract void setProperty(String key, Object newValue);

	/**
	 * Returns the property with the given name, or <code>null</code>
	 * if no such property exists.
	 *
	 * @param propertyName the name of the property to retrieve
	 * @return the property with the given name, or <code>null</code> if not found
	 */
	public abstract Object getProperty(String key);

	/**
	 * Register the action contribution with the configuration. The
	 * registered action contributions will have the opertunity to add
	 * actions to the action bars and context menu of the synchronize
	 * page created using the configuration.
	 * @param contribution an action contribution
	 */
	public abstract void addActionContribution(IActionContribution contribution);
}