package org.eclipse.team.internal.ccvs.core.resources;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.sync.IRemoteResource;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;

public class CVSCompareSyncElement extends CVSRemoteSyncElement {

	public CVSCompareSyncElement(IResource local, IRemoteResource remote) {
		super(false /* two way */, local, null, remote);
	}

	/*
	 * Assume both resources exist.
	 * @see ILocalSyncElement#isDirty()
	 */
	public boolean isDirty() {
		IResource local = getLocal();
		try {
			
			if(local.getType()==IResource.FILE) {
				ICVSFile file = CVSWorkspaceRoot.getCVSFileFor((IFile)local);
				ResourceSyncInfo info = file.getSyncInfo();
				if(info==null) {
					return true;
				}
				boolean sameRevisions = ((RemoteFile)getRemote()).getRevision().equals(info.getRevision());
				return !sameRevisions;
			} else {
				return false;
			}
		} catch(CVSException e) {
			CVSProviderPlugin.log(e.getStatus());
			return true;
		}
	}
}