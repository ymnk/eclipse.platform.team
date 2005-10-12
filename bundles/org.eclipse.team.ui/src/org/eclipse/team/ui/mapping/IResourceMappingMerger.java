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

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

/**
 * The purpose of this interface is to provide support to clients (e.g.
 * repository providers) for model level auto-merging. It is helpful in the
 * cases where a file may contain multiple model elements or a model element
 * consists of multiple files. It can also be used for cases where there is a
 * one-to-one mapping between model elements and files, although
 * <code>IStreamMerger</code> can also be used in that case.
 * 
 * Clients should determine if a merger is available for a resource mapping
 * using the adaptable mechanism as follows:
 * 
 * <pre>
 *     Object o = mapping.getModelProvider().getAdapter(IResourceMappingMerger.class);
 *     if (o instanceof IResourceMappingMerger.class) {
 *        IResourceMappingMerger merger = (IResourceMappingMerger)o;
 *        ...
 *     }
 * </pre>
 * 
 * Clients should group mappings by model provider when performing merges.
 * This will give the merge context an opportunity to perform the
 * merges optimally.
 * 
 * @see org.eclipse.compare.IStreamMerger
 * @see org.eclipse.team.internal.ui.mapping.IResourceMappingManualMerger
 * @since 3.2
 */
public interface IResourceMappingMerger {

    /**
	 * Attempt to automatically merge the mappings of the merge context(<code>MergeContext#getMappings()</code>).
	 * The merge context provides access to the out-of-sync resources (<code>MergeContext#getSyncInfoTree()</code>)
	 * associated with the mappings to be merged. However, the set of resources
	 * may contain additional resources that are not part of the mappings being
	 * merged. Implementors of this interface should use the mappings to
	 * determine which resources to merge and what additional semantics can be
	 * used to attempt the merge.
	 * <p>
	 * The type of merge to be performed depends on what is returned by the
	 * <code>MergeContext#getType()</code> method. If the type is
	 * <code>MergeContext.TWO_WAY</code> the merge will replace the local
	 * contents with the remote contents, ignoring any local changes. For
	 * <code>THREE_WAY</code>, the base is used to attempt to merge remote
	 * changes with local changes.
	 * <p>
	 * Auto-merges should be performed for as many of the context's resource
	 * mappings as possible. If merging was not possible for one or more
	 * mappings, these mappings should be returned in an
	 * <code>MergeStatus</code> whose code is
	 * <code>MergeStatus.CONFLICTS</code> and which provides access to the
	 * mappings which could not be merged. Note that it is up to the model to
	 * decide whether it wants to break one of the provided resource mappings
	 * into several sub-mappings and attempt auto-merging at that level.
	 * 
	 * @param mappings the set of resource mappings being merged
	 * @param mergeContext a context that provides access to the resources
	 *            involved in the merge. The context must not be
	 *            <code>null</code>.
	 * @param monitor a progress monitor
	 * @return a status indicating the results of the operation. A code of
	 *         <code>MergeStatus.CONFLICTS</code> indicates that some or all
	 *         of the resource mappings could not be merged. The mappings that
	 *         were not merged are available using
	 *         <code>MergeStatus#getConflictingMappings()</code>
	 * @throws CoreException if errors occurred
	 */
    public IStatus merge(IMergeContext mergeContext,
            IProgressMonitor monitor) throws CoreException;

}
