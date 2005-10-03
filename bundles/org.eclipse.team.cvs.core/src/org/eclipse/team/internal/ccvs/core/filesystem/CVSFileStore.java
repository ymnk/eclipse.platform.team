/**********************************************************************
 * Copyright (c) 2005 IBM Corporation and others. All rights reserved.   This
 * program and the accompanying materials are made available under the terms of
 * the Common Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.team.internal.ccvs.core.filesystem;

import java.io.InputStream;
import java.net.URI;

import org.eclipse.core.filesystem.FileSystemCore;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileStoreConstants;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFile;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.ILogEntry;

public class CVSFileStore extends FileStore {

	private final CVSURI uri;
	private IFileInfo info;

	public CVSFileStore(CVSURI uri, IFileInfo info) {
		this.uri = uri;
		this.info = info;
	}

	public String[] childNames(int options, IProgressMonitor monitor) {
		IFileInfo[] infos = childInfos(options, monitor);
		String[] names = new String[infos.length];
		for (int i = 0; i < infos.length; i++) {
			names[i] = infos[i].getName();
		}
		return names;
	}

	public IFileStore[] childStores(int options, IProgressMonitor monitor) {
		IFileInfo[] infos = childInfos(options, monitor);
		IFileStore[] children = new IFileStore[infos.length];
		for (int i = 0; i < infos.length; i++) {
			children[i] = getChild(infos[i]);
		}
		return children;
	}
	
	private IFileStore getChild(IFileInfo info) {
		return new CVSFileStore(uri.append(info.getName()), info);
	}

	public IFileInfo fetchInfo(int options, IProgressMonitor monitor) {
		ICVSRemoteFolder folder = uri.getParentFolder();
		if (folder == null) {
			// this is the repo root so return an info that indicates this
			FileInfo info = new FileInfo();
			info.setExists(true);
			info.setName(uri.getRepositoryName());
			info.setAttribute(IFileStoreConstants.ATTRIBUTE_DIRECTORY, true);
		}
		try {
			ICVSResource[] children = folder.fetchChildren(monitor);
			ICVSResource resource = null;
			for (int i = 0; i < children.length; i++) {
				ICVSResource child = children[i];
				if (child.getName().equals(getName())) {
					resource = child;
					break;
				}
			}
			return getFileInfo(resource, monitor);
		} catch (CoreException e) {
			// TODO Need to handle this somehow
			CVSProviderPlugin.log(e);
			return null;
		}
	}

	private IFileInfo getFileInfo(ICVSResource resource, IProgressMonitor monitor) throws TeamException {
		if (resource == null)
			return null;
		FileInfo info = new FileInfo();
		info.setExists(true);
		info.setName(resource.getName());
		info.setAttribute(IFileStoreConstants.ATTRIBUTE_DIRECTORY, resource.isFolder());
		if (!resource.isFolder()) {
			ICVSRemoteFile file = (ICVSRemoteFile)resource;
			ILogEntry entry = file.getLogEntry(monitor);
			info.setLastModified(entry.getDate().getTime());
		} else {
			info.setLastModified(0);
		}
		return info;
	}

	public IFileStore getChild(String name) {
		if (info != null && !info.isDirectory()) {
			return null;
		}
		return new CVSFileStore(uri.append(name), null);
	}

	public String getName() {
		return uri.getLastSegment();
	}

	public IFileStore getParent() {
		if (uri.isRepositoryRoot()) {
			return null;
		}
		return new CVSFileStore(uri.removeLastSegment(), null);
	}

	public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
		ICVSRemoteFile file = uri.toFile();
		IStorage storage = ((IResourceVariant)file).getStorage(monitor);
		return storage.getContents();
	}

	public URI toURI() {
		return uri.toURI();
	}

}
