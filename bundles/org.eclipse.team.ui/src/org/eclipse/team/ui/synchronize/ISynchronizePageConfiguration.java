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
	 * Property constant for the page's viewer advisor which is 
	 * an instance of <code>StructuredViewerAdvisor</code>.
	 * The page's viewer can be obtained from the advisor.
	 * This property can be queried by clients but should not be
	 * set.
	 */
	public static final String P_ADVISOR = TeamUIPlugin.ID  + ".P_ADVISOR"; //$NON-NLS-1$

	/**
	 * Property constant for the page's viewer input which is 
	 * an instance of <code>ISynchronizeModelElement</code>.
	 * This property can be queried by clients but should not be
	 * set.
	 */
	public static final String P_MODEL = TeamUIPlugin.ID  + ".P_MODEL"; //$NON-NLS-1$
	
	/**
	 * Property constant for the <code>SyncInfoSet</code> that was used 
	 * to populate the model (P_MODEL). This property can be queried by 
	 * clients but should not be set.
	 */
	public static final String P_SYNC_INFO_SET = TeamUIPlugin.ID  + ".P_SYNC_INFO_SET"; //$NON-NLS-1$
	
	/**
	 * Property constant for the label provider that is used by the 
	 * page's viewer. Clients who wish to add custom labels should 
	 * obtain any previously registered provider using 
	 * <code>getProperty(P_LABEL_PROVIDER)</code>
	 * and wrap the returned provider (which is an instance of
	 * <code>ILabelProvider</code>).
	 * 
	 * TODO: Should this be a decorator?
	 */
	public static final String P_LABEL_PROVIDER = TeamUIPlugin.ID  + ".P_LABEL_PROVIDER"; //$NON-NLS-1$
	
	/**
	 * Property constant that defines the groups in the toolbar 
	 * menu of the page. The value for this
	 * property should be a string array. If this property is
	 * set to <code>null</code>, the <code>DEFAULT_TOOLBAR_MENU</code>
	 * is used.
	 */
	public static final String P_TOOLBAR_MENU = TeamUIPlugin.ID + ".P_TOOLBAR_MENU"; //$NON-NLS-1$

	/**
	 * The configuration property that defines
	 * the groups in the context menu of the page. The value for this
	 * property should be a string array.
	 */
	public static final String P_CONTEXT_MENU = TeamUIPlugin.ID + ".P_CONTEXT_MENU"; //$NON-NLS-1$
	
	/**
	 * The configuration property that defines
	 * the groups in the dropdown view menu of the page. The value for this
	 * property should be a string array.
	 */
	public static final String P_VIEW_MENU = TeamUIPlugin.ID + ".P_VIEW_MENU"; //$NON-NLS-1$
	
	/**
	 * The configuration property that defines the filter id that
	 * determines which object contribution actions appear in the 
	 * context menu for the page. This defaults to the id of the
	 * participant but can be set to another id or <code>null</code>
	 */
	public static final String P_OBJECT_CONTRIBUTION_ID = TeamUIPlugin.ID +  ".P_OBJECT_CONTRIBUTION_ID"; //$NON-NLS-1$
	
	/**
	 * The configuration property that opens the current selection in the
	 * page. The registered <code>IAction</code> is invoked on a single or
	 * double click depending on the open strategy chosen by the user.
	 */
	public static final String P_OPEN_ACTION = TeamUIPlugin.ID + ".P_OPEN_ACTION"; //$NON-NLS-1$

	/**
	 * Property constant for the working set used to filter the visible
	 * elements of the model. The value can be any <code>IWorkingSet</code>
	 * or <code>null</code>;
	 */
	public static final String P_WORKING_SET = TeamUIPlugin.ID + ".P_WORKING_SET"; //$NON-NLS-1$

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
	 * The id of the working set group that determines whether the
	 * working set selection appears in the view dropdown. This
	 * group can only be added as the first group of the view
	 * dropdoen menu.
	 */
	public static final String WORKING_SET_GROUP = "workingset"; //$NON-NLS-1$

	/**
	 * The id of the preferences group that determines whether the preferences
	 * actions appear in the view dropdown.
	 */
	public static final String PREFERENCES_GROUP = "preferences"; //$NON-NLS-1$
	
	/**
	 * The id of the group that determines where workbench object contributions
	 * should appear. This group will only be used if there is an
	 * OBJECT_CONTRIBUTION_ID set in the configuration
	 */
	public static final String OBJECT_CONTRIBUTIONS_GROUP = IWorkbenchActionConstants.MB_ADDITIONS;

	/**
	 * The id of the layout group that determines whether the layout selection
	 * actions appear in the view dropdown or toolbar.
	 */
	public static final String LAYOUT_GROUP = "layout"; //$NON-NLS-1$

	/**
	 * This is the default group ordering used for the context menu of a page.
	 */
	public static final String[] DEFAULT_CONTEXT_MENU = new String[] { FILE_GROUP,  EDIT_GROUP, SYNCHRONIZE_GROUP, NAVIGATE_GROUP, OBJECT_CONTRIBUTIONS_GROUP};

	/**
	 * This is the default group ordering used for the toobar of a page.
	 */
	public static final String[] DEFAULT_TOOLBAR_MENU = new String[] { SYNCHRONIZE_GROUP, NAVIGATE_GROUP, MODE_GROUP,  LAYOUT_GROUP };
	
	/**
	 * This is the default group ordering used for the toobar of a page.
	 */
	public static final String[] DEFAULT_VIEW_MENU = new String[] { WORKING_SET_GROUP, LAYOUT_GROUP, SYNCHRONIZE_GROUP, PREFERENCES_GROUP };

	/**
	 * Return the particpant associated with the page to shich this configuration
	 * is associated.
	 * @return the particpant
	 */
	public abstract ISynchronizeParticipant getParticipant();
	
	/**
	 * Return the site which provieds access to certain workbench
	 * services.
	 * @return the page site
	 */
	public abstract ISynchronizePageSite getSite();

	/**
	 * Add a property change listener to the configuration.
	 * Registered listeners will receive notification when 
	 * any property changes.
	 * @param listener a property change listener
	 */
	public abstract void addPropertyChangeListener(IPropertyChangeListener listener);

	/**
	 * Remove the registered change listener. Removing an unregistered listener
	 * has no effects.
	 * @param listener a property change listener
	 */
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

	/**
	 * Remove a previously registered action contribution. Removing
	 * a contribution that is not registered has no effect.
	 * @param contribution an action contributio
	 */
	public abstract void removeActionContribution(IActionContribution contribution);
}