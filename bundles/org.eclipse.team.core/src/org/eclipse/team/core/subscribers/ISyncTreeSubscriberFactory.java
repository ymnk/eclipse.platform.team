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
package org.eclipse.team.core.subscribers;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.TeamException;

/**
 * A repository adapter factory is responsible for creating repository adapters.
 * Implementations must provide a public no-arg constructor.
 */
public interface ISyncTreeSubscriberFactory {
	
	/** 
	 * Returns a team subscriber the given id.
	 * @throw TeamException if this method fails. Reasons include:
	 * the location is not valid
	 */
	public SyncTreeSubscriber createSubscriber(QualifiedName id) throws TeamException;
}
