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
package org.eclipse.team.internal.ui.dialogs;

import java.util.*;

import org.eclipse.core.internal.resources.mapping.ResourceMapping;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.ui.*;

public class ResourceMappingHierarchyArea extends DialogArea {

    private String description;
    private TreeViewer viewer;
    private final IResourceMappingContentProvider contentProvider;
    
    private static class CompositeContentProvider implements IResourceMappingContentProvider {

        private final IResourceMappingContentProvider[] providers;
        private final Map providerMap = new HashMap();

        public CompositeContentProvider(IResourceMappingContentProvider[] providers) {
            this.providers = providers;
        }

        public Object getRoot() {
            return this;
        }

        public Object[] getChildren(Object parentElement) {
            if (parentElement == this) {
                List result = new ArrayList();
                for (int i = 0; i < providers.length; i++) {
                    IResourceMappingContentProvider provider = providers[i];
                    Object element = provider.getRoot();
                    providerMap.put(element, provider);
                    result.add(element);
                }
                return result.toArray(new Object[result.size()]);
            } else {
                IResourceMappingContentProvider provider = (IResourceMappingContentProvider)providerMap.get(parentElement);
                if (provider != null) {
                    Object[] elements = provider.getChildren(parentElement);
                    for (int i = 0; i < elements.length; i++) {
                        Object element = elements[i];
                        providerMap.put(element, provider);
                    }
                    return elements;
                }
            }
            return new Object[0];
        }

        public Object getParent(Object element) {
            // TODO
            return null;
        }

        public boolean hasChildren(Object element) {
            return getChildren(element).length > 0;
        }

        public Object[] getElements(Object inputElement) {
            return getChildren(inputElement);
        }

        public void dispose() {
            for (int i = 0; i < providers.length; i++) {
                IResourceMappingContentProvider provider = providers[i];
                provider.dispose();
            }
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            for (int i = 0; i < providers.length; i++) {
                IResourceMappingContentProvider provider = providers[i];
                provider.inputChanged(viewer, oldInput, newInput);
            }
        }
        
    }
    
    public static ResourceMappingHierarchyArea create(ResourceMapping[] mappings) {
        Map factories = new HashMap();
        for (int i = 0; i < mappings.length; i++) {
            ResourceMapping mapping = mappings[i];
            IResourceMappingContentProviderFactory factory = TeamUI.getFactory(mapping);
            List list = (List)factories.get(factory);
            if (list == null) {
                list = new ArrayList();
                factories.put(factory, list);
            }
            list.add(mapping);
        }
        List providers = new ArrayList();
        for (Iterator iter = factories.keySet().iterator(); iter.hasNext();) {
            IResourceMappingContentProviderFactory factory = (IResourceMappingContentProviderFactory) iter.next();
            List list = (List)factories.get(factory);
            providers.add(factory.createContentProvider((ResourceMapping[]) list.toArray(new ResourceMapping[list.size()])));
        }
        IResourceMappingContentProvider provider;
        if (providers.size() == 1) {
            provider = (IResourceMappingContentProvider)providers.get(0);
        } else {
            provider = new CompositeContentProvider((IResourceMappingContentProvider[]) providers.toArray(new IResourceMappingContentProvider[providers.size()]));
        }
        return new ResourceMappingHierarchyArea(provider);
    }
    
    private ResourceMappingHierarchyArea(IResourceMappingContentProvider contentProvider) {
        this.contentProvider = contentProvider;
    }
    
    public void createArea(Composite parent) {
        Composite composite = createComposite(parent, 1, true);
        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.verticalSpacing = 0;
        layout.horizontalSpacing = 0;
        composite.setLayout(layout);
        
        if (description != null)
            createWrappingLabel(composite, description, 1);
        
        viewer = new TreeViewer(composite);
        GridData data = new GridData(GridData.FILL_BOTH);
        data.heightHint = 100;
        data.widthHint = 300;
        viewer.getControl().setLayoutData(data);
        viewer.setContentProvider(getContentProvider());
        viewer.setLabelProvider(new ResourceMappingLabelProvider());
        viewer.setInput(getInput());
    }

    private Object getInput() {
        return getContentProvider().getRoot();
    }

    private IResourceMappingContentProvider getContentProvider() {
        return contentProvider;
    }

    public void setDescription(String string) {
        description = string;
    }

}
