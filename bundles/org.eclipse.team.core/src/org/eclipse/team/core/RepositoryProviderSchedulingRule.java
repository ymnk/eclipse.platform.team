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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * This class provides a scheduling rule that can be used to ensure exclusive access
 * to the resources of a project under the control of a repository provider. When a
 * RepositoryProviderSchedulingRule is held, the holding thread may still modify resources
 * within the project. However, resources in other projects are not contained by a
 * RepositoryProviderSchedulingRule.
 */
public class RepositoryProviderSchedulingRule implements ISchedulingRule {
	
	private RepositoryProvider provider;
	
	public RepositoryProviderSchedulingRule(RepositoryProvider provider) {
		this.provider = provider;
	}
	
	public RepositoryProvider getProvider() {
		return provider;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#contains(org.eclipse.core.runtime.jobs.ISchedulingRule)
	 */
	public boolean contains(ISchedulingRule rule) {
		if (rule instanceof RepositoryProviderSchedulingRule) {
			// In general, RepositoryProviderSchedulingRule can contain other RepositoryProviderSchedulingRules
			RepositoryProvider otherProvider = ((RepositoryProviderSchedulingRule)rule).getProvider();
			if (otherProvider == getProvider()) return true;
		} else if (rule instanceof IResource) {
			// Rules can contain resource modifications in the provider's project
			return getProvider().getProject().contains(rule);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#isConflicting(org.eclipse.core.runtime.jobs.ISchedulingRule)
	 */
	public boolean isConflicting(ISchedulingRule rule) {
		if (rule instanceof RepositoryProviderSchedulingRule) {
			// In general, RepositoryProviderSchedulingRules on the same provider conflict
			if (((RepositoryProviderSchedulingRule)rule).getProvider() == getProvider()) {
				// We can use identity for the check since a RepositoryProvider
				// is only created once for each project
				return true;
			}
		}
		return false;
	}

}
