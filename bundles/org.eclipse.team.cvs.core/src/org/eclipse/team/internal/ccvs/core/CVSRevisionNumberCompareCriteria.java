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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.synchronize.ISubscriberResource;
import org.eclipse.team.core.synchronize.ISubscriberResourceComparator;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;

/**
 * CVSRevisionNumberCompareCriteria
 */
 public class CVSRevisionNumberCompareCriteria implements ISubscriberResourceComparator {
 	
	private CVSSyncTreeSubscriber subscriber;

	public CVSRevisionNumberCompareCriteria(CVSSyncTreeSubscriber subscriber) {
		this.subscriber = subscriber;
	}

	/* (non-Javadoc)
	 * @see ComparisonCriteria#compare(Object, Object, IProgressMonitor)
	 */
	public boolean compare(Object e1, Object e2) {
		if(e1 instanceof IResource && e2 instanceof ICVSRemoteResource) {
			return compare((IResource)e1, (ICVSRemoteResource)e2);
		} else if(e1 instanceof ICVSRemoteResource && e2 instanceof ICVSRemoteResource) {
			return compare((ICVSRemoteResource)e1, (ICVSRemoteResource)e2);
		}
		return false;
	}
	
	/**
	 * @see RemoteSyncElement#timestampEquals(IRemoteResource, IRemoteResource)
	 */
	protected boolean compare(ICVSRemoteResource e1, ICVSRemoteResource e2) {
		if(e1.isContainer()) {
			if(e2.isContainer()) {
				return true;
			}
			return false;
		}
		return e1.equals(e2);
	}

	/**
	 * @see RemoteSyncElement#timestampEquals(IResource, IRemoteResource)
	 */
	protected boolean compare(IResource e1, ICVSRemoteResource e2) {
		if(e1.getType() != IResource.FILE) {
			if(e2.isContainer()) {
				return true;
			}
			return false;
		}
		ICVSFile cvsFile = CVSWorkspaceRoot.getCVSFileFor((IFile)e1);
		try {
			byte[] syncBytes1 = cvsFile.getSyncBytes();
			byte[] syncBytes2 = ((ICVSRemoteFile)e2).getSyncBytes();
		
			if(syncBytes1 != null) {
				if(ResourceSyncInfo.isDeletion(syncBytes1) || ResourceSyncInfo.isMerge(syncBytes1) || cvsFile.isModified(null)) {
					return false;
				}
				return ResourceSyncInfo.getRevision(syncBytes1).equals(ResourceSyncInfo.getRevision(syncBytes2));
			}
			return false;
		} catch(CVSException e) {
			CVSProviderPlugin.log(e);
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.IComparisonCriteria#compare(org.eclipse.core.resources.IResource, org.eclipse.team.core.subscribers.ISubscriberResource)
	 */
	public boolean compare(IResource local, ISubscriberResource remote) {
		return compare(local, (ICVSRemoteResource)remote);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.IComparisonCriteria#compare(org.eclipse.team.core.subscribers.ISubscriberResource, org.eclipse.team.core.subscribers.ISubscriberResource)
	 */
	public boolean compare(ISubscriberResource base, ISubscriberResource remote) {
		return compare((ICVSRemoteResource)base, (ICVSRemoteResource)remote);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.ISubscriberResourceComparator#isThreeWay()
	 */
	public boolean isThreeWay() {
		return subscriber.isThreeWay();
	}
}
