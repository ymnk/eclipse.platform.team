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

import java.io.*;

import org.eclipse.core.runtime.*;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.util.Util;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.core.caches.ICacheSource;
import org.eclipse.team.internal.core.caches.ICacheableReference;
import org.eclipse.ui.*;

/**
 * Provides access to the tags for a remte folder that are cached
 * on the loal disk
 */
public class LocalTagCache implements ICacheSource {
    
    private final String remoteFolderPath;
    private final String localFilePath;
    private final RepositoryRoot location;

    public LocalTagCache(RepositoryRoot location, String remoteFolderPath, String localFilePath) {
        super();
        this.location = location;
        this.remoteFolderPath = remoteFolderPath;
        this.localFilePath = localFilePath;
    }
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.caches.ICacheSource#getLocationDescription()
     */
    public String getLocationDescription() {
        return "Tags for " + Util.appendPath(location.getRoot().getLocation(), remoteFolderPath);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.caches.ICacheSource#fetch(int, org.eclipse.core.runtime.IProgressMonitor)
     */
    public Object fetch(int flags, IProgressMonitor monitor) throws CoreException {
        if ((flags & ICacheableReference.ALWAYS_FETCH) > 0) {
            // fetch from the server and update the disk cache
            return fetchFromServer(flags, monitor);
        } else {
            return loadFromDisk(flags, monitor);
        }
    }
    
    private Object loadFromDisk(int flags, IProgressMonitor monitor) throws WorkbenchException {
        // reload from the disk cache
        Reader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(getFile())));
            IMemento memento = XMLMemento.createReadRoot(reader);
            return null;
        } catch (FileNotFoundException e) {
            // Log the exception and try to load from the server
            CVSUIPlugin.log(IStatus.ERROR, "Tag cache for " + remoteFolderPath + " was not found on disk. Refetching the tags from the server will be attempted.", e);
            return fetchFromServer(flags, monitor);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    // Ignore
                }
            }
        }
    }

    private String getFile() {
        return localFilePath;
    }
    
    private Object fetchFromServer(int flags, IProgressMonitor monitor) {
        CVSTag[] tags = location.fetchTags(getFolder(), isRecurse(flags), monitor);
        writeToDisk(tags);
        return tags;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return getLocationDescription();
    }

}
