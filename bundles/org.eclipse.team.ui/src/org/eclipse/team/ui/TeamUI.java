/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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
import org.eclipse.core.runtime.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.synchronize.ISynchronizeModelProvider;
import org.eclipse.team.internal.ui.synchronize.SynchronizeManager;
import org.eclipse.team.ui.synchronize.ISynchronizeManager;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * TeamUI contains public API for generic UI-based Team functionality.
 * <p>
 * This class is not intended to be subclassed or instantiated by clients
 */
public class TeamUI {

	// manages synchronize participants
	private static ISynchronizeManager synchronizeManager;

    private static class DefaultContentProvider implements IResourceMappingContentProvider {

        /**
         * TODO Should root be a resource mapping?
         */
        private final class RootObject implements IWorkbenchAdapter, IAdaptable {
            private final ResourceMapping[] mappings;

            private RootObject(ResourceMapping[] mappings) {
                super();
                this.mappings = mappings;
            }

            public Object[] getChildren(Object o) {
                return mappings;
            }

            public ImageDescriptor getImageDescriptor(Object object) {
                return null;
            }

            public String getLabel(Object o) {
                return "Other Elements";
            }

            public Object getParent(Object o) {
                return null;
            }

            public Object getAdapter(Class adapter) {
                if (adapter == IWorkbenchAdapter.class)
                    return this;
                return null;
            }
        }

        private final ResourceMapping[] mappings;
        private final Object root;

        public DefaultContentProvider(ResourceMapping[] mappings) {
            this.mappings = mappings;
            root = new RootObject(mappings);
        }

        public Object getRoot() {
            return root;
        }

        public Object[] getChildren(Object parentElement) {
            if (parentElement == root)
                return mappings;
            return new Object[0];
        }

        public Object getParent(Object element) {
            if (element == root)
                return null;
            for (int i = 0; i < mappings.length; i++) {
                ResourceMapping mapping = mappings[i];
                if (element == mapping)
                    return root;
            }
            return null;
        }

        public boolean hasChildren(Object element) {
            if (element == root)
                return true;
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
    }
    
    private static IResourceMappingContentProviderFactory defaultModelContentProviderFactory = new IResourceMappingContentProviderFactory() {
        public IResourceMappingContentProvider createContentProvider(ResourceMapping[] mappings) {
            return new DefaultContentProvider(mappings);
        }

        public ISynchronizeModelProvider createSynchronizeModelProvider(ResourceMapping[] mappings, ISynchronizeModelProviderConfiguration configuration) {
            // TODO Auto-generated method stub
            return null;
        }

        public CompareEditorInput[] createCompareEditorInputs(ResourceMapping[] mappings, MergeContext mergeContext, IProgressMonitor monitor) throws CoreException {
            // TODO Auto-generated method stub
            return null;
        }     
    };

	/**
	 * Property constant indicating the global ignores list has changed. 
	 */
	public static final String GLOBAL_IGNORES_CHANGED = TeamUIPlugin.ID + "global_ignores_changed"; //$NON-NLS-1$
	
    /**
     * Property constant indicating the global file types list has changed.
     * @since 3.1
     */
	public static final String GLOBAL_FILE_TYPES_CHANGED = TeamUIPlugin.ID + "global_file_types_changed"; //$NON-NLS-1$

	/**
	 * Return the synchronize manager.
	 * 
	 * @return the synchronize manager
	 * @since 3.0
	 */
	public static ISynchronizeManager getSynchronizeManager() {
		if (synchronizeManager == null) {
			synchronizeManager = new SynchronizeManager();
		}
		return synchronizeManager;
	}

	/**
	 * Register for changes made to Team properties.
	 * 
	 * @param listener the listener to add
	 */
	public static void addPropertyChangeListener(IPropertyChangeListener listener) {
		TeamUIPlugin.addPropertyChangeListener(listener);
	}

	/**
	 * Remove the listener from Team property change listener list.
	 * 
	 * @param listener the listener to remove
	 */
	public static void removePropertyChangeListener(IPropertyChangeListener listener) {
		TeamUIPlugin.removePropertyChangeListener(listener);
	}
    
    public static IResourceMappingContentProviderFactory getFactory(ResourceMapping mapping) {
        Object o = mapping.getAdapter(IResourceMappingContentProviderFactory.class);
        if (o instanceof IResourceMappingContentProviderFactory) {
            return (IResourceMappingContentProviderFactory) o;
        }
        return defaultModelContentProviderFactory;
    }
}
