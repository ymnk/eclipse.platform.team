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
package org.eclipse.jdt.ui.team;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.*;
import org.eclipse.team.core.subscribers.SyncInfoSet;
import org.eclipse.team.internal.ui.synchronize.views.SyncSetTreeContentProvider;

/**
 * Extend the SyncSetTreeContentProvider to show a Java model elements in the view.
 * 
 * TODO: Refresh behavior needs to be added
 */
public class JavaSyncInfoSetContentProvider extends SyncSetTreeContentProvider {

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.views.SyncSetContentProvider#getModelObject(org.eclipse.core.resources.IResource)
	 */
	public Object getModelObject(IResource resource) {
		IJavaElement element = JavaCore.create(resource);
		if (element != null) {
			return new JavaSyncInfoDiffNode(getSyncInfoSet(), resource, element);
		}
		return super.getModelObject(resource);
	}

	private Object getModelObject(IJavaElement element) {
		return new JavaSyncInfoDiffNode(getSyncInfoSet(), element.getResource(), element);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object object) {
		IJavaElement element = getJavaElement(object);
		if (element == null) {
			return getChildrenOfNonJavaElement(object);
		}
		return internalGetChildren(element);
	}

	private Object[] internalGetChildren(IJavaElement element) {
		switch (element.getElementType()) {
		case IJavaElement.JAVA_PROJECT :
			return internalGetChildren((IJavaProject)element);
		case IJavaElement.PACKAGE_FRAGMENT_ROOT :
			return internalGetChildren((IPackageFragmentRoot)element);
		case IJavaElement.PACKAGE_FRAGMENT :
			return internalGetChildren((IPackageFragment)element);
		default :
			return new Object[0];
		}
	}
	
	private IJavaElement getJavaElement(Object object) {
		if (object instanceof JavaSyncInfoDiffNode) {
			return ((JavaSyncInfoDiffNode)object).getElement();
		}
		return null;
	}

	/*
	 * Return the children of a non-java element
	 */
	private Object[] getChildrenOfNonJavaElement(Object object) {
		return super.getChildren(object);
	}

	/*
	 * Return the children of the IPackageFragmentRoot that should be visible.
	 * In other words, packages that contain files that are out-of-sync or
	 * packages mapped to folders taht are out-of-sync
	 */
	private Object[] internalGetChildren(IPackageFragmentRoot root) {
		if (root.isArchive()) {
			// Archives do not have resource children
			return new Object[0];
		}
		try {
			SyncInfoSet syncInfoSet = getSyncInfoSet();
			List result = new ArrayList();
			IJavaElement[] javaChildren = root.getChildren();
			for (int i = 0; i < javaChildren.length; i++) {
				IJavaElement element = javaChildren[i];
				if (syncInfoSet.getSyncInfo(element.getResource()) != null 
						|| internalGetChildren(element).length > 0) {
					result.add(getModelObject(element));
				}
			}
			Object[] nonJavaChildren = root.getNonJavaResources();
			for (int i = 0; i < nonJavaChildren.length; i++) {
				Object object = nonJavaChildren[i];
				if (object instanceof IResource) {
					IResource resource = (IResource)object;
					if (syncInfoSet.getSyncInfo(resource) != null || syncInfoSet.hasMembers(resource)) {
						result.add(getModelObject(resource));
					}
				}
			}
			return result.toArray(new Object[result.size()]);
		} catch (JavaModelException e) {
			//TODO: log the exception?
			return new Object[0];
		}
	}
	
	private Object[] internalGetChildren(IPackageFragment fragment) {
		try {
			// Packages are shallow so only look for out-of-sync files
			// that are direct children of the folder
			IResource resource = fragment.getCorrespondingResource();
			if (resource == null || resource.getType() == IResource.FILE) {
				return new Object[0];
			}
			SyncInfoSet syncInfoSet = getSyncInfoSet();
			IResource[] members = syncInfoSet.members(resource);
			List result = new ArrayList();
			for (int i = 0; i < members.length; i++) {
				IResource member = members[i];
				if (member.getType() == IResource.FILE) {
					if (syncInfoSet.getSyncInfo(member) != null) {
						result.add(getModelObject(member));
					}
				}
			}
			return result.toArray(new Object[result.size()]);
		} catch (JavaModelException e) {
			//TODO: log the exception?
			return new Object[0];
		}
	}
	
	private Object[] internalGetChildren(IJavaProject project) {
		try {
			SyncInfoSet syncInfoSet = getSyncInfoSet();
			List result = new ArrayList();
			IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
			// filter out package fragments that correspond to projects and
			// replace them with the package fragments directly
			for (int i= 0; i < roots.length; i++) {
				IPackageFragmentRoot root= roots[i];
				if (isProjectPackageFragmentRoot(root)) {
					IJavaElement[] children= root.getChildren();
					for (int k= 0; k < children.length; k++) 
						result.add(getModelObject(children[k]));
				} else if (!root.isArchive() && syncInfoSet.getSyncInfo(root.getResource()) != null 
						|| internalGetChildren(root).length > 0) {
					result.add(getModelObject(root));
				} 
			}
			
			Object[] nonJavaChildren = project.getNonJavaResources();
			for (int i = 0; i < nonJavaChildren.length; i++) {
				Object object = nonJavaChildren[i];
				if (object instanceof IResource) {
					IResource resource = (IResource)object;
					if (syncInfoSet.getSyncInfo(resource) != null || syncInfoSet.hasMembers(resource)) {
						result.add(getModelObject(resource));
					}
				}
			}
			return result.toArray(new Object[result.size()]);
		} catch (JavaModelException e) {
			//TODO: log the exception?
			return new Object[0];
		}
	}
	
	private boolean isProjectPackageFragmentRoot(IPackageFragmentRoot root) {
		IResource resource= root.getResource();
		return (resource instanceof IProject);
	}

}
