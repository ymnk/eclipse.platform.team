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
package org.eclipse.team.core;


/**
 * The purpose of the WriteSyncSchedulingRule is to prevent concurrent modification
 * of the sync info of a repository provider. When a write sync rule is held, no other
 * RepositoryProviderSchedulingRules will be allowed access until the write sync rule
 * is released. However, the write sync rule does not prevent the thread that holds the lock
 * from modifying the workspace. This is important since the sync info may be contained in 
 * resources or in the ISynchronizer which noth require write access to the workspace tree. 
 */
public class WriteSyncSchedulingRule extends RepositoryProviderSchedulingRule {
	public WriteSyncSchedulingRule(RepositoryProvider provider) {
		super(provider);
	}
}
