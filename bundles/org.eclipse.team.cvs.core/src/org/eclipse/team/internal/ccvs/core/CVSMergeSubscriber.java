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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.syncinfo.RemoteSynchronizer;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSynchronizer;

/**
 * CVSMergeSubscriber
 */
public class CVSMergeSubscriber extends CVSSyncTreeSubscriber {

	private CVSTag start, end;
	private IResource[] roots;
	private RemoteSynchronizer remoteSynchronizer;
	private RemoteSynchronizer baseSynchronizer;

	public CVSMergeSubscriber(QualifiedName id, IResource[] roots, CVSTag start, CVSTag end) {		
		super(id, "CVS Merge: " + start.getName() + " - " + end.getName(), "CVS Merge");
		this.start = start;
		this.end = end;
		this.roots = roots;
		initializeSynchronizers();		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.CVSWorkspaceSubscriber#initialize()
	 */
	private void initializeSynchronizers() {				
		QualifiedName id = getId();
		String syncKeyPrefix = id.getLocalName();
		remoteSynchronizer = new RemoteSynchronizer(syncKeyPrefix + end.getName());
		baseSynchronizer = new RemoteSynchronizer(syncKeyPrefix + start.getName());
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
	 * @see org.eclipse.team.core.sync.SyncTreeSubscriber#refresh(org.eclipse.core.resources.IResource[], int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void refresh(IResource[] resources, int depth, IProgressMonitor monitor) throws TeamException {
		int work = 100 * resources.length;
		monitor.beginTask(null, work);
		try {
			for (int i = 0; i < resources.length; i++) {
				IResource resource = resources[i];	
				
				// build the remote tree
				// TODO: We should really only need to do the start tree once!
				ICVSRemoteResource startTree = buildRemoteTree(resource, start, depth, Policy.subMonitorFor(monitor, 40));
				ICVSRemoteResource endTree = buildRemoteTree(resource, end, depth, Policy.subMonitorFor(monitor, 40));
				
				// update the known remote handles 
				IProgressMonitor sub = Policy.infiniteSubMonitorFor(monitor, 20);
				try {
					sub.beginTask(null, 512);
					Set allChanges = new HashSet();
					
					// TODO: We should really only need to do the start tree once!
					baseSynchronizer.removeSyncBytes(resource, IResource.DEPTH_INFINITE);
					baseSynchronizer.collectChanges(resource, startTree, depth, sub);
					allChanges.addAll(Arrays.asList(baseSynchronizer.getChangedResources()));
					
					remoteSynchronizer.removeSyncBytes(resource, IResource.DEPTH_INFINITE);
					remoteSynchronizer.collectChanges(resource, endTree, depth, sub);
					allChanges.addAll(Arrays.asList(remoteSynchronizer.getChangedResources()));

					fireSyncChanged((IResource[]) allChanges.toArray(new IResource[allChanges.size()]));
				} finally {
					sub.done();
					remoteSynchronizer.resetChanges();	 
				}
			}
		} finally {
			monitor.done();
		}
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
