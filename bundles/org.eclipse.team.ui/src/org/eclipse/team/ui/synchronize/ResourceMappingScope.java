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
package org.eclipse.team.ui.synchronize;

import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberResourceMappingContext;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoFilter;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * A synchronize scope whose roots are a set of resource mappings.
 * This scope cannot be persisted accross workbench invocations.
 * <p>
 * Clients are not expected to subclass this class.
 * </p>
 * @since 3.1
 */
public class ResourceMappingScope extends AbstractSynchronizeScope {
    
    private ResourceMapping[] mappings;
    private IResource[] roots;
    private Subscriber subscriber;

    /**
     * Create the resource mapping scope for the subscriber
     */
    public ResourceMappingScope(Subscriber subscriber, ResourceMapping[] mappings) {
        this.subscriber = subscriber;
        this.mappings = mappings;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.ui.synchronize.ISynchronizeScope#getName()
     */
    public String getName() {
        StringBuffer  buffer = new StringBuffer();
        for (int i = 0; i < mappings.length; i++) {
            ResourceMapping mapping = mappings[i];
            String label = getLabel(mapping);
            if (label != null) {
                if(i > 0) buffer.append(", "); //$NON-NLS-1$
                buffer.append(label);
            }
        }
        return buffer.toString();
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.ui.synchronize.ISynchronizeScope#getRoots()
     */
    public IResource[] getRoots() {
        if (roots == null) {
            try {
                roots = collectRoots();
            } catch (CoreException e) {
                TeamUIPlugin.log(e);
                // Fallback tpo displaying all resources within the projects that contain the mappings
                roots = collectProjects();
            }
        }
        return roots;
    }
    
    private IResource[] collectProjects() {
        Set projects = new HashSet();
        for (int i = 0; i < mappings.length; i++) {
            ResourceMapping mapping = mappings[i];
            projects.addAll(Arrays.asList(mapping.getProjects()));
        }
        return (IResource[]) projects.toArray(new IResource[projects.size()]);
    }

    private IResource[] collectRoots() throws CoreException {
        Set resources = new HashSet();
        for (int i = 0; i < mappings.length; i++) {
            ResourceMapping mapping = mappings[i];
            collectRoots(mapping, resources);
        }
        return (IResource[]) resources.toArray(new IResource[resources.size()]);
    }

    private void collectRoots(ResourceMapping mapping, Set resources) throws CoreException {
        ResourceTraversal[] traversals = mapping.getTraversals(getMappingContext(), null);
        for (int i = 0; i < traversals.length; i++) {
            ResourceTraversal traversal = traversals[i];
            resources.addAll(Arrays.asList(traversal.getResources()));
        }
    }

    private ResourceMappingContext getMappingContext() {
        return new SubscriberResourceMappingContext(subscriber, new SyncInfoFilter() {
            public boolean select(SyncInfo info, IProgressMonitor monitor) {
                return true;
            }
        });
    }

    private IWorkbenchAdapter getWorkbenchAdapter(Object modelObject) {
        if (modelObject instanceof IAdaptable) {
            Object wbadapter = ((IAdaptable) modelObject).getAdapter(IWorkbenchAdapter.class);
            if (wbadapter instanceof IWorkbenchAdapter) {
                return (IWorkbenchAdapter) wbadapter;
            }
        }
        Object wbadapter = Platform.getAdapterManager().getAdapter(modelObject, IWorkbenchAdapter.class);
        if (wbadapter instanceof IWorkbenchAdapter) {
            return (IWorkbenchAdapter) wbadapter;
        }
        return null;
    }
    
    private String getLabel(ResourceMapping mapping) {
        Object modelObject = mapping.getModelObject();
        IWorkbenchAdapter adapter = getWorkbenchAdapter(modelObject);
        if (adapter != null) {
            return adapter.getLabel(mapping.getModelObject());
        }
        return null;
    }

}
