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

import org.eclipse.team.core.sync.ISyncTreeSubscriber;

/**
 * TeamProvider
 */
public class TeamProvider {
	
	static private Map subscribers = new HashMap();

	static public ISyncTreeSubscriber getSubscriber(String id) {
		return (ISyncTreeSubscriber)subscribers.get(id); 
	}
	
	static public ISyncTreeSubscriber[] getSubscribers() {
		return (ISyncTreeSubscriber[])subscribers.values().toArray(
						new ISyncTreeSubscriber[subscribers.size()]);
	}
	
	static public void registerSubscriber(ISyncTreeSubscriber subscriber) {
		subscribers.put(subscriber.getId(), subscriber);
	}
}
