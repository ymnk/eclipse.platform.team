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
package org.eclipse.team.internal.ccvs.ui.tags;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * A workbench adapter that can be used to view the resources that make up 
 * a tag source. It is used by the TagConfigurationDialog.
 */
public class TagSourceResourceAdapter implements IAdaptable, IWorkbenchAdapter {

    public static Object getViewerInput(TagSource tagSource) {
        return new TagSourceResourceAdapter(tagSource);
    }
    
    TagSource tagSource;

    private TagSourceResourceAdapter(TagSource tagSource) {
        this.tagSource = tagSource;
    }

    
    /* (non-Javadoc)
     * @see org.eclipse.ui.model.IWorkbenchAdapter#getChildren(java.lang.Object)
     */
    public Object[] getChildren(Object o) {
        if (tagSource instanceof MultiFolderTagSource) {
            
        } else if (tagSource instanceof SingleFolderTagSource) {
            return new CVSFolderElement(((SingleFolderTagSource)tagSource).getFolder(), false).getChildren(o);
        }
        return new Object[0];
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.model.IWorkbenchAdapter#getImageDescriptor(java.lang.Object)
     */
    public ImageDescriptor getImageDescriptor(Object object) {
        // No imgae descriptor
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
     */
    public String getLabel(Object o) {
        return tagSource.getShortDescription();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
     */
    public Object getParent(Object o) {
        // No parent
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
     */
    public Object getAdapter(Class adapter) {
        if (adapter == IWorkbenchAdapter.class) {
            return this;
        }
        return null;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.merge.SingleFolderTagSource#getChildren(java.lang.Object)
     */
    public Object[] getFolderElements() {
        ICVSFolder[] folders = ((MultiFolderTagSource)tagSource).getFolders();
		CVSFolderElement[] elements = new CVSFolderElement[folders.length];
		for (int i = 0; i < folders.length; i++) {
			elements[i] = new CVSFolderElement(folders[i], false);
		}
		return elements;
    }

}
