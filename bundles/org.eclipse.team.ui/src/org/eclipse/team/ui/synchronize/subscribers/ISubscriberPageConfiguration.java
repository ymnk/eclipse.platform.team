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
	public static final String P_SYNCVIEWPAGE_MODE = TeamUIPlugin.ID  + ".P_SYNCVIEWPAGE_MODE";	 //$NON-NLS-1$
	
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

}
