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
package org.eclipse.team.internal.core.caches;

import org.eclipse.core.runtime.*;
import org.eclipse.team.internal.core.Policy;
import org.eclipse.team.internal.core.TeamPlugin;

/**
 * A cache source that keeps a replica on the local disk of information 
 * that is stored on a server.
 * 
 * TODO: need concurreny protection against multiple accesses causing concurrent saves, etc.
 */
public abstract class LocalReplica implements ICacheSource {
    
    private final ICacheSource remote;

    /**
     * Create a local replica of the given remote cache source
     * @param remote the remote cache source
     */
    protected LocalReplica(ICacheSource remote) {
        this.remote = remote;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.caches.ICacheSource#getLocationDescription()
     */
    public String getLocationDescription() {
        return "Local replica of " + remote.getLocationDescription();
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
        if ((flags & ICacheableReference.ALWAYS_FETCH) > 0) {
            // fetch from the server and update the local replica on disk
            return fetchRemote(flags, monitor);
        } else {
            try {
                // load the replica from disk
                return load(flags, monitor);
            } catch (Exception e) {
                // If possible, try to fetch the remote contents
                if ((flags & ICacheableReference.DO_NOT_FETCH_IF_ABSENT) > 0) {
                    // we were instructed not to fetch so throw the exception
                    if (e instanceof CoreException) {
                        throw (CoreException)e;
                    } else if (e instanceof RuntimeException) {
                        throw (RuntimeException)e;
                    }
                    // Impossible but handle anyway.
                    TeamPlugin.log(IStatus.WARNING, "Could not load local replica.", e); //$NON-NLS-1$
                    return null;
                }
                TeamPlugin.log(IStatus.WARNING, "Could not load local replica. Fetching from the remote source.", e); //$NON-NLS-1$
                return fetchRemote(flags, monitor);
            }
        }
    }


    /**
     * Fetch the object from the remote source and update the local replica on disk
     * by calling <code>save</code>.
     * @param flags the flags passed to the get on the cached reference
     * @param monitor a progress monitor
     * @return the object fetched from the server
     */
    protected Object fetchRemote(int flags, IProgressMonitor monitor) throws CoreException {
        monitor.beginTask(null, 100);
        try {
            Object o = remote.fetch(flags, Policy.subMonitorFor(monitor, 90));
            try {
                save(o, Policy.subMonitorFor(monitor, 10));
            } catch (Exception e) {
                // Log the exception and continue since the in ability to create
                // the local replica may not cause anything else to fail
                TeamPlugin.log(IStatus.WARNING, "Could no save local replica. Operation is not affected by oerformance may be reduced.", e); //$NON-NLS-1$
            }
            return o;
        } finally {
            monitor.done();
        }
    }

    /**
     * Save the object that was just fetched from the remote source to disk
     * as the local replica
     * @param o the fetched object
     * @param monitor a progress monitor
     */
    
    protected abstract void save(Object o, IProgressMonitor monitor) throws CoreException;
    
    /**
     * Load the local replica of the remote object from disk.
     * @param flags the flags passed to the get
     * @param monitor a progress monitor
     * @return the object
     */
    protected abstract Object load(int flags, IProgressMonitor monitor) throws CoreException;
    
    /**
     * Return the remote cache source that provides the object cached
     * locally.
     * @return the remote cache source that provides the object cached
     * locally
     */
    protected ICacheSource getRemote() {
        return remote;
    }
}
