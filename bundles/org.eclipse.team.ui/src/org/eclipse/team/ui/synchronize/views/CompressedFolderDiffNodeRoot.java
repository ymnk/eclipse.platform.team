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
package org.eclipse.team.ui.synchronize.views;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.subscribers.SyncInfoSet;
import org.eclipse.team.ui.synchronize.SyncInfoDiffNodeRoot;

public class CompressedFolderDiffNodeRoot extends SyncInfoDiffNodeRoot {

	public CompressedFolderDiffNodeRoot(SyncInfoSet set) {
		super(set);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncInfoDiffNodeRoot#createBuilder()
	 */
	protected SyncInfoDiffNodeBuilder createBuilder() {
		return new CompressedFolderDiffNodeBuilder(this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncInfoDiffNodeRoot#getSorter()
	 */
	public SyncInfoDiffNodeSorter getSorter() {
		return new SyncInfoDiffNodeSorter() {
			protected int compareNames(IResource resource1, IResource resource2) {
				if (resource1.getType() == IResource.FOLDER && resource2.getType() == IResource.FOLDER) {
					return collator.compare(resource1.getParent().toString(), resource2.getProjectRelativePath().toString());
				}
				return super.compareNames(resource1, resource2);
			}
		};
	}
}
