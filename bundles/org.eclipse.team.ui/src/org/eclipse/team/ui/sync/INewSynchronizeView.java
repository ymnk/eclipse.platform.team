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
package org.eclipse.team.ui.sync;

import org.eclipse.ui.IViewPart;

/**
 * A view that displays synchronization targets that are registered with the
 * synchronize view manager.
 * <p>
 * Clients are not intended to implement this interface.
 * </p>
 * 
 * @since 3.0
 */
public interface INewSynchronizeView extends IViewPart {

	/**
	 * The id for this view
	 */
	public static final String VIEW_ID = "org.eclipse.team.sync.views.SynchronizeView";
	
	
	public void display(ISynchronizeViewPage page);
	
	public ISynchronizeViewPage getActivePage();
}