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

import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * The purpose of the WriteResourceSchedulingRule is to prevent concurrent modification
 * of the sync info of a repository provider as well as the resources within the provider's
 * project. When a write resource rule is held, no other
 * RepositoryProviderSchedulingRules will be allowed access until the write resource rule
 * is released. Also, no threads requesting a rule on the provider's project or any of is
 * children will be allwed to run until the write lock is released. 
 */
public class WriteResourcesSchedulingRule extends WriteSyncSchedulingRule {
	public WriteResourcesSchedulingRule(RepositoryProvider provider) {
		super(provider);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#isConflicting(org.eclipse.core.runtime.jobs.ISchedulingRule)
	 */
	public boolean isConflicting(ISchedulingRule rule) {
		if (getProvider().getProject().contains(rule)) {
			// a write resource rule conflicts with modifications on the provider's project
			return true;
		}
		return super.isConflicting(rule);
	}

}
