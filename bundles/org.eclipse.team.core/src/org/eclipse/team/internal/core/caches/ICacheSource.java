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
 * The source of an object that can be cached by an {@link org.eclipse.team.internal.core.caches.ICacheableReference}.
 * The source of the object is a location on disk (or a remote server)
 */
public interface ICacheSource {
    
    /**
     * Return a human readable string that describes the location
     * of the object source.
     * @return a description of the source location
     */
    String getLocationDescription();

    /**
     * Fetch the object from the source. The returned object
     * should be in a form that can be cached by an
     * {@link CacheableReference} for an indeterminant amout of time.
     * @param flags the flags that were passed to the <code>ICacheableReference</code> that initiated the fetch.
     * @param monitor a progress monitor
     * @return a cachable object representing the data at he source location
     */
    Object fetch(int flags, IProgressMonitor monitor) throws CoreException;
    
    /**
     * Return whether the cache source is a local replica of a remote source.
     * @return whether the cache source is a local replica of a remote source
     */
    boolean isLocalReplica();
}
