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

import org.eclipse.ui.PartInitException;


/**
 * Manages synchronization view participants. Clients can programatically add 
 * or remove participants via this manager.
 * <p>
 * Clients are not intended to implement this interface.
 * </p>
 * @see ISynchronizeParticipant
 * @since 3.0 
 */
public interface ISynchronizeManager {	
	/**
	 * Registers the given listener for participant notifications. Has
	 * no effect if an identical listener is already registered.
	 * 
	 * @param listener listener to register
	 */
	public void addSynchronizeParticipantListener(ISynchronizeParticipantListener listener);
	
	/**
	 * Deregisters the given listener for participant notifications. Has
	 * no effect if an identical listener is not already registered.
	 * 
	 * @param listener listener to deregister
	 */
	public void removeSynchronizeParticipantListener(ISynchronizeParticipantListener listener);

	/**
	 * Adds the given participants to the synchronize manager. Has no effect for
	 * equivalent participants are already registered. The participants will be added
	 * to any existing synchronize views.
	 * 
	 * @param consoles consoles to add
	 */
	public void addSynchronizeParticipants(ISynchronizeParticipantReference[] participants);
	
	/**
	 * Removes the given participants from the synchronize manager. If the participants are
	 * being displayed in any synchronize views, the associated pages will be closed.
	 * 
	 * @param consoles consoles to remove
	 */
	public void removeSynchronizeParticipants(ISynchronizeParticipantReference[] participants);
	
	/**
	 * Creates a new participant reference with of the provided type. If the secondayId is specified it
	 * is used as the qualifier for multiple instances of the same type.
	 * <p>
	 * The returned participant reference is a light weight handle describing the participant. The plug-in
	 * defining the participant is not loaded. To instantiate a participant a client must call 
	 * {@link ISynchronizeParticipantReference#createParticipant()} and must call 
	 * {@link ISynchronizeParticipantReference#releaseParticipant()} when finished with the participant.
	 * </p>
	 * @param type the type of the participant
	 * @param secondaryId a unique id for multiple instance support
	 * @return a reference to a participant
	 */
	public ISynchronizeParticipantReference createParticipant(String type, String secondaryId) throws PartInitException;
	
	/**
	 * Returns a collection of synchronize participants registered with the synchronize manager.
	 * 
	 * @return a collection of synchronize participants registered with the synchronize manager.
	 */
	public ISynchronizeParticipantReference[] getSynchronizeParticipants();
	
	/**
	 * Returns the description for the given participant type.
	 * 
	 * @return the description for the given participant type.
	 */
	public ISynchronizeParticipantDescriptor getDescriptor(String id);
	
	/**
	 * Opens the synchronize views in the perspective defined by the user in the team synchronize
	 * perferences.
	 * 
	 * @return the opened synchronize view or <code>null</code> if it can't be opened.
	 */
	public ISynchronizeView showSynchronizeViewInActivePage();
	
	/**
	 * Returns the registered synchronize participants with the given id. It is
	 * possible to have multiple instances of the same participant type.
	 * 
	 * @return the registered synchronize participants with the given id, or 
	 * <code>null</code> if none with that id is not registered.
	 */
	public ISynchronizeParticipantReference get(String id, String secondayId);
}