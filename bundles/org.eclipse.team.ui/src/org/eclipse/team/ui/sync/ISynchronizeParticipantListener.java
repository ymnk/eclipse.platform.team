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

/**
 * A synchronize participant listener is notified when participants are added or 
 * removed from the synchronize manager.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @since 3.0
 */
public interface ISynchronizeParticipantListener {
	
	/**
	 * Notification the given consoles have been added to the console
	 * manager.
	 * 
	 * @param consoles added consoles
	 */
	public void participantsAdded(ISynchronizeParticipant[] synchronizeTargets);
	
	/**
	 * Notification the given consoles have been removed from the
	 * console manager.
	 * 
	 * @param consoles removed consoles
	 */
	public void participantsRemoved(ISynchronizeParticipant[] synchronizeTargets);

}
