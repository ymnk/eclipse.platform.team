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
package org.eclipse.team.core.subscribers;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.core.Policy;

/**
 * A specialized TeamSubscriber that ustilizes a RemoteSynchronizer to store the
 * base and remote sync information.
 */
public abstract class SyncTreeSubscriber extends TeamSubscriber {

	protected Map comparisonCriterias = new HashMap();
	protected String selectedCriteria;

	/**
	 * Method invoked from the constructor to initialize the comparison criteria
	 * and the default criteria. This method can be overriden by subclasses.
	 * This methods registers two content comparators (as-is and ignore whitespace)
	 * And sets the default to the as-is content compare. Subclass may override entirely
	 * or just to add addition comparison criteria and/pr change the default by invoking
	 * <code>setDefaultComparisonCriteria(ComparisonCriteria)</code>.
	 */
	protected void initializeComparisonCriteria() {				
		// setup content comparison criteria
		ComparisonCriteria contentsComparator = new ContentComparisonCriteria(new ComparisonCriteria[] {}, false /*consider whitespace */);
		ComparisonCriteria contentsComparatorIgnoreWhitespace = new ContentComparisonCriteria(new ComparisonCriteria[] {}, true /* ignore whitespace */);
		
		addComparisonCriteria(contentsComparator);
		addComparisonCriteria(contentsComparatorIgnoreWhitespace);
		
		setDefaultComparisonCriteria(contentsComparator);
	}

	/**
	 * Set the default criteria. This should only be invoked 
	 * from the <code>initializeComparisonCriteria</code> method
	 * by subclasses after they have invoked the inherited overriden method.
	 * @param contentsComparator the default compare criteria
	 */
	protected void setDefaultComparisonCriteria(ComparisonCriteria comparator) {
		selectedCriteria = comparator.getId();
	}

	/**
	 * Add the comparison criteria to the subscriber
	 * 
	 * @param comparator
	 */
	protected void addComparisonCriteria(ComparisonCriteria comparator) {
		comparisonCriterias.put(comparator.getId(), comparator);
	}

	public void setCurrentComparisonCriteria(String id) throws TeamException {
		if(! comparisonCriterias.containsKey(id)) {
			throw new TeamException("{0} is not a valid comparison criteria for subscriber {1}" +  id + getDescription());
		}
		this.selectedCriteria = id;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#getCurrentComparisonCriteria()
	 */
	public ComparisonCriteria getCurrentComparisonCriteria() {		
		return (ComparisonCriteria)comparisonCriterias.get(selectedCriteria);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#getComparisonCriterias()
	 */
	public ComparisonCriteria[] getComparisonCriterias() {
		return (ComparisonCriteria[]) comparisonCriterias.values().toArray(new ComparisonCriteria[comparisonCriterias.size()]);
	}

	public ISubscriberResource getRemoteResource(IResource resource) throws TeamException {
		return getRemoteResourceTree().getRemoteResource(resource);
	}

	public ISubscriberResource getBaseResource(IResource resource) throws TeamException {
		return getBaseResourceTree().getRemoteResource(resource);
	}

	/**
	 * Return the synchronizer that provides the remote resources
	 */
	protected abstract SubscriberResourceTree getRemoteResourceTree();

	/**
	 * Return the synchronizer that provides the base resources
	 */
	protected abstract SubscriberResourceTree getBaseResourceTree();

	public SyncInfo getSyncInfo(IResource resource, IProgressMonitor monitor) throws TeamException {
		if (!isSupervised(resource)) return null;
		ISubscriberResource remoteResource = getRemoteResource(resource);
		if(resource.getType() == IResource.FILE) {
			ISubscriberResource baseResource = getBaseResource(resource);
			return getSyncInfo(resource, baseResource, remoteResource, monitor);
		} else {
			// In CVS, folders do not have a base. Hence, the remote is used as the base.
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
			SyncInfo info = new SyncInfo(local, base, remote, this, Policy.subMonitorFor(monitor, 100));
			return info;
		} finally {
			monitor.done();
		}
	}

	public IResource[] members(IResource resource) throws TeamException {
		if(resource.getType() == IResource.FILE) {
			return new IResource[0];
		}	
		try {
			// Filter and return only phantoms associated with the remote synchronizer.
			IResource[] members;
			try {
				members = ((IContainer)resource).members(true /* include phantoms */);
			} catch (CoreException e) {
				if (!isSupervised(resource) || e.getStatus().getCode() == IResourceStatus.RESOURCE_NOT_FOUND) {
					// The resource is no longer supervised or doesn't exist in any form
					// so ignore the exception and return that there are no members
					return new IResource[0];
				}
				throw e;
			}
			List filteredMembers = new ArrayList(members.length);
			for (int i = 0; i < members.length; i++) {
				IResource member = members[i];
				
				// TODO: consider that there may be several sync states on this resource. There
				// should instead be a method to check for the existance of a set of sync types on
				// a resource.
				if(member.isPhantom() && !getRemoteResourceTree().hasRemote(member)) {
					continue;
				}
				
				// TODO: Is this a valid use of isSupervised
				if (isSupervised(resource)) {
					filteredMembers.add(member);
				}
			}
			return (IResource[]) filteredMembers.toArray(new IResource[filteredMembers.size()]);
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}
	}
}
