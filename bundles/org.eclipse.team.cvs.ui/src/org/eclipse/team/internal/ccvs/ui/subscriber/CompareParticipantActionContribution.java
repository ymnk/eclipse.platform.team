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
package org.eclipse.team.internal.ccvs.ui.subscriber;

import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Toolbar actions for compare participants
 */
public class CompareParticipantActionContribution extends CVSParticipantActionContribution {

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.subscriber.CVSParticipantActionContribution#initialize(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration)
	 */
	public void initialize(ISynchronizePageConfiguration configuration) {
		createRemoveAction(configuration);
		super.initialize(configuration);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.subscriber.CVSParticipantActionContribution#modelChanged(org.eclipse.team.ui.synchronize.ISynchronizeModelElement)
	 */
	protected void modelChanged(ISynchronizeModelElement input) {
		// Nothing to do
	}
}
