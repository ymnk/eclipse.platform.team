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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.mapping.IResourceMappingScope;
import org.eclipse.team.ui.mapping.ISynchronizationContext;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.IExtensionStateModel;
import org.eclipse.ui.navigator.internal.extensions.ICommonContentProvider;

/**
 * This content provider displays the mappings as a flat list 
 * of elements.
 * <p>
 * There are three use-cases we need to consider. The first is when there
 * are resource level mappings to be displayed. The second is when there
 * are mappings from a model provider that does not have a content provider
 * registered. The third is for the case where a resource mapping does not
 * have a model provider registered (this may be considered an error case).
 *
 */
public class ResourceMappingContentProvider implements ICommonContentProvider {

	private ISynchronizationContext context;
	private IResourceMappingScope scope;
	
	public ResourceMappingContentProvider() {
    	// Nothing to do
    }

    public ISynchronizationContext getContext() {
		return context;
	}

	public IResourceMappingScope getScope() {
		return scope;
	}

	public Object[] getChildren(Object parentElement) {
    	if (parentElement instanceof ModelProvider) {
			ModelProvider provider = (ModelProvider) parentElement;
        	List children = new ArrayList();
        	ResourceTraversal[] traversals = getTraversals(provider);
        	for (int i = 0; i < traversals.length; i++) {
				ResourceTraversal traversal = traversals[i];
				IResource[] resources = traversal.getResources();
				for (int j = 0; j < resources.length; j++) {
					IResource resource = resources[j];
					children.add(new ResourceAndDepth(provider, resource, traversal.getDepth()));
				}
			}
            return children.toArray(new Object[children.size()]);
        }
        return new Object[0];
    }

	private ResourceTraversal[] getTraversals(ModelProvider provider) {
		List result = new ArrayList();
		ResourceMapping[] mappings = scope.getMappings(provider.getDescriptor().getId());
		for (int i = 0; i < mappings.length; i++) {
			ResourceMapping mapping = mappings[i];
			ResourceTraversal[] traversals = scope.getTraversals(mapping);
			for (int j = 0; j < traversals.length; j++) {
				ResourceTraversal traversal = traversals[j];
				result.add(traversal);
			}
		}
		return ResourceMappingScope.combineTraversals((ResourceTraversal[]) result.toArray(new ResourceTraversal[result.size()]));
	}

    public Object getParent(Object element) {
        if (element instanceof ResourceAndDepth) {
			ResourceAndDepth rad = (ResourceAndDepth) element;
			return rad.getParent();
		}
        return null;
    }

    public boolean hasChildren(Object element) {
        if (element instanceof ModelProvider)
            return true;
        if (element instanceof ResourceAndDepth) {
			ResourceAndDepth rad = (ResourceAndDepth) element;
			return rad.getResource().getType() != IResource.FILE && rad.getDepth() != IResource.DEPTH_ZERO;
		}
        return false;
    }

    public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
    }

    public void dispose() {
        // Nothing to do
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        // Nothing to do
    }

	public void init(IResourceMappingScope input, ISynchronizationContext context) {
		this.scope = input;
		this.context = context;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.internal.extensions.ICommonContentProvider#init(org.eclipse.ui.navigator.IExtensionStateModel, org.eclipse.ui.IMemento)
	 */
	public void init(IExtensionStateModel aStateModel, IMemento aMemento) {
		init((IResourceMappingScope)aStateModel.getProperty(TeamUI.RESOURCE_MAPPING_SCOPE), (ISynchronizationContext)aStateModel.getProperty(TeamUI.SYNCHRONIZATION_CONTEXT));	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.IMementoAware#restoreState(org.eclipse.ui.IMemento)
	 */
	public void restoreState(IMemento aMemento) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.IMementoAware#saveState(org.eclipse.ui.IMemento)
	 */
	public void saveState(IMemento aMemento) {
		// TODO Auto-generated method stub
		
	}
}