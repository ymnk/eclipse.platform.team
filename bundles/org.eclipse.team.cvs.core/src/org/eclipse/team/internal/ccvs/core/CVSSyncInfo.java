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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.sync.ILocalSyncElement;
import org.eclipse.team.core.sync.IRemoteResource;
import org.eclipse.team.core.sync.SyncInfo;
import org.eclipse.team.core.sync.SyncTreeSubscriber;
import org.eclipse.team.internal.ccvs.core.client.Update;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.RemoteResource;

/**
 * CVSSyncInfo
 */
public class CVSSyncInfo extends SyncInfo {

	public CVSSyncInfo(IResource local, IRemoteResource base, IRemoteResource remote, SyncTreeSubscriber subscriber, IProgressMonitor monitor) throws TeamException {
		super(local, base, remote, subscriber, monitor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.SyncInfo#computeSyncKind(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected int calculateKind(IProgressMonitor progress) throws TeamException {
		// special handling for folders, the generic sync algorithm doesn't work well
		// with CVS because folders are not in namespaces (e.g. they exist in all versions
		// and branches).
		IResource local = getLocal();
		if(local.getType() != IResource.FILE && getSubscriber().isThreeWay()) {
			int folderKind = SyncInfo.IN_SYNC;
			ICVSRemoteFolder remote = (ICVSRemoteFolder)getRemote();
			ICVSFolder cvsFolder = CVSWorkspaceRoot.getCVSFolderFor((IContainer)local);
			boolean isCVSFolder = false;
			try {
				isCVSFolder = cvsFolder.isCVSFolder();
			} catch (CVSException e) {
				// Assume the folder is not a CVS folder
			}
			if(!local.exists()) {
				if(remote != null) {
					if (isCVSFolder) {
						if (containsOutgoingDeletions(cvsFolder)) {
							// say the folder is in_sync even though it doesn't exist locally
							folderKind = SyncInfo.IN_SYNC;
						} else {
							folderKind = SyncInfo.INCOMING | SyncInfo.ADDITION;
						}
					} else {
						folderKind = SyncInfo.INCOMING | SyncInfo.ADDITION;
					}
				} else {
					// ignore conflicting deletion to keep phantom sync info
				}
			} else {
				if(remote == null) {
					if(isCVSFolder) {
						folderKind = SyncInfo.INCOMING | SyncInfo.DELETION;
					} else {
						folderKind = SyncInfo.OUTGOING | SyncInfo.ADDITION;
					}
				} else if(!isCVSFolder) {
					folderKind = SyncInfo.CONFLICTING | SyncInfo.ADDITION;
				} else {
					// folder exists both locally and remotely and are considered in sync, however 
					// we aren't checking the folder mappings to ensure that they are the same.
				}
			}
			return folderKind;
		}
	
		// 1. Run the generic sync calculation algorithm, then handle CVS specific
		// sync cases.
		int kind = super.calculateKind(progress);
	
		// 2. Set the CVS specific sync type based on the workspace sync state provided
		// by the CVS server.
		IRemoteResource remote = getRemote();
		if(remote!=null && (kind & SyncInfo.PSEUDO_CONFLICT) == 0) {
			RemoteResource cvsRemote = (RemoteResource)remote;
			int type = cvsRemote.getWorkspaceSyncState();
			switch(type) {
				// the server compared both text files and decided that it cannot merge
				// them without line conflicts.
				case Update.STATE_CONFLICT: 
					return kind | ILocalSyncElement.MANUAL_CONFLICT;

				// the server compared both text files and decided that it can safely merge
				// them without line conflicts. 
				case Update.STATE_MERGEABLE_CONFLICT: 
					return kind | ILocalSyncElement.AUTOMERGE_CONFLICT;				
			}			
		}
	
		// 3. unmanage delete/delete conflicts and return that they are in sync
		kind = handleDeletionConflicts(kind);
	
		return kind;
	}

	/**
	 * Return true if the provided phantom folder conyains any outgoing file deletions.
	 * We only need to detect if there are any files since a phantom folder can only
	 * contain outgoing filre deletions and other folder.
	 * 
	 * @param cvsFolder a phantom folder
	 * @return boolean
	 */
	private boolean containsOutgoingDeletions(ICVSFolder cvsFolder) {
		final boolean result[] = new boolean[] { false };
		try {
			cvsFolder.accept(new ICVSResourceVisitor() {
				public void visitFile(ICVSFile file) throws CVSException {
					// Do nothing. Files are handled below
				}
				public void visitFolder(ICVSFolder folder) throws CVSException {
					if (folder.members(ICVSFolder.FILE_MEMBERS).length > 0) {
						result[0] = true;
					} else {
						folder.acceptChildren(this);
					}
				}
			});
		} catch (CVSException e) {
			CVSProviderPlugin.log(e);
		}
		return result[0];
	}
	
	/*
	 * If the resource has a delete/delete conflict then ensure that the local is unmanaged so that the 
	 * sync info can be properly flushed.
	 */
	private int handleDeletionConflicts(int kind) {
		if(kind == (SyncInfo.CONFLICTING | SyncInfo.DELETION | SyncInfo.PSEUDO_CONFLICT)) {
			try {				
				IResource local = getLocal();
				ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(local);
				if(!cvsResource.isFolder() && cvsResource.isManaged()) {
					cvsResource.unmanage(null);
				}
				return SyncInfo.IN_SYNC;
			} catch(CVSException e) {
				CVSProviderPlugin.log(e.getStatus());
				return SyncInfo.CONFLICTING | SyncInfo.DELETION;
			}
		}
		return kind;
	}
}
