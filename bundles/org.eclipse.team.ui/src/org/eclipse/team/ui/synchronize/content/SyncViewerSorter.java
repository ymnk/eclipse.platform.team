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
package org.eclipse.team.ui.synchronize.content;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.team.ui.synchronize.SyncInfoDiffNode;
import org.eclipse.ui.views.navigator.ResourceSorter;

/**
 * This class sorts <code>SyncInfoDiffNode</code> instances.
 * It is not thread safe so it should not be reused between views.
 */
public class SyncViewerSorter extends ResourceSorter {
			
	private boolean compareFullPaths = false;

	public SyncViewerSorter(int criteria) {
		super(criteria);
	}

	/* (non-Javadoc)
	 * Method declared on ViewerSorter.
	 */
	public int compare(Viewer viewer, Object o1, Object o2) {
		if(o1 instanceof SyncInfoDiffNode || o2 instanceof SyncInfoDiffNode) {
			compareFullPaths = isResourcePath(o1) || isResourcePath(o2);
		}
		IResource resource1 = getResource(o1);
		IResource resource2 = getResource(o2);
		int result;
		if (resource1 != null && resource2 != null) {
			result = super.compare(viewer, resource1, resource2);
		} else {
			result = super.compare(viewer, o1, o2);
		}
		compareFullPaths = false;
		return result;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.navigator.ResourceSorter#compareNames(org.eclipse.core.resources.IResource, org.eclipse.core.resources.IResource)
	 */
	protected int compareNames(IResource resource1, IResource resource2) {
		if(compareFullPaths) {
			return collator.compare(resource1.getFullPath().toString(), resource2.getFullPath().toString());
		} else {
			return collator.compare(resource1.getName(), resource2.getName());
		}
	}
	
	protected boolean isResourcePath(Object o1) {
		if (o1 instanceof SyncInfoDiffNode) {
			return ((SyncInfoDiffNode)o1).isResourcePath();
		}
		return false;
	}

	protected IResource getResource(Object obj) {
		return SyncInfoSetContentProvider.getResource(obj);
	}
}
