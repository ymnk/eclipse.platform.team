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
package org.eclipse.team.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.ui.model.*;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.views.navigator.ResourceSorter;

/**
 * Dialog area which displays the resources for a resource mapping
 */
public class ResourceMappingResourceDisplayArea extends DialogArea {

    private ResourceMapping mapping;
    private ResourceMappingContext context;
    private TreeViewer viewer;
    private Label label;
    
    private static IWorkbenchAdapter getWorkbenchAdapter(IAdaptable o) {
        return (IWorkbenchAdapter)o.getAdapter(IWorkbenchAdapter.class);
    }
    
    public class ResourceMappingElement implements IWorkbenchAdapter, IAdaptable {
        private ResourceMapping mapping;
        private ResourceMappingContext context;

        public ResourceMappingElement(ResourceMapping mapping, ResourceMappingContext context) {
            this.mapping = mapping;
            this.context = context;
        }
        
        /* (non-Javadoc)
         * @see org.eclipse.ui.model.IWorkbenchAdapter#getChildren(java.lang.Object)
         */
        public Object[] getChildren(Object o) {
            ResourceTraversal[] traversals = getTraversals();
            List result = new ArrayList();
            for (int i = 0; i < traversals.length; i++) {
                ResourceTraversal traversal = traversals[i];
                IResource[] resources = traversal.getResources();
                for (int j = 0; j < resources.length; j++) {
                    IResource resource = resources[j];               
                    result.add(new ResourceTraversalElement(this, traversal, resource, context));
                }
            }
            return result.toArray(new Object[result.size()]);
        }

        private ResourceTraversal[] getTraversals() {
            try {
                IProgressMonitor monitor = new NullProgressMonitor(); // TODO
                ResourceTraversal[] traversals = mapping.getTraversals(context, monitor);
                return traversals;
            } catch (CoreException e) {
                TeamUIPlugin.log(IStatus.ERROR, "An error occurred fetching the traversals of " + getLabel(mapping), e); //$NON-NLS-1$
                return new ResourceTraversal[0];
            }
        }

        /* (non-Javadoc)
         * @see org.eclipse.ui.model.IWorkbenchAdapter#getImageDescriptor(java.lang.Object)
         */
        public ImageDescriptor getImageDescriptor(Object o) {
            o = mapping;
            IWorkbenchAdapter workbenchAdapter = getWorkbenchAdapter((IAdaptable)o);
            if (workbenchAdapter == null) {
                Object modelObject = mapping.getModelObject();
                if (modelObject instanceof IAdaptable) {
                    workbenchAdapter = getWorkbenchAdapter((IAdaptable)modelObject);
                    o = modelObject;
                }
            }
            if (workbenchAdapter == null) {
                return null;
            }
            return workbenchAdapter.getImageDescriptor(o);
        }

        /* (non-Javadoc)
         * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
         */
        public String getLabel(Object o) {
            o = mapping;
            IWorkbenchAdapter workbenchAdapter = getWorkbenchAdapter((IAdaptable)o);
            if (workbenchAdapter == null) {
                Object modelObject = mapping.getModelObject();
                if (modelObject instanceof IAdaptable) {
                    workbenchAdapter = getWorkbenchAdapter((IAdaptable)modelObject);
                    o = modelObject;
                }
            }
            if (workbenchAdapter == null) {
                return null;
            }
            return workbenchAdapter.getLabel(o);
            
        }

        /* (non-Javadoc)
         * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
         */
        public Object getParent(Object o) {
            return null;
        }

        /* (non-Javadoc)
         * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
         */
        public Object getAdapter(Class adapter) {
            if (adapter == IWorkbenchAdapter.class)
                return this;
            return null;
        }
    }
    
    /**
     * The model element for resources that are obtained from a traversal.
     */
    public class ResourceTraversalElement implements IWorkbenchAdapter, IAdaptable {
        private ResourceTraversal traversal;
        private ResourceMappingContext context;
        private IResource resource;
        private Object parent;
        
        public ResourceTraversalElement(Object parent, ResourceTraversal traversal, IResource resource, ResourceMappingContext context) {
            this.parent = parent;
            this.traversal = traversal;
            this.resource = resource;
            this.context = context;
        }

        /* (non-Javadoc)
         * @see org.eclipse.ui.model.IWorkbenchAdapter#getChildren(java.lang.Object)
         */
        public Object[] getChildren(Object o) {
            if (traversal.getDepth() == IResource.DEPTH_INFINITE) {
                return getChildren(true);
            } else if (traversal.getDepth() == IResource.DEPTH_ONE && isTraversalRoot(resource)) {
                return getChildren(false);
            }
            return new Object[0];
        }

        private Object[] getChildren(boolean includeFolders) {
            try {
                if (resource.getType() != IResource.FILE) {
                    IResource[] members = members(((IContainer)resource));
                    List result = new ArrayList();
                    for (int i = 0; i < members.length; i++) {
                        IResource child = members[i];
                        if (includeFolders || child.getType() == IResource.FILE)
                            result.add(new ResourceTraversalElement(this, traversal, child, context));
                    }
                    return result.toArray(new Object[result.size()]);
                }
            } catch (CoreException e) {
                TeamUIPlugin.log(IStatus.ERROR, "An error occurred fetching the members of " + resource.getFullPath(), e); //$NON-NLS-1$
            }
            return new Object[0];
        }

        private IResource[] members(IContainer container) throws CoreException {
            if (context == null)
                return container.members();
            return context.fetchMembers(container, null); // TODO progress
        }
        
        /* (non-Javadoc)
         * @see org.eclipse.ui.model.IWorkbenchAdapter#getImageDescriptor(java.lang.Object)
         */
        public ImageDescriptor getImageDescriptor(Object object) {
            IWorkbenchAdapter workbenchAdapter = getWorkbenchAdapter(resource);
            if (workbenchAdapter == null)
                return null;
            return workbenchAdapter.getImageDescriptor(resource);
        }

        /* (non-Javadoc)
         * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
         */
        public String getLabel(Object o) {
            if (isTraversalRoot(resource))
                return resource.getFullPath().toString();
            return resource.getName();
        }
        
        private boolean isTraversalRoot(IResource resource) {
            IResource[] resources = traversal.getResources();
            for (int i = 0; i < resources.length; i++) {
                IResource root = resources[i];
                if (root.equals(resource)) {
                    return true;
                }
            }
            return false;
        }

        /* (non-Javadoc)
         * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
         */
        public Object getParent(Object o) {
            return parent;
        }
        
        /* (non-Javadoc)
         * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
         */
        public Object getAdapter(Class adapter) {
            if (adapter == IWorkbenchAdapter.class)
                return this;
            return null;
        }
    }
    
    /**
     * Create a dialog area tht will display the resources contained in the 
     * given mapping
     */
    public ResourceMappingResourceDisplayArea(ResourceMapping mapping) {
        this.mapping = mapping;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.dialogs.DialogArea#createArea(org.eclipse.swt.widgets.Composite)
     */
    public void createArea(Composite parent) {
        Composite composite = createComposite(parent, 1, true);
        
        label = createWrappingLabel(composite, getLabelText(), 1);
        viewer = new TreeViewer(composite);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.heightHint = 100;
        viewer.getControl().setLayoutData(gridData);
        viewer.setContentProvider(new WorkbenchContentProvider());
        viewer.setLabelProvider(new WorkbenchLabelProvider());
        viewer.setSorter(new ResourceSorter(ResourceSorter.NAME)); // TODO: Should not be a resource sorter
        setInput();
        Dialog.applyDialogFont(parent);
    }

    private String getLabelText() {
        return "Preview the resources that make up for the selected element";
    }

    private void setInput() {
        if (viewer != null) {
            Object o = null;
            if (mapping != null)
                o = new ResourceMappingElement(mapping, context);
            viewer.setInput(o);
        }
    }

    public void setMapping(ResourceMapping mapping) {
        this.mapping = mapping;
        setInput();
    }
}
