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
import org.eclipse.team.core.sync.ISyncTreeSubscriber;
import org.eclipse.team.core.sync.SyncInfo;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.RemoteResource;
import org.eclipse.team.internal.core.Assert;

/**
 * CVSWorkspaceSubscriber
 */
public class CVSWorkspaceSubscriber implements ISyncTreeSubscriber {

	// describes this subscriber
	private String id;
	private String name;
	private String description;
	
	// options this subscriber supports for determining the sync state of resources
	private Map comparisonCriterias = new HashMap();
	private String defaultCriteria;
	
	// qualified name for remote sync info
	private QualifiedName REMOTE_RESOURCE_KEY = new QualifiedName("org.eclipse.team.cvs", "remote-resource-key");
	
	CVSWorkspaceSubscriber(String id, String name, String description) {
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
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#getId()
	 */
	public String getId() {
		return id.toString();
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
		ISynchronizer s = getSynchronizer();
		try {
			byte[] remoteBytes = s.getSyncInfo(getRemoteSyncName(), resource);
			if(remoteBytes != null) {
				return RemoteResource.fromBytes(resource, remoteBytes);
			} else {
				// return the base handle taken from the Entries file
				 return CVSWorkspaceRoot.getBaseFor(resource);				
			}
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#getSyncInfo(org.eclipse.core.resources.IResource)
	 */
	public SyncInfo getSyncInfo(IResource resource, IProgressMonitor monitor) {		
		try {
			return SyncInfo.computeSyncKind(resource, CVSWorkspaceRoot.getBaseFor(resource), getRemoteResource(resource), this, monitor);
		}  catch (TeamException e) {
			// log this error?
			return null;
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
					collectChanges(resource, trees[i], sub);
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
	private void collectChanges(IResource local, ICVSRemoteResource remote, IProgressMonitor monitor) throws TeamException {
		Map children = mergedMembers(local, remote, monitor);	
		for (Iterator it = children.keySet().iterator(); it.hasNext();) {
			IResource localChild = (IResource) it.next();
			IRemoteResource remoteChild = (IRemoteResource)children.get(localChild);
			if(remoteChild != null) {
				byte[] remoteBytes = ((RemoteResource)remoteChild).getSyncBytes();
				try {
					getSynchronizer().setSyncInfo(getRemoteSyncName(), localChild, remoteBytes);
				} catch (CoreException e) {
					CVSException.wrapException(e);
				}
			}
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
			// should instead me a method to check for the existance of a set of sync types on
			// a resource.
			if(! r.exists()) {
				if(getSynchronizer().getSyncInfo(getRemoteSyncName(), r) == null) {
					continue;
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
}
