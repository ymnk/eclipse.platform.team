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

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.*;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.core.subscribers.SyncInfoSet;
import org.eclipse.team.ui.synchronize.SyncInfoDiffNode;

/**
 * A <code>SyncInfoDiffNode</code> that also contains a java model element.
 */
public class JavaSyncInfoDiffNode extends SyncInfoDiffNode {

	private IJavaElement element;

	/**
	 * @param input
	 * @param resource
	 */
	public JavaSyncInfoDiffNode(SyncInfoSet input, IResource resource, IJavaElement element) {
		super(input, resource);
		this.element = element;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncInfoDiffNode#getChildSyncInfos()
	 */
	public SyncInfo[] getDescendantSyncInfos() {
		if (element == null) {
			return super.getDescendantSyncInfos();
		}
		switch (element.getElementType()) {
			case IJavaElement.COMPILATION_UNIT :
			case IJavaElement.CLASS_FILE:
				return new SyncInfo[0];
			case IJavaElement.JAVA_PROJECT :
				return getVisibleChildren((IJavaProject)element);
			case IJavaElement.PACKAGE_FRAGMENT_ROOT :
				return getVisibleChildren((IPackageFragmentRoot)element);
			case IJavaElement.PACKAGE_FRAGMENT :
				return getVisibleChildren((IPackageFragment)element);
			default :
				return super.getDescendantSyncInfos();
		}
	}

	/*
	 * Return the out-of-sync files contained in the package.
	 */
	private SyncInfo[] getVisibleChildren(IPackageFragment fragment) {
		try {
			// Packages are shallow so only look for out-of-sync files
			// that are direct children of the folder
			IResource resource = fragment.getCorrespondingResource();
			if (resource == null || resource.getType() == IResource.FILE) {
				return new SyncInfo[0];
			}
			IResource[] members = getSyncInfoSet().members(resource);
			List result = new ArrayList();
			for (int i = 0; i < members.length; i++) {
				IResource member = members[i];
				if (member.getType() == IResource.FILE) {
					SyncInfo syncInfo = getSyncInfoSet().getSyncInfo(member);
					if (syncInfo != null) {
						result.add(syncInfo);
					}
				}
			}
			return (SyncInfo[]) result.toArray(new SyncInfo[result.size()]);
		} catch (JavaModelException e) {
			//TODO: log the exception?
			return new SyncInfo[0];
		}
	}

	/*
	 * Return the children of the IPackageFragmentRoot that should be visible.
	 * In other words, packages that contain files that are out-of-sync or
	 * packages mapped to folders taht are out-of-sync
	 */
	private SyncInfo[] getVisibleChildren(IPackageFragmentRoot root) {
		if (root.isArchive()) {
			// Archives do not have resource children
			return new SyncInfo[0];
		}
		IResource resource = root.getResource();
		return getSyncInfoSet().getOutOfSyncDescendants(resource);
	}

	/*
	 * Return all the out-of-sync descendants of the project
	 */
	private SyncInfo[] getVisibleChildren(IJavaProject project) {
		return getSyncInfoSet().getOutOfSyncDescendants(project.getResource());
	}

	/**
	 * Return the java element associated with this diff node.
	 * @return a java element
	 */
	public IJavaElement getElement() {
		return element;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncInfoDiffNode#isResourcePath()
	 */
	public boolean isResourcePath() {
		return element instanceof IPackageFragment;
	}

}
