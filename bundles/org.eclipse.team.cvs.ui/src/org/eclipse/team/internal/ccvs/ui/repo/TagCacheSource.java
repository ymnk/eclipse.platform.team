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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.util.Util;
import org.eclipse.team.internal.core.caches.ICacheSource;
import org.eclipse.team.internal.core.caches.ICacheableReference;

/**
 * A cache source that fetches the branch and version
 * tags for a remote folder from a repository.
 */
public class TagCacheSource implements ICacheSource {
    
    public static final int RECURSE = ICacheableReference.BEGINNING_CLIENT_FLAG_RANGE;
    
    private final String remoteFolderPath;
    private final RepositoryRoot location;

    public TagCacheSource(RepositoryRoot location, String remoteFolderPath) {
        super();
        this.location = location;
        this.remoteFolderPath = remoteFolderPath;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.caches.ICacheSource#getLocationDescription()
     */
    public String getLocationDescription() {
        return "Tags for " + Util.appendPath(location.getRoot().getLocation(), remoteFolderPath);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
         return getLocationDescription();
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.caches.ICacheSource#fetch(int, org.eclipse.core.runtime.IProgressMonitor)
     */
    public Object fetch(int flags, IProgressMonitor monitor) throws CoreException {
        ICVSFolder folder = getFolder(monitor);
        if (folder == null) {
            // Could happen if the path was a defined module which no longer exists
            return new CVSTag[0];
        }
        CVSTag[] tags = location.fetchTags(folder, isRecurse(flags), monitor);
        return tags;
    }
    
    private boolean isRecurse(int flags) {
        return (flags & RECURSE) > 0;
    }
    
    private ICVSFolder getFolder(IProgressMonitor monitor) {
        return location.getRemoteFolder(remoteFolderPath, CVSTag.DEFAULT, monitor);
    }

    public String getRemotePath() {
        return remoteFolderPath;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.caches.ICacheSource#isLocalReplica()
     */
    public boolean isLocalReplica() {
        return false;
    }
}
