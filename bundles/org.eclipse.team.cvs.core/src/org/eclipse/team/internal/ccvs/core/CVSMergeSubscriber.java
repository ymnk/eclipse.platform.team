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
import org.eclipse.core.resources.ISynchronizer;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.syncinfo.RemoteSynchronizer;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSynchronizer;

/**
 * CVSMergeSubscriber
 */
public class CVSMergeSubscriber extends CVSSyncTreeSubscriber {

	private static final String ID_QUALIFIER = "org.eclipse.team.cvs";
	private static final String UNIQUE_ID_PREFIX = "merge-";
	
	private CVSTag start, end;
	private IResource[] roots;
	private RemoteSynchronizer remoteSynchronizer;
	private RemoteSynchronizer baseSynchronizer;

	static {
		// TODO: Temporary measure until pesistance of merge is provided
		flushOldMerges();
	}
	
	public static void flushOldMerges() {
		// XXX flush sync info for merge managers. This does not
		// support ongoing merges (subscriptions); we would need
		// some kind of lifecycle management that only flushes sync
		// info for merge managers that are not ongoing merges.
		ISynchronizer synchronizer = ResourcesPlugin.getWorkspace().getSynchronizer();
		QualifiedName[] syncPartners = synchronizer.getPartners();
		for(int i=0; i<syncPartners.length; i++) {
			if(syncPartners[i].getQualifier().equals(RemoteSynchronizer.SYNC_KEY_QUALIFIER)) {
				// this sync partner belongs to the VCM plug-in
				if(syncPartners[i].getLocalName().startsWith(UNIQUE_ID_PREFIX)) {
					// this sync partner does not belong to the sharing manager,
					// it must be a merge manager. Remove this sync partner,
					// which gets rid of its sync info.
					synchronizer.remove(syncPartners[i]);
				}
			}
		}
	}
	
	/**
	 * 
	 */
	private static QualifiedName getUniqueId() {
		String uniqueId = Long.toString(System.currentTimeMillis());
		return new QualifiedName(ID_QUALIFIER, UNIQUE_ID_PREFIX + uniqueId);
	}
	
	public CVSMergeSubscriber(IResource[] roots, CVSTag start, CVSTag end) {		
		super(getUniqueId(), "CVS Merge: " + start.getName() + " - " + end.getName(), "CVS Merge");
		this.start = start;
		this.end = end;
		this.roots = roots;
		initializeSynchronizers();
		// TODO: Is the merge subscriber interested in workspace sync info changes?
		// TODO: Do certain operations (e.g. replace with) invalidate a merge subscriber?
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.CVSWorkspaceSubscriber#initialize()
	 */
	private void initializeSynchronizers() {				
		QualifiedName id = getId();
		String syncKeyPrefix = id.getLocalName();
		remoteSynchronizer = new RemoteSynchronizer(syncKeyPrefix + end.getName(), end);
		baseSynchronizer = new RemoteSynchronizer(syncKeyPrefix + start.getName(), start);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.SyncTreeSubscriber#cancel()
	 */
	public void cancel() {
		remoteSynchronizer.dispose();
		baseSynchronizer.dispose();
		super.cancel();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.SyncTreeSubscriber#isCancellable()
	 */
	public boolean isCancellable() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.SyncTreeSubscriber#roots()
	 */
	public IResource[] roots() throws TeamException {
		return roots;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.CVSSyncTreeSubscriber#getRemoteSynchronizer()
	 */
	protected ResourceSynchronizer getRemoteSynchronizer() {
		return remoteSynchronizer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.CVSSyncTreeSubscriber#getBaseSynchronizer()
	 */
	protected ResourceSynchronizer getBaseSynchronizer() {
		return baseSynchronizer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.SyncTreeSubscriber#isSupervised(org.eclipse.core.resources.IResource)
	 */
	public boolean isSupervised(IResource resource) throws TeamException {
		return getBaseSynchronizer().getSyncBytes(resource) != null || getRemoteSynchronizer().getSyncBytes(resource) != null; 
	}
}
