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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A reference to an object that resides at a non-memory location such
 * as on disk or on a remote server. The reference implementation may cache
 * the in-memory representation of the object.
 */
public interface ICacheableReference {
    
    /**
     * Flag indicating that the default behavior should be used.
     */
    public static final int NONE = 0;
    
    /**
     * Flag indicating that the object should be fetched if it is not
     * cached.
     */
    public static final int DO_NOT_FETCH_IF_ABSENT = 1;
    
    /**
     * Flag indicating that the object should be refetched 
     * even if it is cached.
     */
    public static final int ALWAYS_FETCH = 2;
    
    /**
     * This contant provides the beginning flag that can be used by clients
     * to provide custom flags for fetching objects.
     */
    public static final int BEGINNING_CLIENT_FLAG_RANGE = 256;

    /**
     * Return the object that is persisted at this source. The object will be fetched
     * if it is not cached unless the <code>DO_NOT_FETCH_IF_ABSENT</code> flag is set.
     * If this flag is set and the object is not cached, <code>null</code> is returned.
     * In all other cases, an object is returned unless the object could not be fecthed,
     * in which case an exception will be thrown.
     * If the <code>ALWAYS_FETCH</code> flag is set, the object will always be fetched
     * regardless of whether it is currently cached.
     * This method may be long running if the object is being
     * fetched, so a progress monitor should be provided.
     * @param flags the set of flags that configure the get.
     * @param a progress monitor or <code>null</code>
     * @return the object that is persisted at this source or <code>null</code>
     * if the object is not cached and the <code>DO_NOT_FETCH_IF_ABSENT</code> is
     * set.
     * @throws CoreException if the object could not be fetched
     */
    Object getObject(int flags, IProgressMonitor monitor) throws CoreException;
    
    /**
     * Return the object that is persisted at this source. The object will be fetched
     * if it is not cached. This method may be long running if the object is being
     * fetched, so a progress monitor should be provided. This method is equivalent to
     * <code>getObject(NONE, monitor)</code>.
     * @param a progress monitor or <code>null</code>
     * @return the object that is persisted
     */
    Object getObject(IProgressMonitor monitor) throws CoreException;

    /**
     * Clear the cached object so that the next time it is accessed,
     * it will need to be obtained from the source.
     */
    void clear();

    /**
     * Return the source from which this reference obtains it's cached object.
     * @return the source from which this reference obtains it's cached object
     */
    ICacheSource getSource();
}
