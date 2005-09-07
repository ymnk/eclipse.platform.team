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
package org.eclipse.team.ui;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.MergeContext;
import org.eclipse.core.internal.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.team.internal.ui.ResourceMappingContentProvider;
import org.eclipse.team.internal.ui.ResourceMappingTree;
import org.eclipse.team.internal.ui.dialogs.ResourceMappingLabelProvider;
import org.eclipse.team.internal.ui.synchronize.ISynchronizeModelProvider;

public class ResourceMappingContentProviderFactory implements IResourceMappingContentProviderFactory {
	
	private static final ResourceMappingTree RESOURCE_MAPPING_TREE = new ResourceMappingTree();
	private static final ResourceMappingLabelProvider RESOURCE_MAPPING_LABEL_PROVIDER = new ResourceMappingLabelProvider();

	public IResourceMappingContentProvider createContentProvider(ResourceMapping[] mappings) {
	    return new ResourceMappingContentProvider(mappings);
	}

	public ISynchronizeModelProvider createSynchronizeModelProvider(ResourceMapping[] mappings, ISynchronizeModelProviderConfiguration configuration) {
	    // TODO Auto-generated method stub
	    return null;
	}

	public ILabelProvider getLabelProvider() {
		return RESOURCE_MAPPING_LABEL_PROVIDER;
	}

	public IResourceMappingTreeItem getResourceMappingTree() {
		return RESOURCE_MAPPING_TREE;
	}
}