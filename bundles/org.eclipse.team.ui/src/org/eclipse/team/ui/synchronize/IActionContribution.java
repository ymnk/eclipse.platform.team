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

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IActionBars;


public interface IActionContribution {
	
	/**
	 * Initialize the actions of this contribution.
	 * This method will be invoked once before any calls are
	 * made to <code>filleContextMenu</code> or <code>setActionBars</code>
	 * but after the control for the page has been created. As a result
	 * of this, the site of the configuration can be accessed.
	 * @param configuration the configuration for the part to which
	 * the contribution is associated
	 */
	public void initialize(ISynchronizePageConfiguration configuration);
	
	/**
	 * Contribute actions to the context menu of the part to 
	 * which this contribution is associated. This method is
	 * invoked each time the context menu is shown.
	 * @param manager the menu manager for the context menu
	 */
	public void fillContextMenu(IMenuManager manager);
	
	/**
	 * Contribute to the action bars of the part to which this
	 * contribution is associated. 
	 * @param actionBars the actions bars of the part
	 */
	public void setActionBars(IActionBars actionBars);
	
	/**
	 * Dispose of any resources associated with the actions of this
	 * contribution. This method is called when the view that owns
	 * the menuto which the contribution was made is disposed.
	 */
	public void dispose();
}