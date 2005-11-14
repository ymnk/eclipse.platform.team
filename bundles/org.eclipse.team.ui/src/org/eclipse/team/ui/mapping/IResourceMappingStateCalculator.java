/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.mapping;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Calculates the synchronization state of the resource mappings associated with 
 * a model provider. The state should be calculated and cached in the 
 * synchronization context for later use by the model merge or
 * model content provider.
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is a guarantee neither that this API will
 * work nor that it will remain the same. Please do not use this API without
 * consulting with the Platform/Team team.
 * </p>
 * 
 * @since 3.2
 */
public interface IResourceMappingStateCalculator {
	
	/**
	 * Calculate the synchronization states of the resource mappings
	 * in the scope of the given context for the model provider
	 * from which this calculator was obtained. The calculated states
	 * should be cached with the context for later use.
	 * @param context the synchronization context
	 * @param monitor a progress monitor
	 * @throws CoreException 
	 */
	void calculateStates(ISynchronizationContext context, IProgressMonitor monitor) throws CoreException;

}
