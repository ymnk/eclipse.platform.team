package org.eclipse.team.internal.ccvs.core.resources;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.ccvs.core.*;
import org.eclipse.team.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;

/**
 * Implements the ICVSFolder interface on top of an 
 * instance of the ICVSFolder interface
 * 
 * @see ICVSFolder
 */
class EclipseFolder extends EclipseResource implements ICVSFolder {

	protected EclipseFolder(IContainer container) {
		super(container);		
	}

	/**
	 * 
	 * @see ICVSFolder#getFolders()
	 */
	public ICVSFolder[] getFolders() throws CVSException {
		try {
			IContainer folder = (IContainer)resource;
			
			if(!resource.exists()) {
				return new ICVSFolder[0];
			}
			
			final List folders = new ArrayList();
			
			ResourceSyncInfo[] syncDirs = CVSProviderPlugin.getSynchronizer().members(getIOFile());
			for (int i = 0; i < syncDirs.length; i++) {
				if(syncDirs[i].isDirectory()) {
					folders.add(new EclipseFolder(folder.getFolder(new Path(syncDirs[i].getName()))));
				}			
			}
			
			IResource[] members = folder.members();
			for (int i = 0; i < members.length; i++) {
				IResource child = members[i];
				if(child.getType()!=IResource.FILE) {
					EclipseFolder childFolder = new EclipseFolder((IContainer)child);
					if(!childFolder.isIgnored() && !folders.contains(childFolder)) {
						folders.add(childFolder);						
					}		
				}
			}
			return (ICVSFolder[]) folders.toArray(new ICVSFolder[folders.size()]);
		} catch(CoreException e) {
			throw new CVSException(e.getStatus());
		}
	}
	
	/**
	 * @see ICVSFolder#getFiles()
	 */
	public ICVSFile[] getFiles() throws CVSException {
		try {
			IContainer folder = (IContainer)resource;
			
			if(!resource.exists()) {
				return new ICVSFile[0];
			}
			
			final List files = new ArrayList();
			
			ResourceSyncInfo[] syncFiles = CVSProviderPlugin.getSynchronizer().members(getIOFile());
			for (int i = 0; i < syncFiles.length; i++) {
				if(!syncFiles[i].isDirectory()) {
					files.add(new EclipseFile((IFile)folder.getFile(new Path(syncFiles[i].getName()))));
				}			
			}
			
			IResource[] members = folder.members();
			for (int i = 0; i < members.length; i++) {
				IResource child = members[i];
				if(child.getType()==IResource.FILE) {
					EclipseFile childFile = new EclipseFile((IFile)child);
					if(!childFile.isIgnored() && !files.contains(childFile)) {
						files.add(childFile);						
					}		
				}
			}				
			return (ICVSFile[]) files.toArray(new ICVSFile[files.size()]);	
		} catch(CoreException e) {
			throw new CVSException(e.getStatus());
		}
	}

	/**
	 * @see ICVSFolder#createFolder(String)
	 */
	public ICVSFolder getFolder(String name) throws CVSException {
		if ((CURRENT_LOCAL_FOLDER.equals(name)) || ((CURRENT_LOCAL_FOLDER + SEPARATOR).equals(name)))
			return this;
		IPath path = new Path(name);
		if(resource.getType()==IResource.ROOT && path.segmentCount()==1) {
			return new EclipseFolder(((IWorkspaceRoot)resource).getProject(name));
		} else {
			return new EclipseFolder(((IContainer)resource).getFolder(new Path(name)));
		}
	}

	/**
	 * @see ICVSFolder#createFile(String)
	 */
	public ICVSFile getFile(String name) throws CVSException {
		return new EclipseFile(((IContainer)resource).getFile(new Path(name)));
	}

	/**
	 * @see ICVSFolder#mkdir()
	 */
	public void mkdir() throws CVSException {
		try {
			if(resource.getType()==IResource.PROJECT) {
				IProject project = (IProject)resource;
				project.create(null);
				project.open(null);				
			} else {
				((IFolder)resource).create(false /*don't force*/, true /*make local*/, null);
			}				
		} catch (CoreException e) {
			throw new CVSException(e.getStatus());
		} 
	}
		
	/**
	 * @see ICVSResource#isFolder()
	 */
	public boolean isFolder() {
		return true;
	}
		
	/**
	 * @see ICVSFolder#acceptChildren(ICVSResourceVisitor)
	 */
	public void acceptChildren(ICVSResourceVisitor visitor) throws CVSException {
		
		ICVSResource[] subFiles;
		ICVSResource[] subFolders;
		
		subFiles = getFiles();
		subFolders = getFolders();
		
		for (int i=0; i<subFiles.length; i++) {
			subFiles[i].accept(visitor);
		}
		
		for (int i=0; i<subFolders.length; i++) {
			subFolders[i].accept(visitor);
		}
	}

	/**
	 * @see ICVSResource#accept(ICVSResourceVisitor)
	 */
	public void accept(ICVSResourceVisitor visitor) throws CVSException {
		visitor.visitFolder(this);
	}

	/**
	 * @see ICVSResource#getRemoteLocation(ICVSFolder)
	 */
	public String getRemoteLocation(ICVSFolder stopSearching) throws CVSException {
				
		if (getFolderSyncInfo() != null) {
			return getFolderSyncInfo().getRemoteLocation();
		}			

		ICVSFolder parent = getParent();
		if(parent!=null && !equals(stopSearching)) {
			String parentLocation;
			parentLocation = parent.getRemoteLocation(stopSearching);
			if (parentLocation!=null) {
				return parentLocation + SEPARATOR + getName();
			}		
		}
		return null;
	}

	/*
	 * @see ICVSFolder#getFolderInfo()
	 */
	public FolderSyncInfo getFolderSyncInfo() throws CVSException {
		return CVSProviderPlugin.getSynchronizer().getFolderSync(getIOFile());
	}

	/*
	 * @see ICVSFolder#setFolderInfo(FolderSyncInfo)
	 */
	public void setFolderSyncInfo(FolderSyncInfo folderInfo) throws CVSException {
		CVSProviderPlugin.getSynchronizer().setFolderSync(getIOFile(), folderInfo);
		// the server won't add directories as sync info, therefore it must be done when
		// a directory is shared with the repository.
		setSyncInfo(new ResourceSyncInfo(getName()));
	}

	/*
	 * @see ICVSFolder#isCVSFolder()
	 */
	public boolean isCVSFolder() {
		try {
			return CVSProviderPlugin.getSynchronizer().getFolderSync(getIOFile()) != null;
		} catch(CVSException e) {
			return false;
		}
	}

	/*
	 * @see ICVSResource#unmanage()
	 */
	public void unmanage() throws CVSException {
		CVSProviderPlugin.getSynchronizer().deleteFolderSync(getIOFile(), new NullProgressMonitor());
	}
	
	/*
	 * @see ICVSResource#isIgnored()
	 */
	public boolean isIgnored() {
		if(isCVSFolder()) {
			return false;
		}		
		return super.isIgnored();
	}
	
	/*
	 * @see ICVSFolder#getChild(String)
	 */
	public ICVSResource getChild(String path) throws CVSException {
		IResource child = ((IContainer)resource).findMember(path);
		if(child!=null) {
			if(child.getType()==IResource.FILE) {
				return new EclipseFile((IFile)child);
			} else {
				return new EclipseFolder((IContainer)child);
			}
		}
		return null;
	}
}