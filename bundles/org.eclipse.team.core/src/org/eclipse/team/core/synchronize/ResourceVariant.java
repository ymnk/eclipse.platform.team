/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.core.synchronize;

import java.io.InputStream;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.*;

/**
 * A resource variant is a partial implementation of a remote resource
 * that caches any fetched contents in a <code>ResourceVariantCache</code>.
 */
public abstract class ResourceVariant implements IRemoteResource, IAdaptable {
	
	// holds the storage instance for this resource variant
	private IStorage storage;
	
	/**
	 * Add the resource variant to the given cache. The provided resource
	 * will be cached even if another instance was cached previously.
	 * @param cache the resource variant cache
	 * @param resource the resource variant to be cached
	 */
	public static void cacheResourceVariant(RemoteContentsCache cache, ResourceVariant resource) {
		cache.beginOperation();
		try {
			RemoteContentsCacheEntry entry = cache.getCacheEntry(resource.getUniquePath());
			entry.setResourceVariant(resource);
		} finally {
			cache.endOperation();
		}
	}
	
	/**
	 * Get the resource variant with the given unique path from the cache.
	 * If the resource is not cached, <code>null</code> is returned.
	 * @param cache the resource variant cache
	 * @param id the unique path of the desired resource variant
	 * @return the cached resource variant or <code>null</code>
	 */
	public static ResourceVariant getResourceVariant(RemoteContentsCache cache, String id) {
		if (!cache.hasEntry(id)) return null;
		RemoteContentsCacheEntry entry = cache.getCacheEntry(id);
		return entry.getResourceVariant();
	}
	
	/*
	 * Internal class which provides access to the cached contents
	 * of this resource variant
	 */
	class ResourceVariantStorage implements IStorage {
		public InputStream getContents() throws CoreException {
			if (!isContentsCached()) {
				// The cache may have been cleared if someone held
				// on to the storage too long
				throw new TeamException("There is no cached contents for resource {0}." + getUniquePath());
			}
			return getCache().getCacheEntry(getUniquePath()).getContents();
		}
		public IPath getFullPath() {
			return getFullPath();
		}
		public String getName() {
			return ResourceVariant.this.getName();
		}
		public boolean isReadOnly() {
			return true;
		}
		public Object getAdapter(Class adapter) {
			return ResourceVariant.this.getAdapter(adapter);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.synchronize.IRemoteResource#getStorage(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStorage getStorage(IProgressMonitor monitor) throws TeamException {
		if (isContainer()) return null;
		ensureContentsCached(monitor);
		if (storage == null) {
			storage = new ResourceVariantStorage();
		}
		return storage;
	}
	
	private void ensureContentsCached(IProgressMonitor monitor) throws TeamException {
		// Ensure that the contents are cached from the server
		if (!isContentsCached()) {
			// Ensure that a resource variant handle is cached before fetching the contents
			getCache().beginOperation();
			try {
				if (!isCached()) {
					cacheResourceVariant(getCache(), this);
				}
			} finally {
				getCache().endOperation();
			}
			// Fetching of contents can be done without holding the cache lock.
			// The lock will be obtained when the contents are set.
			fetchContents(monitor);
		}
	}
	
	/**
	 * Method that is invoked when the contents of the resource variant need to 
	 * be fetched. Subclasses should override this method and invoke <code>setContents</code>
	 * with a stream containing the fetched contents.
	 * @param monitor a progress monitor
	 */
	protected abstract void fetchContents(IProgressMonitor monitor);

	/**
	 * This method should be invoked by subclasses from within their <code>fetchContents</code>
	 * method in order to cache the contents for this resource variant.
	 * @param stream the stream containing the contents of the resource variant
	 * @param monitor a progress monitor
	 * @throws TeamException
	 */
	protected void setContents(InputStream stream, IProgressMonitor monitor) throws TeamException {
		getCacheEntry().setContents(stream, monitor);
	}
	
	private synchronized RemoteContentsCacheEntry getCacheEntry() {
		return getCache().getCacheEntry(getUniquePath());
	}
	
	/**
	 * Return whether there are already contents cached for ths resource variant.
	 * This method will return <code>false</code> even if the contents are currently
	 * being cached by another thread. The consequence of this is that the contents
	 * may be fetched twice in the rare case where two threads request the same contents
	 * at the same time.
	 */
	protected boolean isContentsCached() {
		if (!isCached()) {
			return false;
		}
		RemoteContentsCacheEntry entry = getCache().getCacheEntry(getUniquePath());
		return entry.getState() == RemoteContentsCacheEntry.READY;
	}
	
	/**
	 * Return <code>true</code> if the cache contains an entry for this resource
	 * variant. It is possible that another instance of this variant is cached.
	 * To get the cached instance, call <code>getCachedVariant()</code>. Note that 
	 * cached contents can be retrieved from any handle to a resource variant but other
	 * satte information may only be accessible from the cached copy.
	 * @return whether the variant is cached
	 */
	protected boolean isCached() {
		return (getCache().hasEntry(getUniquePath()));
	}

	/**
	 * Get the path that uniquely identifies the remote resource
	 * variant. This path descibes the remote location where
	 * the remote resource is stored and also uniquely identifies
	 * each resource variant. It is used to uniquely identify this
	 * resource variant when it is stored in the resource variant cache.
	 * @return the full path of the remote resource variant
	 */
	public abstract String getUniquePath();
	
	/**
	 * Return the size (in bytes) of the contents of this resource variant.
	 * The method will return 0 if the contents have not yet been cached
	 * locally
	 */
	public long getSize() {
		if (!isContentsCached()) return 0;
		RemoteContentsCacheEntry entry = getCacheEntry();
		if (entry == null || entry.getState() != RemoteContentsCacheEntry.READY) {
			return 0;
		}
		return entry.getSize();
	}
	
	/**
	 * Return the cache that is used to cache this resource variant and its contents.
	 * @return Returns the cache.
	 */
	protected abstract RemoteContentsCache getCache();
	
	/**
	 * Return the cached handle for this resource variant if there is
	 * one. If there isn't one, then this handle will be cached.
	 * @return a cached copy of this resource variant
	 */
	protected ResourceVariant getCachedHandle() {
		getCache().beginOperation();
		try {
			if (isCached()) {
				return getResourceVariant(getCache(), getUniquePath());
			} else {
				cacheResourceVariant(getCache(), this);
				return this;
			}
		} finally {
			getCache().endOperation();
		}
	}
}
