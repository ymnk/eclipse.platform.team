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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.sync.ComparisonCriteria;
import org.eclipse.team.core.sync.ContentComparisonCriteria;
import org.eclipse.team.core.sync.IRemoteResource;
import org.eclipse.team.core.sync.SyncInfo;

/**
 * CVSMergeSubscriber
 */
public class CVSMergeSubscriber extends CVSWorkspaceSubscriber {

	private CVSTag start, end;
	private IResource[] resources;
	
	private String REMOTE_ID = "remote-id";
	private String BASE_ID = "base-id";
	private QualifiedName END_KEY;
	private QualifiedName START_KEY;

	public CVSMergeSubscriber(QualifiedName id, IResource[] resources, CVSTag start, CVSTag end) {		
		this.id = id;
		this.start = start;
		this.end = end;
		this.resources = resources;
		setName("CVS Merge: " + start.getName() + " - " + end.getName());
		setDescription("CVS Merge");
		initialize();		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.CVSWorkspaceSubscriber#initialize()
	 */
	protected void initialize() {				
		// setup comparison criteria
		ComparisonCriteria revisionNumberComparator = new CVSRevisionNumberCompareCriteria();
		ComparisonCriteria contentsComparator = new ContentComparisonCriteria(new ComparisonCriteria[] {revisionNumberComparator}, false /*consider whitespace */);
		ComparisonCriteria contentsComparatorIgnoreWhitespace = new ContentComparisonCriteria(new ComparisonCriteria[] {revisionNumberComparator}, true /* ignore whitespace */);
		
		comparisonCriterias.put(revisionNumberComparator.getId(), revisionNumberComparator);
		comparisonCriterias.put(contentsComparator.getId(), contentsComparator);
		comparisonCriterias.put(contentsComparatorIgnoreWhitespace.getId(), contentsComparatorIgnoreWhitespace);
		
		// default
		defaultCriteria = contentsComparatorIgnoreWhitespace.getId();
		
		// setup sync tree partners
		QualifiedName id = getId();
		END_KEY = new QualifiedName(id.getQualifier(), id.getLocalName() + REMOTE_ID);
		START_KEY = new QualifiedName(id.getQualifier(), id.getLocalName() + BASE_ID);
		
		//		install sync info participant
		getSynchronizer().add(getRemoteSyncName());
		getSynchronizer().add(getBaseSyncName());
	}
	
	
	private QualifiedName getBaseSyncName() {
		return START_KEY;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.CVSWorkspaceSubscriber#getRemoteSyncName()
	 */
	protected QualifiedName getRemoteSyncName() {
		return END_KEY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.SyncTreeSubscriber#cancel()
	 */
	public void cancel() {
		// TODO Auto-generated method stub
		super.cancel();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.SyncTreeSubscriber#getRemoteResource(org.eclipse.core.resources.IResource)
	 */
	public IRemoteResource getRemoteResource(IResource resource)	throws TeamException {
		// TODO Auto-generated method stub
		return super.getRemoteResource(resource);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.SyncTreeSubscriber#getSyncInfo(org.eclipse.core.resources.IResource, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public SyncInfo getSyncInfo(IResource resource, IProgressMonitor monitor) throws TeamException {
		return new SyncInfo(resource, null, null, this, monitor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.SyncTreeSubscriber#isCancellable()
	 */
	public boolean isCancellable() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.SyncTreeSubscriber#members(org.eclipse.core.resources.IResource)
	 */
	public IResource[] members(IResource resource) throws TeamException {
		return new IResource[0];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.SyncTreeSubscriber#refresh(org.eclipse.core.resources.IResource[], int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void refresh(IResource[] resources, int depth, IProgressMonitor monitor) throws TeamException {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.SyncTreeSubscriber#roots()
	 */
	public IResource[] roots() throws TeamException {
		return resources;
	}
}
