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
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.ContentComparisonCriteria;
import org.eclipse.team.internal.ccvs.core.syncinfo.RemoteSynchronizer;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSynchronizer;
import org.eclipse.team.internal.core.VestigeConfigurationItem;

/**
 * A CVSMergeSubscriber is responsible for maintaining the remote trees for a merge into
 * the workspace. The remote trees represent the CVS revisions of the start and end
 * points (version or branch) of the merge.
 * 
 * This subscriber stores the remote handles in the resource tree sync info slot. When
 * the merge is cancelled this sync info is cleared.
 * 
 * A merge can persist between workbench sessions and thus can be used as an
 * ongoing merge.
 * 
 * TODO: Is the merge subscriber interested in workspace sync info changes?
 *	TODO: Do certain operations (e.g. replace with) invalidate a merge subscriber?
 */
public class CVSMergeSubscriber extends CVSSyncTreeSubscriber {

	private static final String ID_QUALIFIER = "org.eclipse.team.cvs";
	private static final String UNIQUE_ID_PREFIX = "merge-";
	
	private CVSTag start, end;
	private IResource[] roots;
	private RemoteSynchronizer remoteSynchronizer;
	private RemoteSynchronizer baseSynchronizer;

	private static QualifiedName getUniqueId() {
		String uniqueId = Long.toString(System.currentTimeMillis());
		return new QualifiedName(ID_QUALIFIER, UNIQUE_ID_PREFIX + uniqueId);
	}
	
	public CVSMergeSubscriber(IResource[] roots, CVSTag start, CVSTag end) {		
		super(getUniqueId(), "CVS Merge: " + start.getName() + " to " + end.getName(), "CVS Merge");
		this.start = start;
		this.end = end;
		this.roots = roots;
		initialize(null /* no default base */, null /* no default remote */);
	}
	
	public CVSMergeSubscriber(IResource[] roots, CVSTag start, CVSTag end, ICVSRemoteResource base, ICVSRemoteResource remote) {		
		super(getUniqueId(), "CVS Merge: " + start.getName() + " to " + end.getName(), "CVS Merge");
		this.start = start;
		this.end = end;
		this.roots = roots;
		initialize(base, remote);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.CVSWorkspaceSubscriber#initialize()
	 */
	private void initialize(ICVSRemoteResource base, ICVSRemoteResource remote) {				
		QualifiedName id = getId();
		String syncKeyPrefix = id.getLocalName();
		remoteSynchronizer = new RemoteSynchronizer(syncKeyPrefix + end.getName(), end, base);
		baseSynchronizer = new RemoteSynchronizer(syncKeyPrefix + start.getName(), start, remote);
		
		try {
			setCurrentComparisonCriteria(ContentComparisonCriteria.ID_IGNORE_WS);
		} catch (TeamException e) {
			// use the default but log an exception because the content comparison should
			// always be available.
			CVSProviderPlugin.log(e);
		}
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.SyncTreeSubscriber#saveState(org.eclipse.team.internal.core.VestigeConfigurationItem)
	 */
	public void saveState(VestigeConfigurationItem state) {
		super.saveState(state);
		state.setName("merge");
		state.addAttribute("startTag", start.getName());
		state.addAttribute("startTagType", new Integer(start.getType()).toString());
		state.addAttribute("endTag", end.getName());
		state.addAttribute("endTagType", new Integer(end.getType()).toString());
	}
}
