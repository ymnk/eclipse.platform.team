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
package org.eclipse.team.ui.synchronize.subscribers;

import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.IWorkingSet;

/**
 * Interface which defines properties and behavior specific to subscriber
 * participants.
 */
public interface ISubscriberPageConfiguration extends ISynchronizePageConfiguration {
	
	/**
	 * Property constant for the mode used to filter the visible
	 * elements of the model. The value can be one of the mode integer
	 * constants.
	 */
	public static final String P_MODE = TeamUIPlugin.ID  + ".P_SYNCVIEWPAGE_MODE";	 //$NON-NLS-1$
	
	/**
	 * Property constant which indicates which modes are to be available to the user.
	 * The value is to be an integer that combines one or more of the
	 * mode bit values.
	 * Either <code>null</code> or <code>0</code> can be used to indicate that
	 * mode filtering is not supported.
	 */
	public static final String P_SUPPORTED_MODES = TeamUIPlugin.ID  + ".P_SUPPORTED_MODES";	 //$NON-NLS-1$
	
	/**
	 * Modes are direction filters for the view
	 */
	public final static int INCOMING_MODE = 0x1;
	public final static int OUTGOING_MODE = 0x2;
	public final static int BOTH_MODE = 0x4;
	public final static int CONFLICTING_MODE = 0x8;
	public final static int ALL_MODES = INCOMING_MODE | OUTGOING_MODE | CONFLICTING_MODE | BOTH_MODE;

	/**
	 * Return the value of the P_WORKING_SET property of this configuration.
	 * @return the working set property
	 */
	IWorkingSet getWorkingSet();
	
	/**
	 * Set the P_WORKING_SET property of this configuration to the
	 * given working set (which may be <code>null</code>).
	 * @param set the working set or <code>null</code>
	 */
	void setWorkingSet(IWorkingSet set);
	
	/**
	 * Return the value of the P_MODE property of this configuration.
	 * @return the mode property value
	 */
	int getMode();

	/**
	 * Set the P_MODE property of this configuration to the
	 * given mode flag (one of <code>INCOMING_MODE</code>,
	 * <code>OUTGOING_MODE</code>, <code>BOTH_MODE</code>
	 * or <code>CONFLICTING_MODE</code>).
	 * @param mode the mode value
	 */
	void setMode(int mode);
	
	/**
	 * Return the value of the P_SUPPORTED_MODES property of this configuration.
	 * @return the supported modes property value
	 */
	int getSupportedModes();
	
	/**
	 * Set the P_SUPPORTED_MODES property of this configuration to the
	 * ORed combination of one or more mode flags (<code>INCOMING_MODE</code>,
	 * <code>OUTGOING_MODE</code>, <code>BOTH_MODE</code>
	 * and <code>CONFLICTING_MODE</code>).
	 * @param modes the supported modes
	 */
	void setSupportedModes(int modes);
}
