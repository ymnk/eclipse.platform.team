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
package org.eclipse.team.ui.mapping;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Interface for delegating a merge to a model provider.
 * 
 * @see org.eclipse.team.ui.mapping.IResourceMappingMerger
 * @since 3.2
 */
public interface IResourceMappingManualMerger {

    /**
     * Delegates a manual merge to a model provider. The merge can
     * be performed asynchronously. The implementor is responsible
     * for disposing of the merge context when the merge is finished.
     * 
     * @param part the part from which the request to merge originated
     * @param mergeContext the merge content that provides access to the
     *            resources that need to be merged
     * @param monitor a progress monitor
     * @throws CoreException
     */
    public void performManualMerge(
            IWorkbenchPart part, IMergeContext mergeContext,
            IProgressMonitor monitor) throws CoreException;
    
}
