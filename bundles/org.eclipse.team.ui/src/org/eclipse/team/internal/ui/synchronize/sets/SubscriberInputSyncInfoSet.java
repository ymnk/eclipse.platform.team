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
package org.eclipse.team.internal.ui.synchronize.sets;

import org.eclipse.team.ui.synchronize.ISyncInfoSet;

public class SubscriberInputSyncInfoSet extends SyncInfoSetDelegator {

	private SubscriberInput input;

	public SubscriberInputSyncInfoSet(SubscriberInput input) {
		this.input = input;
	}

	protected ISyncInfoSet getSyncInfoSet() {
		return input.getSyncInfoSet();
	}

	/**
	 * @return Returns the input.
	 */
	public SubscriberInput getInput() {
		return input;
	}
}
