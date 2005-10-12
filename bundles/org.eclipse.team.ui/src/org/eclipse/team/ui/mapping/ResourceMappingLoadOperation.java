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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.internal.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchPart;

/**
 * The steps of an load (replace with version or branch) operation are:
 * <ol>
 * <li>Obtain the selection to be operated on.
 * <li>Determine the projection of the selection onto resources
 * using resource mappings and traversals.
 * 		<ul>
 * 		<li>this will require traversal of remote only
 * 		</ul>
 * <li>Ensure that all affected mappings are known
 *      <ul>
 * 		<li>additional mappings may be included due to resource project
 *      (i.e. many-to-one case).
 *      <li>notify users of additional mappings that will be affected
 *      <li>this list must include locally changed model elements whose
 *      changes will be lost including any that were deleted.
 *      <li>this list could include changed remote elements that 
 *      will be received including additions
 * 		</ul>
 * <li>Perform the replace at the resource level
 * </ol>
 */
public class ResourceMappingLoadOperation extends ResourceMappingOperation {

	protected ResourceMappingLoadOperation(IWorkbenchPart part, IResourceMappingOperationInput input) {
		super(part, input);
	}

	protected void execute(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ResourceMappingOperation#getDefaultMaualMerger()
	 */
	protected IResourceMappingManualMerger getDefaultManualMerger() {
		// Loading never requires a manual merge
		throw new UnsupportedOperationException();
	}

}
