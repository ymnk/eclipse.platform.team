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

import java.lang.ref.SoftReference;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.core.Policy;

/**
 * Implementation of {@link org.eclipse.team.internal.core.caches.ICacheableReference} which provides
 * caching behavior. Cached objects are always held onto by soft references
 * so the memory will be freed if memory is low.
 * @see org.eclipse.team.internal.core.caches.ICacheableReference
 */
public class CacheableReference implements ICacheableReference {
    
    Object cacheLock = new Object();
    SoftReference cached;
    ICacheSource source;

    /**
     * Create a reference that will load the cachable object from the
     * given source
     */
    public CacheableReference(ICacheSource source) {
        super();
        this.source = source;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.caches.IObjectSource#getObject(org.eclipse.core.runtime.IProgressMonitor)
     */
    public Object getObject(IProgressMonitor monitor) throws CoreException {
        return getObject(NONE, monitor);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.caches.IObjectSource#getObject(boolean, org.eclipse.core.runtime.IProgressMonitor)
     */
    public Object getObject(int flags, IProgressMonitor monitor) throws CoreException {
        if ((flags & ALWAYS_FETCH) == 0) {
            // Return the cached instance unless told to refetch
	        synchronized(cacheLock) {
	            if (cached != null) {
		            Object o = cached.get();
		            if (o != null) {
		                return o;
		            }
	            }
	        }
        }
        if ((flags & DO_NOT_FETCH_IF_ABSENT) > 0 && (flags & ALWAYS_FETCH) == 0) {
            // The object is not cached and the client requested to avoid fetching it
            // Let ALWAYS_FETCH override DO_NOT_FETCH_IF_ABSENT
            return null;
        }
        if ((flags & ONLY_LOAD_REPLICA_IF_ABSENT) > 0 && !source.isLocalReplica()) {
            // The source is not a local replica so don't load
            return null;   
        }
        
        // TODO: should serialize fetch to ensure it doesn't happen concurrently
        Object o = fetchObject(flags, Policy.monitorFor(monitor));
        setCachedObject(o);
        return o;
    }

    /**
     * Set the object that is cached by this reference
     * @param o the object to be cached
     */
    public void setCachedObject(Object o) {
        if (o != null) {
            synchronized(cacheLock) {
                cached = new SoftReference(o);
            }
        }
    }

    /**
     * Fetch the object from the source. Note that <code>null</code>
     * should not be returned from this method but instead an exception
     * indicating why the fetch failed should be thrown.
     * @param monitor a progress monitor
     * @return the fetched object
     */
    protected Object fetchObject(int flags, IProgressMonitor monitor) throws CoreException {
        return source.fetch(flags, monitor);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.caches.ICacheableReference#clear()
     */
    public void clear() {
        cached.clear();
        
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.caches.ICacheableReference#getSource()
     */
    public ICacheSource getSource() {
        return source;
    }

}
