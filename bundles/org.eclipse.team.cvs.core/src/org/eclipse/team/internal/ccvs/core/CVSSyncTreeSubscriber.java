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
package org.eclipse.team.internal.ccvs.core;

import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.core.subscribers.helpers.*;
import org.eclipse.team.internal.ccvs.core.resources.*;
import org.eclipse.team.internal.ccvs.core.syncinfo.CVSRefreshOperation;

/**
 * This class provides common funtionality for three way sychronizing
 * for CVS.
 */
public abstract class CVSSyncTreeSubscriber extends SyncTreeSubscriber {
	
	public static final String SYNC_KEY_QUALIFIER = "org.eclipse.team.cvs"; //$NON-NLS-1$
	
	private QualifiedName id;
	private String name;
	private String description;
	
	CVSSyncTreeSubscriber(QualifiedName id, String name, String description) {
		this.id = id;
		this.name = name;
		this.description = description;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#getId()
	 */
	public QualifiedName getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#getName()
	 */
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#getDescription()
	 */
	public String getDescription() {
		return description;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.TeamSubscriber#roots()
	 */
	public IResource[] roots() {
		return null;
	}

	public SyncInfo getSyncInfo(IResource resource, IProgressMonitor monitor) throws TeamException {
		if (!isSupervised(resource)) return null;
		if(resource.getType() == IResource.FILE) {
			return super.getSyncInfo(resource, monitor);
		} else {
			// In CVS, folders do not have a base. Hence, the remote is used as the base.
			ISubscriberResource remoteResource = getRemoteResource(resource);
			return getSyncInfo(resource, remoteResource, remoteResource, monitor);
		}
	}
	
	/**
	 * Method that creates an instance of SyncInfo for the provider local, base and remote.
	 * Can be overiden by subclasses.
	 * @param local
	 * @param base
	 * @param remote
	 * @param monitor
	 * @return
	 */
	protected SyncInfo getSyncInfo(IResource local, ISubscriberResource base, ISubscriberResource remote, IProgressMonitor monitor) throws TeamException {
		try {
			monitor = Policy.monitorFor(monitor);
			monitor.beginTask(null, 100);
			CVSSyncInfo info = new CVSSyncInfo(local, base, remote, this, Policy.subMonitorFor(monitor, 100));
			return info;
		} finally {
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#refresh(org.eclipse.core.resources.IResource[], int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void refresh(IResource[] resources, int depth, IProgressMonitor monitor) throws TeamException {
		monitor = Policy.monitorFor(monitor);
		List errors = new ArrayList();
		try {
			monitor.beginTask(null, 100 * resources.length);
			for (int i = 0; i < resources.length; i++) {
				IResource resource = resources[i];
				IStatus status = refresh(resource, depth, Policy.subMonitorFor(monitor, 100));
				if (!status.isOK()) {
					errors.add(status);
				}
			}
		} finally {
			monitor.done();
		} 
		if (!errors.isEmpty()) {
			throw new CVSException(new MultiStatus(CVSProviderPlugin.ID, 0, 
					(IStatus[]) errors.toArray(new IStatus[errors.size()]), 
					Policy.bind("CVSSyncTreeSubscriber.1", getName()), null)); //$NON-NLS-1$
		}
	}

	public IStatus refresh(IResource resource, int depth, IProgressMonitor monitor) {
		monitor = Policy.monitorFor(monitor);
		try {
			// Take a guess at the work involved for refreshing the base and remote tree
			int baseWork = getCacheFileContentsHint() ? 10 : 30;
			int remoteWork = 100;
			monitor.beginTask(null, baseWork + remoteWork);
			IResource[] baseChanges = refreshBase(new IResource[] {resource}, depth, Policy.subMonitorFor(monitor, baseWork));
			IResource[] remoteChanges = refreshRemote(new IResource[] {resource}, depth, Policy.subMonitorFor(monitor, remoteWork));
			
			Set allChanges = new HashSet();
			allChanges.addAll(Arrays.asList(remoteChanges));
			allChanges.addAll(Arrays.asList(baseChanges));
			IResource[] changedResources = (IResource[]) allChanges.toArray(new IResource[allChanges.size()]);
			fireTeamResourceChange(TeamDelta.asSyncChangedDeltas(this, changedResources));
			return Status.OK_STATUS;
		} catch (TeamException e) {
			return new CVSStatus(IStatus.ERROR, Policy.bind("CVSSyncTreeSubscriber.2", resource.getFullPath().toString(), e.getMessage()), e); //$NON-NLS-1$
		} finally {
			monitor.done();
		} 
	}
	
	protected IResource[] refreshBase(IResource[] resources, int depth, IProgressMonitor monitor) throws TeamException {
		return new CVSRefreshOperation(getBaseSynchronizationCache(), null, getBaseTag()).refresh(resources, depth,  getCacheFileContentsHint(), monitor);
	}

	protected IResource[] refreshRemote(IResource[] resources, int depth, IProgressMonitor monitor) throws TeamException {
		return new CVSRefreshOperation(getRemoteSynchronizationCache(), getBaseSynchronizationCache(), getRemoteTag()).refresh(resources, depth,  getCacheFileContentsHint(), monitor);
	}
	
	/**
	 * Return the tag associated with the base tree. t is used by the refreshBase method.
	 */
	protected abstract CVSTag getRemoteTag();
	
	/**
	 * Return the tag associated with the base tree. t is used by the refreshRemote method.
	 */
	protected abstract CVSTag getBaseTag();

	/**
	 * Return whether the contents for remote resources will be required by the comparison
	 * criteria of the subscriber.
	 * @return boolean
	 */
	protected boolean getCacheFileContentsHint() {
		return getCurrentComparisonCriteria().usesFileContents();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#isSupervised(org.eclipse.core.resources.IResource)
	 */
	public boolean isSupervised(IResource resource) throws TeamException {
		try {
			RepositoryProvider provider = RepositoryProvider.getProvider(resource.getProject(), CVSProviderPlugin.getTypeId());
			if (provider == null) return false;
			// TODO: what happens for resources that don't exist?
			// TODO: is it proper to use ignored here?
			ICVSResource cvsThing = CVSWorkspaceRoot.getCVSResourceFor(resource);
			if (cvsThing.isIgnored()) {
				// An ignored resource could have an incoming addition (conflict)
				return hasRemote(resource);
			}
			return true;
		} catch (TeamException e) {
			// If there is no resource in coe this measn there is no local and no remote
			// so the resource is not supervised.
			if (e.getStatus().getCode() == IResourceStatus.RESOURCE_NOT_FOUND) {
				return false;
			}
			throw e;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.TeamSubscriber#isThreeWay()
	 */
	public boolean isThreeWay() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.SyncTreeSubscriber#initializeComparisonCriteria()
	 */
	protected void initializeComparisonCriteria() {
		// Do not use inherited as we want content comparison that checks revision numbers first
		
		// add revision number comparison and make it the default
		ComparisonCriteria revisionNumberComparator = new CVSRevisionNumberCompareCriteria();
		addComparisonCriteria(revisionNumberComparator);
		
		// Add the content comparisons
		ComparisonCriteria contentsComparator = new ContentComparisonCriteria(new ComparisonCriteria[] {revisionNumberComparator}, false /*consider whitespace */);
		ComparisonCriteria contentsComparatorIgnoreWhitespace = new ContentComparisonCriteria(new ComparisonCriteria[] {revisionNumberComparator}, true /* ignore whitespace */);
		addComparisonCriteria(contentsComparator);
		addComparisonCriteria(contentsComparatorIgnoreWhitespace);
		
		// Set the default
		setDefaultComparisonCriteria(revisionNumberComparator);
	}
	
	public ISubscriberResource getRemoteResource(IResource resource) throws TeamException {
		return getRemoteResource(resource, getRemoteSynchronizationCache());
	}

	public ISubscriberResource getBaseResource(IResource resource) throws TeamException {
		return getRemoteResource(resource, getBaseSynchronizationCache());
	}
	
	/**
	 * Return the synchronization cache that provides access to the base sychronization bytes.
	 */
	protected abstract SynchronizationCache getBaseSynchronizationCache();

	/**
	 * Return the synchronization cache that provides access to the base sychronization bytes.
	 */
	protected abstract SynchronizationCache getRemoteSynchronizationCache();
	
	private ISubscriberResource getRemoteResource(IResource resource, SynchronizationCache cache) throws TeamException {
		byte[] remoteBytes = cache.getSyncBytes(resource);
		if (remoteBytes == null) {
			// There is no remote handle for this resource
			return null;
		} else {
			// TODO: This code assumes that the type of the remote resource
			// matches that of the local resource. This may not be true.
			if (resource.getType() == IResource.FILE) {
				byte[] parentBytes = cache.getSyncBytes(resource.getParent());
				if (parentBytes == null) {
					CVSProviderPlugin.log(new CVSException( 
							Policy.bind("ResourceSynchronizer.missingParentBytesOnGet", getSyncName(cache).toString(), resource.getFullPath().toString())));
					// Assume there is no remote and the problem is a programming error
					return null;
				}
				return RemoteFile.fromBytes(resource, remoteBytes, parentBytes);
			} else {
				return RemoteFolder.fromBytes(resource, remoteBytes);
			}
		}
	}
	
	private String getSyncName(SynchronizationCache cache) {
		if (cache instanceof SynchronizationSyncBytesCache) {
			return ((SynchronizationSyncBytesCache)cache).getSyncName().toString();
		}
		return cache.getClass().getName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.helpers.SyncTreeSubscriber#hasRemote(org.eclipse.core.resources.IResource)
	 */
	protected boolean hasRemote(IResource resource) throws TeamException {
		return getRemoteSynchronizationCache().getSyncBytes(resource) != null;
	}

}
