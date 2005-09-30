package org.eclipse.team.internal.ccvs.core.filesystem;

import java.net.URI;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFolder;

public class CVSFileSystem extends FileSystem {

	public CVSFileSystem() {
		super();
	}

	public IFileStore getStore(URI uri) {
		return new CVSFileStore(getRemoteResource(uri));
	}

	private ICVSRemoteResource getRemoteResource(URI uri) {
		ICVSRepositoryLocation repository = getRepository(uri);
		if (repository == null)
			throw new IllegalArgumentException(NLS.bind("Invalid uri {0}", new String[] {uri.toString()}));
		String path = getPath(uri);
		CVSTag tag = getTag(uri);
		ICVSRemoteFolder folder = new RemoteFolder(null, repository, path, tag);
		return null;
	}

	private CVSTag getTag(URI uri) {
		// TODO Auto-generated method stub
		return null;
	}

	private String getPath(URI uri) {
		// TODO Auto-generated method stub
		return null;
	}

	private ICVSRepositoryLocation getRepository(URI uri) {
		String host = uri.getHost();
		
		return null;
	}

}
