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
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.core.caches.LocalReplica;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;

/**
 * Provides access to the tags for a remte folder that are cached
 * on the loal disk
 */
public class LocalTagCache extends LocalReplica {
    
    private final String localFilePath;

    public LocalTagCache(RepositoryRoot location, String remotePath, String localFilePath) {
        super(new TagCacheSource(location, remotePath));
        this.localFilePath = localFilePath;
    }
    
    private Object loadFromDisk(int flags, IProgressMonitor monitor) throws CoreException {
        // reload from the disk cache
        Reader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(getFile())));
            IMemento memento = XMLMemento.createReadRoot(reader);
            return null;
        } catch (FileNotFoundException e) {
            // Log the exception and try to load from the server
            String remoteFolderPath = ((TagCacheSource)getRemote()).getRemotePath();
            CVSUIPlugin.log(IStatus.ERROR, "Tag cache for " + remoteFolderPath + " was not found on disk. Refetching the tags from the server will be attempted.", e);
            return fetchRemote(flags, monitor);
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

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.caches.LocalReplica#save(java.lang.Object, org.eclipse.core.runtime.IProgressMonitor)
     */
    protected void save(Object o, IProgressMonitor monitor) throws CoreException {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.caches.LocalReplica#load(org.eclipse.core.runtime.IProgressMonitor)
     */
    protected Object load(IProgressMonitor monitor) throws CoreException {
        // TODO Auto-generated method stub
        return null;
    }
}
