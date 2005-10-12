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
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.team.internal.ui.dialogs.ResourceMappingLabelProvider;
import org.eclipse.team.ui.IResourceMappingContentProvider;

/**
 * Placeholder for Common Navigator extension
 */
public abstract class NavigatorContentExtension {

	private static final ResourceMappingLabelProvider RESOURCE_MAPPING_LABEL_PROVIDER = new ResourceMappingLabelProvider();
	private final ITeamViewerContext context;
	
	public NavigatorContentExtension(ITeamViewerContext context) {
		this.context = context;
	}
	
	/**
	 * Returns a content provider that can be used to display the provided
	 * mappings in an element hierarchy that is consistent with the model. The
	 * resulting hierarchy may contain additional elements as long as they are
	 * for organizational purposes only. Any additional elements must not
	 * include additional resources (i.e. <code>IResource</code>) in the
	 * resource mappings contained in the view.
	 * 
	 * @return a content provider that will arrange the provided mappings into a
	 *         hierarchy.
	 */
	public abstract IResourceMappingContentProvider getContentProvider();

	/**
	 * Return a label provider that can be used to provide labels for any
	 * resource mappings that adapt to this factory and any model objects that
	 * appear in the tree created by the
	 * <code>IResourceMappingContentProvider</code> returned from the
	 * <code>createContentProvider</code>. method.
	 * 
	 * @return a label provider
	 */
	public ILabelProvider getLabelProvider() {
		return RESOURCE_MAPPING_LABEL_PROVIDER;
	}

	public ITeamViewerContext getContext() {
		return context;
	}

	public void dispose() {
		// TODO Auto-generated method stub
	}
}
