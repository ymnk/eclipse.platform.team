package org.eclipse.team.internal.ccvs.core.resources;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.ccvs.core.ICVSFile;
import org.eclipse.team.ccvs.core.ICVSFolder;
import org.eclipse.team.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProvider;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;

public class CVSWorkspaceRoot {

	private ICVSFolder localRoot;
	
	public CVSWorkspaceRoot(IContainer resource){
		this.localRoot = getCVSFolderFor(resource);
	}
	
/*	public CVSWorkspaceRoot(File resource){
		try {
			this.localRoot = getCVSFolderFor(resource);
		} catch(ClassCastException e) {
			this.localRoot = null;
		}
	}
*/
	/*
	 * @see ICVSWorkspaceRoot#getCVSResourceFor(File)
	 */
/*	public static ICVSResource getCVSResourceFor(File resource) {
		if (resource.exists()) {
			if (resource.isDirectory()) {
				return new LocalFolder(resource);
			} else {
				return new LocalFile(resource);
			}
		}
		return null;		
	}
*/
	/*
	 * @see ICVSWorkspaceRoot#getRemoteResourceFor(IResource)
	 */
	public static ICVSRemoteResource getRemoteResourceFor(IResource resource) throws CVSException {
		ICVSResource managed = getCVSResourceFor(resource);
		if (managed.isFolder()) {
			ICVSFolder folder = (ICVSFolder)managed;
			if (folder.isCVSFolder()) {
				FolderSyncInfo syncInfo = folder.getFolderSyncInfo();
				return new RemoteFolder(null, CVSProvider.getInstance().getRepository(syncInfo.getRoot()), new Path(syncInfo.getRepository()), syncInfo.getTag());
			}
		} else {
			if (managed.isManaged())
				return RemoteFile.getBase((RemoteFolder)getRemoteResourceFor(resource.getParent()), (ICVSFile)managed);
		}
		return null;
	}

	/*
	 * @see ICVSWorkspaceRoot#getRemoteRoot()
	 */
	public ICVSRepositoryLocation getRemoteLocation() throws CVSException {
		return CVSProvider.getInstance().getRepository(localRoot.getFolderSyncInfo().getRoot());
	}

	/*
	 * @see ICVSWorkspaceRoot#getLocalRoot()
	 */
	public ICVSFolder getLocalRoot() {
		return localRoot;
	}

	/**
	 * Gives you an LocalFolder for a absolute path in
	 * platform dependend style.
	 * 
	 * @throws CVSException on path.indexOf("CVS") != -1
	 * @throws CVSException on internal IOExeption
	 */
	public static ICVSFolder getCVSFolderFor(IContainer resource) {
		return new EclipseFolder(resource);
	}


	public static ICVSFile getCVSFileFor(IFile resource) {
		return new EclipseFile(resource);
	}


	public static ICVSResource getCVSResourceFor(IResource resource) {
		if (resource.getType() == IResource.FILE)
			return getCVSFileFor((IFile) resource);
		else
			return getCVSFolderFor((IContainer) resource);
	}

}