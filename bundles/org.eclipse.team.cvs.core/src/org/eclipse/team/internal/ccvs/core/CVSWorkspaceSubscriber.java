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
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
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
import org.eclipse.team.internal.ccvs.core.syncinfo.OptimizedRemoteSynchronizer;
import org.eclipse.team.internal.ccvs.core.syncinfo.RemoteSynchronizer;

/**
 * CVSWorkspaceSubscriber
 */
public class CVSWorkspaceSubscriber extends SyncTreeSubscriber implements IResourceStateChangeListener {

	private static final byte[] NO_REMOTE = new byte[0];
	
	// describes this subscriber
	protected QualifiedName id;
	private String name;
	private String description;
	
	// options this subscriber supports for determining the sync state of resources
	protected Map comparisonCriterias = new HashMap();
	protected String defaultCriteria;
	
	private RemoteSynchronizer remoteSynchronizer;
	
	// qualified name for remote sync info
	private static final String REMOTE_RESOURCE_KEY = "remote-resource-key";
	
	CVSWorkspaceSubscriber() {
	}
	
	CVSWorkspaceSubscriber(QualifiedName id, String name, String description) {
		this.id = id;
		this.name = name;
		this.description = description;
		
		// install sync info participant
		remoteSynchronizer = new OptimizedRemoteSynchronizer(REMOTE_RESOURCE_KEY);
		
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
			return remoteSynchronizer.members(resource);
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#getRemoteResource(org.eclipse.core.resources.IResource)
	 */
	public IRemoteResource getRemoteResource(IResource resource) throws TeamException {
		return remoteSynchronizer.getRemoteResource(resource);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#getSyncInfo(org.eclipse.core.resources.IResource)
	 */
	public SyncInfo getSyncInfo(IResource resource, IProgressMonitor monitor) throws TeamException {	
		IRemoteResource remoteResource = getRemoteResource(resource);
		if(resource.getType() == IResource.FILE) {
			return new CVSSyncInfo(resource, CVSWorkspaceRoot.getBaseFor(resource), remoteResource, this, monitor);
		} else {
			// In CVS, folders do not have a base. Hence, the remotge is used as the base.
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
					remoteSynchronizer.collectChanges(resource, trees[i], depth, sub);
				} finally {
					sub.done();			 
				}
			}
		} finally {
			monitor.done();
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
				// TODO should use revision and tag to determine if remote is stale
				if (resource.getType() == IResource.FILE
						&& (resource.exists() || resource.isPhantom())) {
					remoteSynchronizer.removeSyncBytes(resource, IResource.DEPTH_ZERO);
				}
			} catch (CVSException e) {
				CVSProviderPlugin.log(e);
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
		try {
			remoteSynchronizer.removeSyncBytes(project, IResource.DEPTH_INFINITE);
		} catch (CVSException e) {
			CVSProviderPlugin.log(e);
		}
		TeamDelta delta = new TeamDelta(this, TeamDelta.PROVIDER_DECONFIGURED, project);
		fireTeamResourceChange(new TeamDelta[] {delta});
	}
	
	/**
	 * @param string
	 */
	protected void setDescription(String string) {
		description = string;
	}

	/**
	 * @param string
	 */
	protected void setName(String string) {
		name = string;
	}
}
