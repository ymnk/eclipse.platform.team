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
package org.eclipse.team.core.subscribers.trees;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.ComparisonCriteria;
import org.eclipse.team.core.subscribers.TeamDelta;
import org.eclipse.team.core.subscribers.TeamSubscriber;
import org.eclipse.team.internal.core.Policy;

public abstract class TreeTeamSubscriber extends TeamSubscriber {
	
	private QualifiedName id;
	private String name;
	private String description;
	
	// options this subscriber supports for determining the sync state of resources
	private Map comparisonCriterias = new HashMap();
	private String defaultCriteria;
	
	public TreeTeamSubscriber(QualifiedName id, String name, String description) {
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
			throw new TeamException(id + " is not a valid comparison criteria");
		}
		this.defaultCriteria = id;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#getComparisonCriterias()
	 */
	public ComparisonCriteria[] getComparisonCriterias() {
		return (ComparisonCriteria[]) comparisonCriterias.values().toArray(new ComparisonCriteria[comparisonCriterias.size()]);
	}

	protected void addComparisonCriteria(ComparisonCriteria comparator) {
		comparisonCriterias.put(comparator.getId(), comparator);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.TeamSubscriber#members(org.eclipse.core.resources.IResource)
	 */
	public IResource[] members(IResource resource) throws TeamException {
		if(resource.getType() == IResource.FILE) {
			return new IResource[0];
		}	
		try {
			IResource[] members = ((IContainer)resource).members(true /* include phantoms */);
			List filteredMembers = new ArrayList(members.length);
			for (int i = 0; i < members.length; i++) {
				IResource member = members[i];
				
				if(member.isPhantom() && getRemoteTree().getRemoteHandle(member) == null) {
					continue;
				}
				
				if (isSupervised(resource)) {
					filteredMembers.add(member);
				}
			}
			return (IResource[]) filteredMembers.toArray(new IResource[filteredMembers.size()]);
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#refresh(org.eclipse.core.resources.IResource[], int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void refresh(IResource[] resources, int depth, IProgressMonitor monitor) throws TeamException {
		monitor = Policy.monitorFor(monitor);
		try {
			monitor.beginTask(null, 100);
			IResource[] remoteChanges = getRemoteTree().refresh(resources, depth, Policy.subMonitorFor(monitor, 60));
			IResource[] baseChanges = getBaseTree().refresh(resources, depth, Policy.subMonitorFor(monitor, 40));
		
			Set allChanges = new HashSet();
			allChanges.addAll(Arrays.asList(remoteChanges));
			allChanges.addAll(Arrays.asList(baseChanges));
			IResource[] changedResources = (IResource[]) allChanges.toArray(new IResource[allChanges.size()]);
			fireTeamResourceChange(TeamDelta.asSyncChangedDeltas(this, changedResources));
		} finally {
			monitor.done();
		} 
	}
	
	/**
	 * Return the synchronizer that provides the remote resources
	 */
	protected abstract RemoteHandleTree getRemoteTree();
	
	/**
	 * Return the synchronizer that provides the base resources
	 */
	protected abstract RemoteHandleTree getBaseTree();
}
