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
package org.eclipse.team.ui.synchronize.viewers;

import java.util.*;

import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.core.subscribers.SyncInfoSet;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.ISharedImages;

/**
 * A compressed folder appears under a project and contains out-of-sync resources
 */
public class CompressedFolderDiffNode extends SyncInfoDiffNode {

	public CompressedFolderDiffNode(IDiffContainer parent, SyncInfoSet input, IResource resource) {
		super(parent, input, resource);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncInfoDiffNode#getChildSyncInfos()
	 */
	public SyncInfo[] getDescendantSyncInfos() {
		IResource[] children = getSyncInfoSet().members(getResource());
		List result = new ArrayList();
		for (int i = 0; i < children.length; i++) {
			IResource child = children[i];
			SyncInfo info = getSyncInfoSet().getSyncInfo(child);
			if (info != null) {
				if (child.getType() == IResource.FILE) {
					result.add(info);
				}
			}
		}
		return (SyncInfo[]) result.toArray(new SyncInfo[result.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncInfoDiffNode#isResourcePath()
	 */
	public boolean isResourcePath() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.structuremergeviewer.DiffNode#getName()
	 */
	public String getName() {
		IResource resource = getResource();
		return resource.getProjectRelativePath().toString();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncInfoDiffNode#getImageDescriptor(java.lang.Object)
	 */
	public ImageDescriptor getImageDescriptor(Object object) {
		return TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_COMPRESSED_FOLDER);
	}
}
