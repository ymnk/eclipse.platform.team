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

import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;

/**
 * A context determined by Team providers and passed to model views
 * in order to display a subset of a model that is involved in a team
 * operation
 * 
 * WARNING: This class is part of a provision API and is subject to change
 * until the release is final.
 * 
 * @since 3.2
 */
public interface ITeamViewerContext {

	public static final String ALL_MAPPINGS = "";
	
	/**
	 * Return the model providers that have mappings
	 * in this context.
	 * @return the model providers that have mappings
	 * in this context
	 */
	public ModelProvider[] getModelProviders();
	
	/**
	 * Return the set of resource mappings associated with 
	 * the given model provider affected by the
	 * team operation that provided the context. If all
	 * resource mappings are desired, pass <code>ALL_MAPPINGS</code>
	 * as the model provider id.
	 * @param modelProviderId the model provider id.
	 * @return a set of resource mappings
	 */
	public ResourceMapping[] getResourceMappings(String modelProviderId);

}
