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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.JavaWorkbenchAdapter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IActionFilter;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class ModelElementlJavaFactory implements IAdapterFactory {

	private static class JavaModelElement extends PlatformObject implements IResourceMapper{
		
		private final IJavaElement javaElement;
		private ITraversal[] traversals;

		public JavaModelElement(IJavaElement fragment) {
			this.javaElement = fragment;
		}
		
		public Object getModelObject() {
			return javaElement;
		}
		public ITraversal[] getTraversals(ITraversalContext context, IProgressMonitor monitor) throws CoreException {
			if(traversals == null) {
				traversals = new ITraversal[]{
						new ITraversal() {

							public IProject[] getProjects() {
								return new IProject[] {javaElement.getResource().getProject()};
							}
			
							public IResource[] getResources() {
								return new IResource[]{javaElement.getResource()};
							}
			
							public int getDepth() {
								return IResource.DEPTH_ONE;
							}
						}
					};
			}
			return traversals;
		}
	}
	
	private static class JavaModelElementWorkbenchAdapter implements IWorkbenchAdapter {
		 private final IJavaElement element;
		 private final IWorkbenchAdapter adapter = new JavaWorkbenchAdapter();

		public JavaModelElementWorkbenchAdapter(IJavaElement element) {
			this.element = element;
		 }

		public Object[] getChildren(Object o) {
			return adapter.getChildren(element);
		}

		public ImageDescriptor getImageDescriptor(Object object) {
			return adapter.getImageDescriptor(element);
		}

		public String getLabel(Object o) {
			return adapter.getLabel(element);
		}

		public Object getParent(Object o) {
			return adapter.getParent(element);
		}	
	}
	
	private static class ResourceModelElement extends PlatformObject implements IResourceMapper {
		
		private final IResource resource;
		private ITraversal[] traversals;

		public ResourceModelElement(IResource resource) {
			this.resource = resource;
		}
		
		public Object getModelObject() {
			return resource;
		}
		
		public ITraversal[] getTraversals(ITraversalContext context, IProgressMonitor monitor) throws CoreException {
			if(traversals == null) {
				traversals = new ITraversal[]{
						new ITraversal() {

							public IProject[] getProjects() {
								return new IProject[] {resource.getProject()};
							}
			
							public IResource[] getResources() {
								return new IResource[]{resource};
							}
			
							public int getDepth() {
								return IResource.DEPTH_INFINITE;
							}
						}
					};
			}
			return traversals;
		}
	}
	
	
	
	public Object getAdapter(final Object o, Class adapterType) {
		if (adapterType.isInstance(o)) {
			return o;
		}
		if (adapterType == IResourceMapper.class && o instanceof IJavaElement) {
			return new JavaModelElement((IJavaElement)o);
		} else if(adapterType == IResourceMapper.class && o instanceof IResource) {
			return new ResourceModelElement((IResource)o);
		} else if(adapterType == IWorkbenchAdapter.class && o instanceof IResourceMapper) {
			return new ResourceMappingWorkbenchAdapter();
		} else if(adapterType == IActionFilter.class && o instanceof IResourceMapper) {
			return new PropertyTester();
		}
		return null;
	}

	public Class[] getAdapterList() {
		return new Class[]{IResourceMapper.class, IWorkbenchAdapter.class, IActionFilter.class};
	}
}
