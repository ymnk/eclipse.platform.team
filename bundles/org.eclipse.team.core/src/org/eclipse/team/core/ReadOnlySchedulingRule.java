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
 * The purpose of ReadOnlySchedulingRule is to prevent Write*SchedulingRule threads 
 * from running concurrently with the read only thread. It does not prevent the read only 
 * thread from modifying resources or sync info. Also, it does not prevent other threads from
 * modifying resources. It is the reponsibility of clients to 
 * choose the proper scheduling rule up front given the above mentioned characteristics.
 */
public class ReadOnlySchedulingRule extends RepositoryProviderSchedulingRule {
	public ReadOnlySchedulingRule(RepositoryProvider provider) {
		super(provider);
	}
	public boolean contains(ISchedulingRule rule) {
		if (rule instanceof WriteSyncSchedulingRule) {
			// Read rules cannot contain write rules
			RepositoryProvider otherProvider = ((RepositoryProviderSchedulingRule)rule).getProvider();
			if (otherProvider == getProvider()) return false;
		}
		return super.contains(rule);
	}
	public boolean isConflicting(ISchedulingRule rule) {
		if (rule instanceof ReadOnlySchedulingRule) {
			// Read rules never conflict with other reads
			return false;
		}
		return super.isConflicting(rule);
	}
}
