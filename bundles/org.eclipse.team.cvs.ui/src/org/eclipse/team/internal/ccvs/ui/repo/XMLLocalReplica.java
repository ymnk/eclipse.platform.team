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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.core.caches.ICacheSource;
import org.eclipse.team.internal.core.caches.LocalReplica;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;

/**
 * A local replica that uses IMemento to persist and restore the local replica.
 */
public abstract class XMLLocalReplica extends LocalReplica {

    /**
     * Create an IMemento based local replica.
     * @param remote the remote cache source being replicated
     */
    protected XMLLocalReplica(ICacheSource remote) {
        super(remote);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.caches.LocalReplica#save(java.lang.Object, org.eclipse.core.runtime.IProgressMonitor)
     */
    protected void save(Object o, IProgressMonitor monitor) throws CoreException {
        XMLMemento memento = XMLMemento.createWriteRoot(getType());
        save(o, memento, monitor);
        Writer writer = null;
        File file = getFile();
        File parentDir = file.getParentFile();
        if (!parentDir.exists())
            parentDir.mkdirs();
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
            memento.save(writer);
        } catch (FileNotFoundException e) {
            throw new TeamException("Could not create file " + file.getAbsolutePath(), e);
        } catch (IOException e) {
            throw new TeamException("Could not write to file " + file.getAbsolutePath(), e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e1) {
                    // Ignore
                }
            }
        }

    }

    private String getType() {
        return "replica"; //$NON-NLS-1$
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.caches.LocalReplica#load(org.eclipse.core.runtime.IProgressMonitor)
     */
    protected Object load(int flags, IProgressMonitor monitor) throws CoreException {
        // reload from the disk cache
        Reader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(getFile())));
            XMLMemento memento = XMLMemento.createReadRoot(reader);
            return load(flags, memento, monitor);
        } catch (FileNotFoundException e) {
            // No file on disk so fetch from the remote source
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

    /**
     * Return the file where the local replica is to be persisted.
     * @return the file where the local replica is to be persisted
     */
    protected abstract File getFile();
    
    /**
     * Save the object to the given memento which will be used to
     * persist the locla replica to disk.
     * @param o the object being cached
     * @param memento the memento used to cache the object
     * @param monitor a progress monitor
     */
    protected abstract void save(Object o, IMemento memento, IProgressMonitor monitor) throws CoreException;
    
    /**
     * Load the object from the given memento.
     * @param flags the flags passed to the cache reference
     * @param memento the memento containing the replica
     * @param monitor a progress monitor
     * @return the object build from the replica
     */
    protected abstract Object load(int flags, IMemento memento, IProgressMonitor monitor) throws CoreException;

}
