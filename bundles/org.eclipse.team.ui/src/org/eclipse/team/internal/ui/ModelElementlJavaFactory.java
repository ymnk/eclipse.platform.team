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
package org.eclipse.team.internal.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.team.core.traversals.*;

public class ModelElementlJavaFactory implements IAdapterFactory {

	private static class JavaPackageFragmentModelElement implements IModelElement {
		
		private final IPackageFragment fragment;
		private ITraversal[] traversals;

		public JavaPackageFragmentModelElement(IPackageFragment fragment) {
			this.fragment = fragment;
		}
		
		public ITraversal[] getTraversals(IModelContext context, IProgressMonitor monitor) throws CoreException {
			if(traversals == null) {
				traversals = new ITraversal[]{
						new ITraversal() {

							public IProject getProject() {
								return fragment.getResource().getProject();
							}
			
							public IResource[] getResources() {
								return new IResource[]{fragment.getResource()};
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
	
	public Object getAdapter(final Object o, Class adapterType) {
		if (adapterType.isInstance(o)) {
			return o;
		}
		if (adapterType == IModelElement.class && o instanceof IPackageFragment) {
			return new JavaPackageFragmentModelElement((IPackageFragment)o);
		}
		return null;
	}

	public Class[] getAdapterList() {
		return new Class[]{IModelElement.class};
	}
}
