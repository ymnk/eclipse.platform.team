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

import org.eclipse.team.ui.synchronize.views.ILogicalView;
import org.eclipse.ui.IWorkbenchPage;

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
	public void addSynchronizeParticipants(ISynchronizeParticipant[] consoles);
	
	/**
	 * Removes the given participants from the synchronize manager. If the participants are
	 * being displayed in any synchronize views, the associated pages will be closed.
	 * 
	 * @param consoles consoles to remove
	 */
	public void removeSynchronizeParticipants(ISynchronizeParticipant[] consoles);
	
	/**
	 * Returns a collection of synchronize participants registered with the synchronize manager.
	 * 
	 * @return a collection of synchronize participants registered with the synchronize manager.
	 */
	public ISynchronizeParticipant[] getSynchronizeParticipants();
	
	/**
	 * Opens the synchronize view in the given page. Has no effect if the view is 
	 * already open in that page. 
	 * 
	 * @return the opened synchronize view 
	 */
	public ISynchronizeView showSynchronizeViewInActivePage(IWorkbenchPage page);
	
	/**
	 * Returns the registered synchronize participants with the given id. It is
	 * possible to have multiple instances of the same participant type.
	 * 
	 * @return the registered synchronize participants with the given id, or 
	 * <code>null</code> if none with that id is not registered.
	 */
	public ISynchronizeParticipant[] find(String id);
	
	/**
	 * Returns the participant descriptor for the given participant id or 
	 * <code>null</code> if a descriptor is not found for that id.
	 * 
	 * @return the participant descriptor for the given participant id or 
	 * <code>null</code> if a descriptor is not found for that id.
	 */
	public ISynchronizeParticipantDescriptor getParticipantDescriptor(String id);
	
	/**
	 * Return all the logical views that have been registered with Team.
	 * The providers associated with the views will not be instantiated until
	 * they are accessed so this list can be retrieved without fear of loading
	 * client plugins.
	 * @return the regisitered logical views
	 */
	public ILogicalView[] getLogicalViews();
	
	/**
	 * Return the registered view with the given id or <code>null</code> if no view
	 * exists for the given id. The provider associated with the view will not be instantiated until
	 * it is accessed so the logical view can be retrieved without fear of loading
	 * client plugins.
	 * @param id the id of the logical view
	 * @return the logical view with the given id or <code>null</code>
	 */
	public ILogicalView getLogicalView(String id);
}