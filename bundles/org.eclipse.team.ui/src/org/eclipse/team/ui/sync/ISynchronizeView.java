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
 * A view that displays synchronization participants that are registered with the
 * synchronize manager.
 * <p>
 * Clients are not intended to implement this interface.
 * </p>
 * 
 * @since 3.0
 */
public interface ISynchronizeView extends IViewPart {

	/**
	 * The id for this view
	 */
	public static final String VIEW_ID = "org.eclipse.team.sync.views.SynchronizeView";
	
	/**
	 * Displays the given synchronize participant in the Synchronize View. This
	 * has no effect if this participant is already being displayed.
	 * 
	 * @param participant participant to be displayed, cannot be <code>null</code>
	 */
	public void display(ISynchronizeParticipant participant);
	
	/**
	 * Returns the participant currently being displayed in the Synchronize View
	 * or <code>null</code> if none.
	 *  
	 * @return the participant currently being displayed in the Synchronize View
	 * or <code>null</code> if none
	 */
	public ISynchronizeParticipant getParticipant();
}