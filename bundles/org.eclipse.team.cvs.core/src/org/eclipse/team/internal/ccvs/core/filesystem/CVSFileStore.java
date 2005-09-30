package org.eclipse.team.internal.ccvs.core.filesystem;

import java.io.InputStream;
import java.net.URI;

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;

public class CVSFileStore extends FileStore {

	private final ICVSRemoteResource remoteResource;

	public CVSFileStore(ICVSRemoteResource remoteResource) {
		this.remoteResource = remoteResource;
	}

	public String[] childNames(int options, IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		return null;
	}

	public IFileInfo fetchInfo(int options, IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		return null;
	}

	public IFileStore getChild(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getName() {
		return remoteResource.getName();
	}

	public IFileStore getParent() {
		// TODO Remote resource may not have a parent
		return new CVSFileStore((ICVSRemoteResource)remoteResource.getParent());
	}

	public IFileStore mkdir(int options, IProgressMonitor monitor) throws CoreException {
		throw new UnsupportedOperationException();
	}

	public InputStream openInputStream(int options, IProgressMonitor monitor)
			throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	public URI toURI() {
		// TODO Auto-generated method stub
		return null;
	}

}
