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
package org.eclipse.team.internal.ui.synchronize.views;

import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.core.subscribers.SyncInfoSet;
import org.eclipse.team.ui.synchronize.SyncInfoDiffNode;

/**
 * A compressed folder appears under a project and contains out-of-sync resources
 */
public class CompressedFolder extends SyncInfoDiffNode {

	public CompressedFolder(SyncInfoSet input, IResource resource) {
		super(input, resource);
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
				if (child.getType() == IResource.FOLDER) {
					// for folders, add all out-of-sync children
					// NOTE: the method getOutOfSyncDescendants includes the out-of-sync parent
					result.addAll(Arrays.asList(getSyncInfoSet().getOutOfSyncDescendants(child)));
				} else {
					// for files, just add the info
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

}
