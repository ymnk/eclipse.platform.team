/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package resourcemapping;

import java.util.*;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.IResourceMapper;
import org.eclipse.core.resources.mapping.ITraversal;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class ResourceMappingWorkbenchAdapter implements IWorkbenchAdapter {
	
	public Object[] getChildren(Object o) {	
		if (o instanceof IResourceMapper) {
			try {
				List resources = new ArrayList();
				ITraversal[] traversals = ((IResourceMapper) o).getTraversals(null, new NullProgressMonitor());
				for (int i = 0; i < traversals.length; i++) {
					ITraversal traversal = traversals[i];
					resources.addAll(Arrays.asList(traversal.getResources()));
				}
				if(resources.size() == 1) {
					return new Object[0];
				} else {
					return (IResource[]) resources.toArray(new IResource[resources.size()]);
				}
			} catch (CoreException e) {
				// break and return no children
			}
		}
		return new Object[0];
	}

	public ImageDescriptor getImageDescriptor(Object o) {
		IWorkbenchAdapter wbadapter = getModelObjectAdapter(o);
		if (wbadapter != null) {
			return wbadapter.getImageDescriptor(((IResourceMapper)o).getModelObject());
		}
		return null; //$NON-NLS-1$
	}

	public String getLabel(Object o) {
		IWorkbenchAdapter wbadapter = getModelObjectAdapter(o);
		if (wbadapter != null) {
			return wbadapter.getLabel(((IResourceMapper)o).getModelObject());
		}
		return ""; //$NON-NLS-1$
	}

	public Object getParent(Object o) {
		return null;
	}

	protected IWorkbenchAdapter getModelObjectAdapter(Object o) {
		if (o instanceof IResourceMapper) {
			Object object = ((IResourceMapper) o).getModelObject();
			if (object instanceof IAdaptable) {
				IWorkbenchAdapter wbadapter = (IWorkbenchAdapter) ((IAdaptable) object).getAdapter(IWorkbenchAdapter.class);
				if (wbadapter != null) {
					return wbadapter;
				}
			}
		}
		return null;
	}
}
