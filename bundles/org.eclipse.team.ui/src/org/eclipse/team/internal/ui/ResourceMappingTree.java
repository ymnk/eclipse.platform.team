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
package org.eclipse.team.internal.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.internal.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.ui.IResourceMappingTreeItem;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class ResourceMappingTree implements IResourceMappingTreeItem {

	public ResourceMapping[] getParents(ResourceMapping mapping) {
		IWorkbenchAdapter wa = getWorkbenchAdapter(mapping);
		if (wa != null) {
			Object parent = wa.getParent(mapping.getModelObject());
			return asResourceMappings(new Object[] { parent });
		}
		return new ResourceMapping[0];
	}

	public ResourceMapping[] getChildren(ResourceMapping mapping, IProgressMonitor monitor) {
		IWorkbenchAdapter wa = getWorkbenchAdapter(mapping);
		if (wa != null) {
			Object[] objects = wa.getChildren(mapping.getModelObject());
			return asResourceMappings(objects);
		}
		return new ResourceMapping[0];
	}
	
	private ResourceMapping[] asResourceMappings(Object[] objects) {
		List result = new ArrayList();
		for (int i = 0; i < objects.length; i++) {
			Object object = objects[i];
			if (object instanceof IAdaptable) {
				IAdaptable adaptable = (IAdaptable) object;
				Object adapted = adaptable.getAdapter(ResourceMapping.class);
				if (adapted instanceof ResourceMapping) {
					result.add((ResourceMapping) adapted);
				}
			}
		}
		return (ResourceMapping[]) result.toArray(new ResourceMapping[result.size()]);
	}
	
	private IWorkbenchAdapter getWorkbenchAdapter(ResourceMapping mapping) {
        Object modelObject = mapping.getModelObject();
        if (modelObject instanceof IAdaptable) {
        	Object adapter = ((IAdaptable)modelObject).getAdapter(IWorkbenchAdapter.class);
        	if (adapter instanceof IWorkbenchAdapter)
        		return (IWorkbenchAdapter)adapter;
        }
        return null;
	}

}
