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

import org.eclipse.core.resources.IResource;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.ISharedImages;
import org.eclipse.team.ui.synchronize.views.*;
import org.eclipse.team.ui.synchronize.views.SyncInfoLabelProvider;

/**
 * Label provider for compressed folders
 */
public class CompressedFolderLabelProvider extends SyncInfoLabelProvider {

	private Image compressedFolderImage;
	
	public Image getCompressedFolderImage() {
		if (compressedFolderImage == null) {
			compressedFolderImage = TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_COMPRESSED_FOLDER).createImage();
		}
		return compressedFolderImage;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	public void dispose() {
		if (compressedFolderImage != null) {
			compressedFolderImage.dispose();
		}
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		if (element instanceof CompressedFolder) {
			return getCompressedFolderImage();
		}
		return super.getImage(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		if (element instanceof CompressedFolder) {
			IResource resource = SyncInfoSetContentProvider.getResource(element);
			return resource.getProjectRelativePath().toString();
		}
		return super.getText(element);
	}

}
