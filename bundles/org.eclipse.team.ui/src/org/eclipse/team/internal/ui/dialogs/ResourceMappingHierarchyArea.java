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
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.ui.*;

public class ResourceMappingHierarchyArea extends DialogArea {

    private String description;
    private TreeViewer viewer;
    private final CompositeContentProvider contentProvider;
    
    /*
     * TODO: There are some potential problems here
     *   - the input changed method probably should not be propogated to the
     *     sub-providers. Perhaps an additional method is needed (setViewer)?
     *   - this content provider has state that is dependant on what is
     *     displayed in the view. Should a refresh of the viewer clear this state?
     *     I don't think it needs to unless the input changes (which it never does
     *     after the first set).
     */
    private static class CompositeContentProvider implements IResourceMappingContentProvider, ILabelProvider {

        private final Map providers; // Map of IResourceMappingContentProvider -> ILabelProvider
        private final Map providerMap = new HashMap();
        private final ILabelProvider defaultLabelProvider = new ResourceMappingLabelProvider();

        public CompositeContentProvider(Map providers) {
            this.providers = providers;
        }

        public Object getRoot() {
        	IResourceMappingContentProvider provider = getSingleProvider();
        	if (provider != null) {
				Object root = provider.getRoot();
        		providerMap.put(root, provider);
				return root;
        	}
            return this;
        }

		private IResourceMappingContentProvider getSingleProvider() {
			if (providers.size() == 1) {
				return (IResourceMappingContentProvider)providers.keySet().iterator().next();
			}
			return null;
		}

        public Object[] getChildren(Object parentElement) {
        	IResourceMappingContentProvider singleProvider = getSingleProvider();
        	if (singleProvider != null) {
        		return singleProvider.getChildren(parentElement);
        	}
            if (parentElement == this) {
                List result = new ArrayList();
               	for (Iterator iter = providers.keySet().iterator(); iter.hasNext();) {
    				IResourceMappingContentProvider provider = (IResourceMappingContentProvider) iter.next();
                    Object element = provider.getRoot();
                    providerMap.put(element, provider);
                    result.add(element);
                }
                return result.toArray(new Object[result.size()]);
            } else {
                IResourceMappingContentProvider provider = getProvider(parentElement);
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
        	IResourceMappingContentProvider singleProvider = getSingleProvider();
        	if (singleProvider != null) {
        		return singleProvider.getParent(element);
        	}
        	if (element == this)
        		return null;
        	IResourceMappingContentProvider provider = getProvider(element);
        	if (element == provider.getRoot()) {
        		return this;
        	}
            return provider.getParent(element);
        }

		private IResourceMappingContentProvider getProvider(Object element) {
			return (IResourceMappingContentProvider)providerMap.get(element);
		}

        public boolean hasChildren(Object element) {
        	IResourceMappingContentProvider singleProvider = getSingleProvider();
        	if (singleProvider != null) {
        		return singleProvider.hasChildren(element);
        	}
        	if (element != this) {	
	        	IResourceMappingContentProvider provider = getProvider(element);
	        	if (provider != null)
	        		return provider.hasChildren(element);
        	}
        	return getChildren(element).length > 0;
        }

        public Object[] getElements(Object inputElement) {
        	IResourceMappingContentProvider singleProvider = getSingleProvider();
        	if (singleProvider != null) {
        		return singleProvider.getElements(inputElement);
        	}
        	if (inputElement != this) {	
	        	IResourceMappingContentProvider provider = getProvider(inputElement);
	        	if (provider != null)
	        		return provider.getElements(inputElement);
        	}
            return getChildren(inputElement);
        }

        public void dispose() {
        	providerMap.clear();
        	for (Iterator iter = providers.keySet().iterator(); iter.hasNext();) {
				IResourceMappingContentProvider provider = (IResourceMappingContentProvider) iter.next();
				provider.dispose();
				((ILabelProvider)providers.get(provider)).dispose();
            }
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        	providerMap.clear();
           	for (Iterator iter = providers.keySet().iterator(); iter.hasNext();) {
				IResourceMappingContentProvider provider = (IResourceMappingContentProvider) iter.next();
                provider.inputChanged(viewer, oldInput, newInput);
            }
        }

		private ILabelProvider getLabelProvider(Object o) {
        	IResourceMappingContentProvider singleProvider = getSingleProvider();
        	if (singleProvider != null) {
        		return (ILabelProvider)providers.get(singleProvider);
        	}
			if (o != this) {		
				IResourceMappingContentProvider provider = getProvider(o);
				if (provider != null)
					return (ILabelProvider)providers.get(provider);
				// The provider for the object is not known so try the parent
				Object parent = getParent(o);
				if (parent != null)
					return getLabelProvider(parent);
			}
			return defaultLabelProvider;
		}

		public Image getImage(Object element) {
			return getLabelProvider(element).getImage(element);
		}

		public String getText(Object element) {
			return getLabelProvider(element).getText(element);
		}

		public void addListener(ILabelProviderListener listener) {
			defaultLabelProvider.addListener(listener);
			for (Iterator iter = providers.values().iterator(); iter.hasNext();) {
				ILabelProvider lp = (ILabelProvider) iter.next();
				lp.addListener(listener);
			}
		}

		public boolean isLabelProperty(Object element, String property) {
			return getLabelProvider(element).isLabelProperty(element, property);
		}

		public void removeListener(ILabelProviderListener listener) {
			defaultLabelProvider.removeListener(listener);
			for (Iterator iter = providers.values().iterator(); iter.hasNext();) {
				ILabelProvider lp = (ILabelProvider) iter.next();
				lp.removeListener(listener);
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
        Map providers = new HashMap();
        for (Iterator iter = factories.keySet().iterator(); iter.hasNext();) {
            IResourceMappingContentProviderFactory factory = (IResourceMappingContentProviderFactory) iter.next();
            List list = (List)factories.get(factory);
            IResourceMappingContentProvider contentProvider = factory.createContentProvider((ResourceMapping[]) list.toArray(new ResourceMapping[list.size()]));
			providers.put(contentProvider, factory.getLabelProvider());
        }
        CompositeContentProvider provider = new CompositeContentProvider(providers);
        return new ResourceMappingHierarchyArea(provider);
    }
    
    private ResourceMappingHierarchyArea(CompositeContentProvider contentProvider) {
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
        viewer.setLabelProvider(getContentProvider());
        viewer.setInput(getInput());
    }

    private Object getInput() {
        return getContentProvider().getRoot();
    }

    private CompositeContentProvider getContentProvider() {
        return contentProvider;
    }

    public void setDescription(String string) {
        description = string;
    }

}
