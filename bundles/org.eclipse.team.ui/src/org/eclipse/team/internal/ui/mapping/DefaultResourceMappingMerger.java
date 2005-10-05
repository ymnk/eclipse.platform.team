/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.ui.mapping.IMergeContext;
import org.eclipse.team.ui.mapping.IResourceMappingMerger;
import org.eclipse.team.ui.mapping.MergeStatus;
import org.eclipse.team.ui.synchronize.ResourceMappingScope;

/**
 * A default merger that delegates the merge to the merge context.
 */
public class DefaultResourceMappingMerger implements IResourceMappingMerger {
	
	public IStatus merge(IMergeContext mergeContext, IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(null, 100);
			IStatus status = mergeContext.merge(mergeContext.getSyncInfoTree(), Policy.subMonitorFor(monitor, 75));
			return covertFilesToMappings(status, mergeContext);
		} finally {
			monitor.done();
		}
	}

	private IStatus covertFilesToMappings(IStatus status, IMergeContext mergeContext) {
		if (status.getCode() == MergeStatus.CONFLICTS) {
			// In general, we can't say which mapping failed so return them all
			// TODO: Should not assume that we are dealing with a resource mapping scope
			return new MergeStatus(status.getPlugin(), status.getMessage(), ((ResourceMappingScope)mergeContext.getScope()).getResourceMappings());
		}
		return status;
	}

}
