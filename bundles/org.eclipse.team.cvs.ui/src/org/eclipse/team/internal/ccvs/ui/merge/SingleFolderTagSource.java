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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.model.CVSFolderElement;
import org.eclipse.team.internal.ccvs.ui.repo.RepositoryManager;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * A tag source that returns the tags associated with a single remote folder
 */
public class SingleFolderTagSource extends TagSource implements IWorkbenchAdapter, IAdaptable {

    public static CVSTag[] getTags(ICVSFolder folder, int type) {
        if (type == CVSTag.HEAD)
            return new CVSTag[] { CVSTag.DEFAULT } ;
        return CVSUIPlugin.getPlugin().getRepositoryManager().getKnownTags(folder, type);
    }
    
    private ICVSFolder folder;
    
    public SingleFolderTagSource(ICVSFolder folder) {
        this.folder = folder;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.merge.TagSource#getTags(int)
     */
    public CVSTag[] getTags(int type) {
        return getTags(getFolder(), type);
    }

    /**
     * Return the folder the tags are obtained from
     * @return the folder the tags are obtained from
     */
    public ICVSFolder getFolder() {
         return folder;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.merge.TagSource#refresh(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void refresh(IProgressMonitor monitor) throws TeamException {
        CVSUIPlugin.getPlugin().getRepositoryManager().refreshDefinedTags(getFolder(), false /* replace */, true /* notify */, monitor);
        fireChange();
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.merge.TagSource#getLocation()
     */
    public ICVSRepositoryLocation getLocation() {
		RepositoryManager mgr = CVSUIPlugin.getPlugin().getRepositoryManager();
		ICVSRepositoryLocation location = mgr.getRepositoryLocationFor(getFolder());
		return location;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.merge.TagSource#getShortDescription()
     */
    public String getShortDescription() {
        return getFolder().getName();
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.merge.TagSource#commit(org.eclipse.team.internal.ccvs.core.CVSTag[], boolean, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void commit(final CVSTag[] tags, final boolean replace, IProgressMonitor monitor) throws CVSException {
		try {
            final RepositoryManager manager = CVSUIPlugin.getPlugin().getRepositoryManager();	
            manager.run(new IRunnableWithProgress() {
            	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            		try {
            		    ICVSFolder folder = getFolder();
            		    if (replace) {
            		        CVSTag[] oldTags = manager.getKnownTags(folder);
            		        manager.removeTags(folder, oldTags);
            		    }
            		    manager.addTags(folder, tags);
            		} catch (CVSException e) {
            			throw new InvocationTargetException(e);
            		}
            	}
            }, monitor);
        } catch (InvocationTargetException e) {
            throw CVSException.wrapException(e);
        } catch (InterruptedException e) {
            // Ignore
        }
        fireChange();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.model.IWorkbenchAdapter#getChildren(java.lang.Object)
     */
    public Object[] getChildren(Object o) {
        return new CVSFolderElement(getFolder(), false).getChildren(o);
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
        return getShortDescription();
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

}
