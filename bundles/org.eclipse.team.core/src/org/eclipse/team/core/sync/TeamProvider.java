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
package org.eclipse.team.core.sync;

import java.util.HashMap;
import java.util.Map;

/**
 * TeamProvider
 */
public class TeamProvider {
	
	static private Map subscribers = new HashMap();

	static public SyncTreeSubscriber getSubscriber(String id) {
		return (SyncTreeSubscriber)subscribers.get(id); 
	}
	
	static public SyncTreeSubscriber[] getSubscribers() {
		return (SyncTreeSubscriber[])subscribers.values().toArray(
						new SyncTreeSubscriber[subscribers.size()]);
	}
	
	static public void registerSubscriber(SyncTreeSubscriber subscriber) {
		subscribers.put(subscriber.getId(), subscriber);
	}
}
