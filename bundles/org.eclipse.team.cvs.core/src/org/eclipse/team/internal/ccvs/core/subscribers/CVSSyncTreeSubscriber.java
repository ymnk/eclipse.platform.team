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
package org.eclipse.team.internal.ccvs.core.subscribers;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.ComparisonCriteria;
import org.eclipse.team.core.subscribers.ContentComparisonCriteria;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.core.subscribers.trees.TreeTeamSubscriber;
import org.eclipse.team.core.sync.IRemoteResource;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.Policy;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.core.TeamPlugin;

/**
 * This class provides common funtionality for three way sychronizing
 * for CVS.
 */
public abstract class CVSSyncTreeSubscriber extends TreeTeamSubscriber {
	
	CVSSyncTreeSubscriber(QualifiedName id, String name, String description) {
		super(id, name, description);
		initializeComparisonCriteria();
	}

	public IRemoteResource getRemoteResource(IResource resource) throws TeamException {
		return getRemoteTree().getRemoteHandle(resource);
	}

	public IRemoteResource getBaseResource(IResource resource) throws TeamException {
		return getBaseTree().getRemoteHandle(resource);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#getSyncInfo(org.eclipse.core.resources.IResource)
	 */
	public SyncInfo getSyncInfo(IResource resource, IProgressMonitor monitor) throws TeamException {
		if (!isSupervised(resource)) return null;
		IRemoteResource remoteResource = getRemoteResource(resource);
		if(resource.getType() == IResource.FILE) {
			IRemoteResource baseResource = getBaseResource(resource);
			return getSyncInfo(resource, baseResource, remoteResource, monitor);
		} else {
			// In CVS, folders do not have a base. Hence, the remote is used as the base.
			return getSyncInfo(resource, remoteResource, remoteResource, monitor);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#isSupervised(org.eclipse.core.resources.IResource)
	 */
	public boolean isSupervised(IResource resource) throws TeamException {
		RepositoryProvider provider = RepositoryProvider.getProvider(resource.getProject(), CVSProviderPlugin.getTypeId());
		if (provider == null) return false;
		// TODO: what happens for resources that don't exist?
		// TODO: is it proper to use ignored here?
		ICVSResource cvsThing = CVSWorkspaceRoot.getCVSResourceFor(resource);
		if (cvsThing.isIgnored()) {
			// An ignored resource could have an incoming addition (conflict)
			return getRemoteResource(resource) != null;
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.TeamSubscriber#isThreeWay()
	 */
	public boolean isThreeWay() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.TeamSubscriber#isCancellable()
	 */
	public boolean isCancellable() {
		return false;
	}
	
	/**
	 * Method invoked from the constructor to initialize the comparison criteria
	 * and the default criteria.
	 * This method can be overriden by subclasses.
	 */
	protected void initializeComparisonCriteria() {				
		// setup comparison criteria
		ComparisonCriteria revisionNumberComparator = new CVSRevisionNumberCompareCriteria();
		ComparisonCriteria contentsComparator = new ContentComparisonCriteria(new ComparisonCriteria[] {revisionNumberComparator}, false /*consider whitespace */);
		ComparisonCriteria contentsComparatorIgnoreWhitespace = new ContentComparisonCriteria(new ComparisonCriteria[] {revisionNumberComparator}, true /* ignore whitespace */);
		
		addComparisonCriteria(revisionNumberComparator);
		addComparisonCriteria(contentsComparator);
		addComparisonCriteria(contentsComparatorIgnoreWhitespace);
		
		try {
			// default
			setCurrentComparisonCriteria(revisionNumberComparator.getId());
		} catch (TeamException e) {
			TeamPlugin.log(e);			
		}
	}
	
	protected SyncInfo getSyncInfo(IResource local, IRemoteResource base, IRemoteResource remote, IProgressMonitor monitor) throws TeamException {
		try {
			monitor = Policy.monitorFor(monitor);
			monitor.beginTask(null, 100);
			return new CVSSyncInfo(local, base, remote, this, Policy.subMonitorFor(monitor, 100));
		} finally {
			monitor.done();
		}
	}
}
