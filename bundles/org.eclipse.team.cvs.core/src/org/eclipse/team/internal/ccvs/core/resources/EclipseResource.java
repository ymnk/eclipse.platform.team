package org.eclipse.team.internal.ccvs.core.resources;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.ccvs.core.ICVSFolder;
import org.eclipse.team.ccvs.core.ICVSResource;
import org.eclipse.team.core.IIgnoreInfo;
import org.eclipse.team.core.TeamPlugin;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.Policy;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.Assert;
import org.eclipse.team.internal.ccvs.core.util.FileNameMatcher;
import org.eclipse.team.internal.ccvs.core.util.SyncFileUtil;
import org.eclipse.team.internal.ccvs.core.util.Util;

/**
 * Represents handles to CVS resource on the local file system. Synchronization
 * information is taken from the CVS subdirectories. 
 * 
 * @see LocalFolder
 * @see LocalFile
 */
abstract class EclipseResource implements ICVSResource {

	 // The seperator that must be used when creating CVS resource paths. Never use
	 // the platform default seperator since it is not compatible with CVS resources.
	protected static final String SEPARATOR = Session.SERVER_SEPARATOR;
	protected static final String CURRENT_LOCAL_FOLDER = Session.CURRENT_LOCAL_FOLDER;
		
	/*
	 * The local resource represented by this handle
	 */
	IResource resource;
	
	/*
	 * Creates a CVS handle to the provided resource
	 */
	protected EclipseResource(IResource resource) {
		Assert.isNotNull(resource);
		this.resource = resource;
	}
	
	/*
	 * Get the extention of the path of resource relative to the path of root
	 * 
	 * @throws CVSException if root is not a root-folder of resource
	 */
	public String getRelativePath(ICVSFolder root) throws CVSException {
		try {
			EclipseResource rootFolder;
			String result;
			rootFolder = (EclipseResource)root;
			result = Util.getRelativePath(rootFolder.getPath(), getPath()); 
			return result;	
		} catch (ClassCastException e) {
			throw new CVSException(Policy.bind("LocalResource.invalidResourceClass"), e); //$NON-NLS-1$
		}
	}

	/*
	 * @see ICVSResource#delete()
	 */
	public void delete() throws CVSException {
		try {
			resource.delete(true /*force*/, null);
		} catch(CoreException e) {
			throw new CVSException(e.getStatus());
		}
	}

	/*
	 * @see ICVSResource#exists()
	 */
	public boolean exists() {
		return resource.exists();
	}

	/*
	 * Returns the parent folder of this resource of <code>null</code> if resource
	 * the resource.
	 * 
	 * @see ICVSResource#getParent()
	 */
	public ICVSFolder getParent() {
		IContainer parent = resource.getParent();
		if (parent == null || parent.getType() == IResource.ROOT) {
			return null;
		}
		return new EclipseFolder(parent);
	}

	/*
	 * @see ICVSResource#getName()
	 */
	public String getName() {
		return resource.getName();
	}

	/*
	 * @see ICVSResource#isIgnored()
	 */
	public boolean isIgnored() {
		// a managed resource is never ignored
		if(isManaged()) {
			return false;
		}
		
		// initialize matcher with global ignores and basic CVS ignore patterns
		IIgnoreInfo[] ignorePatterns = TeamPlugin.getManager().getGlobalIgnore();
		FileNameMatcher matcher = new FileNameMatcher(SyncFileUtil.BASIC_IGNORE_PATTERNS);
		for (int i = 0; i < ignorePatterns.length; i++) {
			IIgnoreInfo info = ignorePatterns[i];
			if(info.getEnabled()) {
				matcher.register(info.getPattern(), "true"); //$NON-NLS-1$
			}
		}
		
		// 1. check CVS default patterns and global ignores
		boolean ignored = matcher.match(getName());
		
		// 2. check .cvsignore file
		if(!ignored) {
			ignored = CVSProviderPlugin.getSynchronizer().isIgnored(getIOFile());		
		}
		
		// 3. check the parent
		if(!ignored) {
			ICVSFolder parent = getParent();
			if(parent==null) return false;
			return parent.isIgnored();
		} else {
			return ignored;
		}
	}

	/*
	 * @see ICVSResource#setIgnored()
	 */
	public void setIgnored() throws CVSException {
		CVSProviderPlugin.getSynchronizer().setIgnored(getIOFile(), null);
	}
	
	/*
	 * @see ICVSResource#setIgnoredAs(String)
	 */
	public void setIgnoredAs(String pattern) throws CVSException {
		CVSProviderPlugin.getSynchronizer().setIgnored(getIOFile(), pattern);		
	}

	/*
	 * @see ICVSResource#isManaged()
	 */
	public boolean isManaged() {
		try {
			return getSyncInfo() != null;
		} catch(CVSException e) {
			return false;
		}
	}
			
	/**
	 * Two ManagedResources are equal, if there cvsResources are
	 * equal (and that is, if the point to the same file)
	 */
	public boolean equals(Object obj) {
		
		if (!(obj instanceof EclipseResource)) {
			return false;
		} else {
			return getPath().equals(((EclipseResource) obj).getPath());
		}
	}
			
	/*
	 * @see ICVSResource#getPath()
	 */
	public String getPath() {
		return resource.getFullPath().toString();
	}	
	
	/*
	 * @see ICVSResource#isFolder()
	 */
	public boolean isFolder() {
		return false;
	}
	
	/*
	 * @see ICVSResource#getSyncInfo()
	 */
	public ResourceSyncInfo getSyncInfo() throws CVSException {
		return CVSProviderPlugin.getSynchronizer().getResourceSync(getIOFile());
	}

	/*
	 * @see ICVSResource#setSyncInfo(ResourceSyncInfo)
	 */
	public void setSyncInfo(ResourceSyncInfo info) throws CVSException {
		if (getParent().isCVSFolder()) {
			CVSProviderPlugin.getSynchronizer().setResourceSync(getIOFile(), info);		
		}
	}
	
	/*
	 * Implement the hashcode on the underlying strings, like it is done in the equals.
	 */
	public int hashCode() {
		return getPath().hashCode();
	}	
	
	/*
	 * Give the pathname back
	 */
	public String toString() {
		return getPath();
	}
			
	/*
	 * REFACTOR Temporary helper for accessing the underlying file associated with this
	 * eclipse resource. Must be removed when the refactoring is complete.
	 */
	protected File getIOFile() {
		return resource.getLocation().toFile();
	}
	
		/*
	 * @see ICVSResource#reloadSyncInfo(IProgressMonitor)
	 */
	public void reloadSyncInfo(IProgressMonitor monitor) throws CVSException {
		CVSProviderPlugin.getSynchronizer().reload(getIOFile(), monitor);
	}

	/*
	 * @see ICVSResource#saveSyncInfo(IProgressMonitor)
	 */
	public void saveSyncInfo(IProgressMonitor monitor) throws CVSException {
		CVSProviderPlugin.getSynchronizer().save(getIOFile(), monitor);
	}
}