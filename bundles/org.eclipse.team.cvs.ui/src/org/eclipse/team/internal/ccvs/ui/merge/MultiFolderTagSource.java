/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.merge;

import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.ui.model.CVSFolderElement;

/**
 * A tag source for multiple folders.
 * 
 * TODO: Temporarily a subclass of single folder until I 
 * can figure out how to handle the multi-folder case.
 */
public class MultiFolderTagSource extends SingleFolderTagSource {

    private ICVSFolder[] folders;

    public MultiFolderTagSource(ICVSFolder[] folders) {
        super(folders[0]);
        this.folders = folders;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.merge.SingleFolderTagSource#getShortDescription()
     */
    public String getShortDescription() {
        return "{0} folders" + folders.length;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.merge.SingleFolderTagSource#getChildren(java.lang.Object)
     */
    public Object[] getChildren(Object o) {
		CVSFolderElement[] elements = new CVSFolderElement[folders.length];
		for (int i = 0; i < folders.length; i++) {
			elements[i] = new CVSFolderElement(folders[i], false);
		}
		return elements;
    }

    /**
     * Set the folders of this tag source
     * @param remoteFolders
     */
    public void setFolders(ICVSFolder[] remoteFolders) {
        folders = remoteFolders;
    }

}
