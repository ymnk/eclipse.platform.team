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

import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.ITeamResourceChangeListener;
import org.eclipse.team.core.subscribers.TeamDelta;
import org.eclipse.team.core.subscribers.utils.SessionSynchronizationCache;
import org.eclipse.team.core.subscribers.utils.SynchronizationCache;

/**
 * This subscriber is used when comparing the local workspace with its
 * corresponding remote.
 */
public class CVSCompareSubscriber extends CVSSyncTreeSubscriber implements ITeamResourceChangeListener {

	public static final String QUALIFIED_NAME = CVSProviderPlugin.ID + ".compare"; //$NON-NLS-1$
	private static final String UNIQUE_ID_PREFIX = "compare-"; //$NON-NLS-1$
	
	private CVSTag tag;
	private SynchronizationCache remoteSynchronizer;
	private IResource[] resources;

	public CVSCompareSubscriber(IResource[] resources, CVSTag tag) {
		super(getUniqueId(), "CVS Compare with {0}" + tag.getName(), "Shows the differences between a tag and the workspace.");
		this.resources = resources;
		this.tag = tag;
		initialize();
	}

	private void initialize() {
		remoteSynchronizer = new SessionSynchronizationCache();
		CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().addListener(this);
	}

	public void dispose() {	
		CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().removeListener(this);	
		remoteSynchronizer.dispose();	
	}
	
	private static QualifiedName getUniqueId() {
		String uniqueId = Long.toString(System.currentTimeMillis());
		return new QualifiedName(QUALIFIED_NAME, UNIQUE_ID_PREFIX + uniqueId); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.CVSSyncTreeSubscriber#getRemoteTag()
	 */
	protected CVSTag getRemoteTag() {
		return tag;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.CVSSyncTreeSubscriber#getBaseTag()
	 */
	protected CVSTag getBaseTag() {
		// No base tag needed since it's a two way compare
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.CVSSyncTreeSubscriber#getBaseSynchronizationCache()
	 */
	protected SynchronizationCache getBaseSynchronizationCache() {
		// No base cache needed since it's a two way compare
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.CVSSyncTreeSubscriber#getRemoteSynchronizationCache()
	 */
	protected SynchronizationCache getRemoteSynchronizationCache() {
		return remoteSynchronizer;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.TeamSubscriber#isThreeWay()
	 */
	public boolean isThreeWay() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.TeamSubscriber#roots()
	 */
	public IResource[] roots() {
		return resources;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.ITeamResourceChangeListener#teamResourceChanged(org.eclipse.team.core.subscribers.TeamDelta[])
	 */
	public void teamResourceChanged(TeamDelta[] deltas) {
		List outgoingDeltas = new ArrayList(deltas.length);
		for (int i = 0; i < deltas.length; i++) {
			TeamDelta delta = deltas[i];
			if ((delta.getFlags() & TeamDelta.ROOT_REMOVED) != 0) {
				IResource resource = delta.getResource();
				outgoingDeltas.addAll(Arrays.asList(handleRemovedRoot(resource)));
			} else if ((delta.getFlags() & TeamDelta.SYNC_CHANGED) != 0) {
				IResource resource = delta.getResource();
				try {
					if (isSupervised(resource)) {
						outgoingDeltas.add(new TeamDelta(this, delta.getFlags(), resource));
					}
				} catch (TeamException e) {
					// Log and ignore
					CVSProviderPlugin.log(e);
				}
			}
		}
		
		fireTeamResourceChange((TeamDelta[]) outgoingDeltas.toArray(new TeamDelta[outgoingDeltas.size()]));
	}

	private TeamDelta[] handleRemovedRoot(IResource removedRoot) {
		// Determine if any of the roots of the compare are affected
		List removals = new ArrayList(resources.length);
		for (int j = 0; j < resources.length; j++) {
			IResource root = resources[j];
			if (removedRoot.getFullPath().isPrefixOf(root.getFullPath())) {
				// The root is no longer managed by CVS
				removals.add(root);
			}
		}
		if (removals.isEmpty()) {
			return new TeamDelta[0];
		}
		
		// Adjust the roots of the subscriber
		List newRoots = new ArrayList(resources.length);
		newRoots.addAll(Arrays.asList(resources));
		newRoots.removeAll(removals);
		resources = (IResource[]) newRoots.toArray(new IResource[newRoots.size()]);
		 
		// Create the deltas for the removals
		TeamDelta[] deltas = new TeamDelta[removals.size()];
		for (int i = 0; i < deltas.length; i++) {
			deltas[i] = new TeamDelta(this, TeamDelta.ROOT_REMOVED, (IResource)removals.get(i));
		}
		return deltas;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.TeamSubscriber#isSupervised(org.eclipse.core.resources.IResource)
	 */
	public boolean isSupervised(IResource resource) throws TeamException {
		if (super.isSupervised(resource)) {
			if (!resource.exists() && getRemoteSynchronizationCache().getSyncBytes(resource) == null) {
				// Exclude conflicting deletions
				return false;
			}
			for (int i = 0; i < resources.length; i++) {
				IResource root = resources[i];
				if (root.getFullPath().isPrefixOf(resource.getFullPath())) {
					return true;
				}
			}
		}
		return false;
	}
}
