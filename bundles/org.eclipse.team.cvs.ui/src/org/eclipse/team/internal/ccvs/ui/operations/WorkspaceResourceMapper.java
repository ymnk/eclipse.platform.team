/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.operations;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Utility class used to warp an set of resources in a resource mapper.
 * The resulting mapper will return the workspace root as the model
 * object.
 * 
 * @since 3.1
 */
public final class WorkspaceResourceMapper extends ResourceMapping {
    
    private final IResource[] resources;
    private final int depth;
    IProject[] projects;
    
    /**
     * Convert the provided resources to one or more resource mappers
     * that traverse the elements deeply. The model element of the resource
     * mappers will be the workspace root.
     * @param resources the resources
     * @return a resource mappers that traverses the resources
     */
    public static ResourceMapping[] asResourceMappers(final IResource[] resources, int depth) {
        return new ResourceMapping[] { new WorkspaceResourceMapper(resources, depth) };
    }
    
    public WorkspaceResourceMapper(IResource[] resources, int depth) {
        this.resources = resources;
        this.depth = depth;
    }
    public Object getModelObject() {
        return ResourcesPlugin.getWorkspace().getRoot();
    }
    public IProject[] getProjects() {
        if (projects == null) {
            Set set = new HashSet();
            for (int i = 0; i < resources.length; i++) {
                IResource resource = resources[i];
                set.add(resource.getProject());
            }
            projects = (IProject[]) set.toArray(new IProject[set.size()]);
        }
        return projects;
    }
    public ResourceTraversal[] getTraversals(ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
        return asTraversals(resources, depth, context);
    }
    private ResourceTraversal[] asTraversals(final IResource[] resources, final int depth, ResourceMappingContext context) {
        return new ResourceTraversal[] { new ResourceTraversal(resources, depth)} ;
    }
}