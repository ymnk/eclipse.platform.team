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
package org.eclipse.team.internal.ccvs.ui.repo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.ui.IMemento;

/**
 * Provides access to the tags for a remte folder that are cached
 * on the loal disk
 */
public class LocalTagCache extends XMLLocalReplica {
    
    private static final String NAME_ATTRIBUTE = "name"; //$NON-NLS-1$
    private static final String TYPE_ATTRIBUTE = "type"; //$NON-NLS-1$
    private static final String TAG_ELEMENT = "tag"; //$NON-NLS-1$
    
    private final String localFilePath;

    public LocalTagCache(RepositoryRoot location, String remotePath, String localFilePath) {
        super(new TagCacheSource(location, remotePath));
        this.localFilePath = localFilePath;
    }

    private String getFilePath() {
        return localFilePath;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.repo.XMLLocalReplica#getFile()
     */
    protected File getFile() {
        return new File(getFilePath());
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.repo.XMLLocalReplica#save(java.lang.Object, org.eclipse.ui.IMemento, org.eclipse.core.runtime.IProgressMonitor)
     */
    protected void save(Object o, IMemento memento, IProgressMonitor monitor) throws CoreException {
        CVSTag[] tags = (CVSTag[])o;
        for (int i = 0; i < tags.length; i++) {
            CVSTag tag = tags[i];
            IMemento child = memento.createChild(TAG_ELEMENT);
            child.putInteger(TYPE_ATTRIBUTE, tag.getType());
            child.putString(NAME_ATTRIBUTE, tag.getName());
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.repo.XMLLocalReplica#load(int, org.eclipse.ui.IMemento, org.eclipse.core.runtime.IProgressMonitor)
     */
    protected Object load(int flags, IMemento memento, IProgressMonitor monitor) throws CoreException {
        IMemento[] children = memento.getChildren(TAG_ELEMENT);
        List tags = new ArrayList();
        for (int i = 0; i < children.length; i++) {
            IMemento child = children[i];
            Integer type = child.getInteger(TYPE_ATTRIBUTE);
            String name = child.getString(NAME_ATTRIBUTE);
            if (type != null 
                    && name != null 
                    && (type.intValue() == CVSTag.BRANCH 
                            || type.intValue() == CVSTag.VERSION)) {
                CVSTag tag = new CVSTag(name, type.intValue());
                tags.add(tag);
            }
        }
        return (CVSTag[]) tags.toArray(new CVSTag[tags.size()]);
    }

    /**
     * Set the tags in the replica to those provided.
     * @param tags the tags
     * @throws CoreException
     */
    public void setTags(CVSTag[] tags) throws CoreException {
        save(tags, Policy.monitorFor(null));
    }

    /**
     * Delete the replica from disk
     */
    public void dispose() {
        getFile().delete();
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.caches.ICacheSource#isLocalReplica()
     */
    public boolean isLocalReplica() {
        return true;
    }
}
