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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ISynchronizer;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.sync.ComparisonCriteria;
import org.eclipse.team.core.sync.ContentComparisonCriteria;
import org.eclipse.team.core.sync.IRemoteResource;
import org.eclipse.team.core.sync.SyncInfo;
import org.eclipse.team.core.sync.SyncTreeSubscriber;
import org.eclipse.team.core.sync.TeamDelta;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFile;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFolder;
import org.eclipse.team.internal.ccvs.core.resources.RemoteResource;
import org.eclipse.team.internal.ccvs.core.util.Util;
import org.eclipse.team.internal.core.Assert;

/**
 * CVSWorkspaceSubscriber
 */
public class CVSWorkspaceSubscriber extends SyncTreeSubscriber implements IResourceStateChangeListener {

	private static final byte[] NO_REMOTE = new byte[0];
	
	// describes this subscriber
	private QualifiedName id;
	private String name;
	private String description;
	
	// options this subscriber supports for determining the sync state of resources
	private Map comparisonCriterias = new HashMap();
	private String defaultCriteria;
	
	// qualified name for remote sync info
	private QualifiedName REMOTE_RESOURCE_KEY = new QualifiedName("org.eclipse.team.cvs", "remote-resource-key");
	
	CVSWorkspaceSubscriber(QualifiedName id, String name, String description) {
		this.id = id;
		this.name = name;
		this.description = description;
		
		// install sync info participant
		getSynchronizer().add(getRemoteSyncName());
		
		// setup comparison criteria
		ComparisonCriteria revisionNumberComparator = new CVSRevisionNumberCompareCriteria();
		ComparisonCriteria contentsComparator = new ContentComparisonCriteria(new ComparisonCriteria[] {revisionNumberComparator}, false /*consider whitespace */);
		ComparisonCriteria contentsComparatorIgnoreWhitespace = new ContentComparisonCriteria(new ComparisonCriteria[] {revisionNumberComparator}, true /* ignore whitespace */);
		
		comparisonCriterias.put(revisionNumberComparator.getId(), revisionNumberComparator);
		comparisonCriterias.put(contentsComparator.getId(), contentsComparator);
		comparisonCriterias.put(contentsComparatorIgnoreWhitespace.getId(), contentsComparatorIgnoreWhitespace);
		
		// default
		defaultCriteria = revisionNumberComparator.getId();
		
		// TODO: temporary proxy for CVS events
		CVSProviderPlugin.addResourceStateChangeListener(this); 
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
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#isSupervised(org.eclipse.core.resources.IResource)
	 */
	public boolean isSupervised(IResource resource) {
		RepositoryProvider provider = RepositoryProvider.getProvider(resource.getProject(), CVSProviderPlugin.getTypeId());
		return provider != null;
	}

	/* 
	 * Return the list of projects shared with a CVS team provider.
	 * 
	 * [Issue : this will have to change when folders can be shared with
	 * a team provider instead of the current project restriction]
	 * (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#roots()
	 */
	public IResource[] roots() throws TeamException {
		List result = new ArrayList();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			if(project.isOpen()) {
				RepositoryProvider provider = RepositoryProvider.getProvider(project, CVSProviderPlugin.getTypeId());
				if(provider != null) {
					result.add(project);
				}
			}
		}
		return (IProject[]) result.toArray(new IProject[result.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#members(org.eclipse.core.resources.IResource)
	 */
	public IResource[] members(IResource resource) throws TeamException {
		try {
			return doMembers(resource);
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#getRemoteResource(org.eclipse.core.resources.IResource)
	 */
	public IRemoteResource getRemoteResource(IResource resource)	throws TeamException {
		try {
			byte[] remoteBytes = getRemoteSyncBytes(resource);
			if (remoteBytes == null) {
				// The remote has never been fetched so use the base
				if(resource.getType() != IResource.FILE && !resource.exists()) {
					// In CVS, a folder with no local or remote cannot have a base either
					return null;
				}
				// return the base handle taken from the Entries file
				return CVSWorkspaceRoot.getBaseFor(resource);	
			} else if (remoteDoesNotExist(remoteBytes)) {
				// The remote is known to not exist
				return null;
			} else {
				// TODO: This code assumes that the type of the remote resource
				// matches that of the local resource. This may not be true.
				// TODO: This is rather complicated. There must be a better way!
				if (resource.getType() == IResource.FILE) {
					return RemoteFile.fromBytes(resource, remoteBytes, getRemoteSyncBytes(resource.getParent()));
				} else {
					return RemoteFolder.fromBytes((IContainer)resource, remoteBytes);
				}
			}
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#getSyncInfo(org.eclipse.core.resources.IResource)
	 */
	public SyncInfo getSyncInfo(IResource resource, IProgressMonitor monitor) throws TeamException {	
		IRemoteResource remoteResource = getRemoteResource(resource);
		if(resource.getType() == IResource.FILE) {
			return new CVSSyncInfo(resource, CVSWorkspaceRoot.getBaseFor(resource), remoteResource, this, monitor);
		} else {
			return new CVSSyncInfo(resource, remoteResource, remoteResource, this, monitor);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#refresh(org.eclipse.core.resources.IResource[], int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void refresh(IResource[] resources, int depth, IProgressMonitor monitor) throws TeamException {
		ICVSRemoteResource[] trees = new ICVSRemoteResource[resources.length];
		int work = 100 * resources.length;
		monitor.beginTask(null, work);
		try {
			for (int i = 0; i < trees.length; i++) {
				IResource resource = resources[i];	
				
				// build the remote tree
				// TODO: we are currently ignoring the depth parameter because the build remote tree is
				// by default deep!
				trees[i] = CVSWorkspaceRoot.getRemoteTree(
								resource, 
								null /* build tree based on tags found in the local sync info */ , 
								Policy.subMonitorFor(monitor, 70));
				
				// update the known remote handles 
				IProgressMonitor sub = Policy.infiniteSubMonitorFor(monitor, 30);
				try {
					sub.beginTask(null, 512);
					collectChanges(resource, trees[i], depth, sub);
				} finally {
					sub.done();			 
				}
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * @param resource
	 */
	private void collectChanges(IResource local, ICVSRemoteResource remote, int depth, IProgressMonitor monitor) throws TeamException {
		byte[] remoteBytes;
		if (remote != null) {
			remoteBytes = ((RemoteResource)remote).getSyncBytes();
		} else {
			remoteBytes = NO_REMOTE;
		}
		try {
				
			// TODO: when does sync information get updated? For example, on commit. And
			// when does it get cleared.
			getSynchronizer().setSyncInfo(getRemoteSyncName(), local, remoteBytes);
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
		if (depth == IResource.DEPTH_ZERO) return;
		Map children = mergedMembers(local, remote, monitor);	
		for (Iterator it = children.keySet().iterator(); it.hasNext();) {
			IResource localChild = (IResource) it.next();
			ICVSRemoteResource remoteChild = (ICVSRemoteResource)children.get(localChild);
			collectChanges(localChild, remoteChild, 
				depth == IResource.DEPTH_INFINITE ? IResource.DEPTH_INFINITE : IResource.DEPTH_ZERO, 
				monitor);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#getComparisonCriterias()
	 */
	public ComparisonCriteria[] getComparisonCriterias() {
		return (ComparisonCriteria[]) comparisonCriterias.values().toArray(new ComparisonCriteria[comparisonCriterias.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#getCurrentComparisonCriteria()
	 */
	public ComparisonCriteria getCurrentComparisonCriteria() {		
		return (ComparisonCriteria)comparisonCriterias.get(defaultCriteria);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#setCurrentComparisonCriteria(java.lang.String)
	 */
	public void setCurrentComparisonCriteria(String id) throws TeamException {
		if(! comparisonCriterias.containsKey(id)) {
			throw new CVSException(id + " is not a valid comparison criteria");
		}
		this.defaultCriteria = id;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#isThreeWay()
	 */
	public boolean isThreeWay() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#isCancellable()
	 */
	public boolean isCancellable() {
		// this is the default subscriber for all shared CVS things in the workspace. You can't
		// cancel it. Instead users will disconnect resources from CVS control.
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#cancel()
	 */
	public void cancel() {
		// noop
	}
	
	protected IResource[] doMembers(IResource resource) throws TeamException, CoreException {
		if(resource.getType() == IResource.FILE) {
			return new IResource[0];
		}	
		
		// TODO: will have to filter and return only the CVS phantoms.
		IResource[] members = ((IContainer)resource).members(true /* include phantoms */);
		List filteredMembers = new ArrayList(members.length);
		for (int i = 0; i < members.length; i++) {
			IResource r = members[i];
			
			// TODO: consider that there may be several sync states on this resource. There
			// should instead be a method to check for the existance of a set of sync types on
			// a resource.
			if(! r.exists()) {
				if(CVSWorkspaceRoot.getCVSResourceFor(r).getSyncInfo() == null) { 
					if(getRemoteSyncBytes(r) == null) {
						continue;
					}
				}
			}
			
			ICVSResource cvsThing = CVSWorkspaceRoot.getCVSResourceFor(r);
			if( !cvsThing.isIgnored()) {
				filteredMembers.add(r);
			}
		}
		return (IResource[]) filteredMembers.toArray(new IResource[filteredMembers.size()]);
	}

	protected QualifiedName getRemoteSyncName() {
		return REMOTE_RESOURCE_KEY;
	}
	
	protected ISynchronizer getSynchronizer() {
		return ResourcesPlugin.getWorkspace().getSynchronizer();
	}
	
	/*
	 * Get the sync bytes for the remote resource
	 */
	private byte[] getRemoteSyncBytes(IResource r) throws CoreException {
		return getSynchronizer().getSyncInfo(getRemoteSyncName(), r);
	}
	
	/*
	 * Return true if the given bytes indocate that the remote does not exist.
	 * The provided byte array must not be null;
	 */
	private boolean remoteDoesNotExist(byte[] remoteBytes) {
		Assert.isNotNull(remoteBytes);
		return Util.equals(remoteBytes, NO_REMOTE);
	}
	
	protected Map mergedMembers(IResource local, IRemoteResource remote, IProgressMonitor progress) throws TeamException {
	
		// {IResource -> IRemoteResource}
		Map mergedResources = new HashMap();
		
		IRemoteResource[] remoteChildren =
			remote != null ? remote.members(progress) : new IRemoteResource[0];
		
		IResource[] localChildren;			
		try {	
			if( local.getType() != IResource.FILE && local.exists() ) {
				localChildren = ((IContainer)local).members();
			} else {
				localChildren = new IResource[0];
			}
		} catch(CoreException e) {
			throw new TeamException(e.getStatus());
		}
		
		if (remoteChildren.length > 0 || localChildren.length > 0) {
			List syncChildren = new ArrayList(10);
			Set allSet = new HashSet(20);
			Map localSet = null;
			Map remoteSet = null;

			if (localChildren.length > 0) {
				localSet = new HashMap(10);
				for (int i = 0; i < localChildren.length; i++) {
					IResource localChild = localChildren[i];
					String name = localChild.getName();
					localSet.put(name, localChild);
					allSet.add(name);
				}
			}

			if (remoteChildren.length > 0) {
				remoteSet = new HashMap(10);
				for (int i = 0; i < remoteChildren.length; i++) {
					IRemoteResource remoteChild = remoteChildren[i];
					String name = remoteChild.getName();
					remoteSet.put(name, remoteChild);
					allSet.add(name);
				}
			}
		
			Iterator e = allSet.iterator();
			while (e.hasNext()) {
				String keyChildName = (String) e.next();

				if (progress != null) {
					if (progress.isCanceled()) {
						throw new OperationCanceledException();
					}
					// XXX show some progress?
				}

				IResource localChild =
					localSet != null ? (IResource) localSet.get(keyChildName) : null;

				IRemoteResource remoteChild =
					remoteSet != null ? (IRemoteResource) remoteSet.get(keyChildName) : null;
				
				if (localChild == null) {
					// there has to be a remote resource available if we got this far
					Assert.isTrue(remoteChild != null);
					boolean isContainer = remoteChild.isContainer();				
					localChild = getResourceChild(local /* parent */, keyChildName, isContainer);
				}
				mergedResources.put(localChild, remoteChild);				
			}
		}
		return mergedResources;
	}
	
	/*
	 * Returns a handle to a non-existing resource.
	 */
	private IResource getResourceChild(IResource parent, String childName, boolean isContainer) {
		if (parent.getType() == IResource.FILE) {
			return null;
		}
		if (isContainer) {
			return ((IContainer) parent).getFolder(new Path(childName));
		} else {
			return ((IContainer) parent).getFile(new Path(childName));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.IResourceStateChangeListener#resourceSyncInfoChanged(org.eclipse.core.resources.IResource[])
	 */
	public void resourceSyncInfoChanged(IResource[] changedResources) {
		
		// TODO: hack for clearing the remote state when anything to the resource
		// sync is changed. Should be able to set the *right* remote/base based on
		// the sync being set.
		// TODO: This will throw exceptions if performed during the POST_CHANGE delta phase!!!
		for (int i = 0; i < changedResources.length; i++) {
			IResource resource = changedResources[i];
			try {
				if (resource.exists() || resource.isPhantom()) {
					getSynchronizer().flushSyncInfo(getRemoteSyncName(), resource, IResource.DEPTH_ZERO);
				}
			} catch (CoreException e) {
				CVSProviderPlugin.log(CVSException.wrapException(e));
			}
		}		
		
		TeamDelta[] deltas = new TeamDelta[changedResources.length];
		for (int i = 0; i < changedResources.length; i++) {
			IResource resource = changedResources[i];
			deltas[i] = new TeamDelta(this, TeamDelta.SYNC_CHANGED, resource);
		}
		fireTeamResourceChange(deltas); 
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.IResourceStateChangeListener#resourceModified(org.eclipse.core.resources.IResource[])
	 */
	public void resourceModified(IResource[] changedResources) {
		// TODO: This is only ever called from a delta POST_CHANGE
		// which causes problems since the workspace tree is closed
		// for modification and we flush the sync info in resourceSyncInfoChanged
		
		// Since the listeners of the Subscriber will also listen to deltas
		// we don't need to propogate this.
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.IResourceStateChangeListener#projectConfigured(org.eclipse.core.resources.IProject)
	 */
	public void projectConfigured(IProject project) {
		TeamDelta delta = new TeamDelta(this, TeamDelta.PROVIDER_CONFIGURED, project);
		fireTeamResourceChange(new TeamDelta[] {delta});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.IResourceStateChangeListener#projectDeconfigured(org.eclipse.core.resources.IProject)
	 */
	public void projectDeconfigured(IProject project) {
		TeamDelta delta = new TeamDelta(this, TeamDelta.PROVIDER_DECONFIGURED, project);
		fireTeamResourceChange(new TeamDelta[] {delta});
	}
}
