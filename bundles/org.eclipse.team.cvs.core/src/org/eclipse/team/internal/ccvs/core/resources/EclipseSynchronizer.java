package org.eclipse.team.internal.ccvs.core.resources;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ISynchronizer;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.ccvs.core.CVSTag;
import org.eclipse.team.ccvs.core.ICVSFile;
import org.eclipse.team.ccvs.core.ICVSFolder;
import org.eclipse.team.ccvs.core.ICVSResource;
import org.eclipse.team.core.TeamPlugin;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.Policy;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.Assert;
import org.eclipse.team.internal.ccvs.core.util.SyncFileWriter;

/**
 * A synchronizer is responsible for managing synchronization information for local
 * CVS resources.
 * 
 * [Notes:
 *  1. how can we expire cache elements and purge to safe memory?
 *  2. how can we safeguard against overwritting meta files changes made outside of Eclipse? I'm
 *     not sure we should force setting file contents in EclipseFile handles?
 *  4. how do we reload
 * ]
 * 
 * @see ResourceSyncInfo
 * @see FolderSyncInfo
 */
public class EclipseSynchronizer {
	// the resources plugin synchronizer is used to cache and possibly persist. These 
	// are keys for storing the sync info.
	private static final QualifiedName FOLDER_SYNC_KEY = new QualifiedName(CVSProviderPlugin.ID, "folder-sync");
	private static final QualifiedName RESOURCE_SYNC_KEY = new QualifiedName(CVSProviderPlugin.ID, "resource-sync");
	private static final QualifiedName IGNORE_SYNC_KEY = new QualifiedName(CVSProviderPlugin.ID, "folder-ignore");
	
	// the cvs eclipse synchronizer is a singleton
	private static EclipseSynchronizer instance;
	
	// track resources that have changed in a given operation
	private int nestingCount = 0;
	private Set changedResources = new HashSet();
	private Set changedFolders = new HashSet();
	
	private EclipseSynchronizer() {
		getSynchronizer().add(RESOURCE_SYNC_KEY);
		getSynchronizer().add(FOLDER_SYNC_KEY);
		getSynchronizer().add(IGNORE_SYNC_KEY);
	}
	
	public static EclipseSynchronizer getInstance() {
		if (instance == null) {
			ResourcesPlugin.getWorkspace().getSynchronizer().remove(RESOURCE_SYNC_KEY);
			ResourcesPlugin.getWorkspace().getSynchronizer().remove(FOLDER_SYNC_KEY);
			ResourcesPlugin.getWorkspace().getSynchronizer().remove(IGNORE_SYNC_KEY);
			instance = new EclipseSynchronizer();			
		}
		return instance;
	}

	public void setFolderSync(IContainer folder, FolderSyncInfo info) throws CVSException {
		try {
			beginOperation(null);
			setCachedFolderSync(folder, info);
			changedFolders.add(folder);
		} finally {
			endOperation(null);
		}
	}
	
	public FolderSyncInfo getFolderSync(IContainer folder) throws CVSException {
		try {
			beginOperation(null);
			if(folder.getType()==IResource.ROOT) return null;					
			FolderSyncInfo info = getCachedFolderSync(folder);
			if (info == null) {
				info = SyncFileWriter.readFolderConfig(CVSWorkspaceRoot.getCVSFolderFor(folder));
				if(info!=null) {
					setCachedFolderSync(folder, info);
					// read the child meta-files also
					getMetaResourceSyncForFolder(folder, null);
				}
			}
			return info;
		} finally {
			endOperation(null);
		}
	}	

	public void setResourceSync(IResource resource, ResourceSyncInfo info) throws CVSException {
		try {
			beginOperation(null);
			setCachedResourceSync(resource, info);
			changedResources.add(resource);		
		} finally {
			endOperation(null);
		}		
	}
	
	public ResourceSyncInfo getResourceSync(IResource resource) throws CVSException {
		try {
			beginOperation(null);
			if(resource.getType()==IResource.ROOT) return null;			
			ResourceSyncInfo info = getCachedResourceSync(resource);
			if(info==null) {
				info = getMetaResourceSyncForFolder(resource.getParent(), resource);
			}
			return info;
		} finally {
			endOperation(null);
		}
	}

	public void deleteFolderSync(IContainer folder, IProgressMonitor monitor) throws CVSException {
		try {
			beginOperation(null);
			setCachedFolderSync(folder, null);
			changedFolders.add(folder);
		} finally {
			endOperation(null);
		}
	}
	
	public void deleteResourceSync(IResource resource, IProgressMonitor monitor) throws CVSException {
		try {
			beginOperation(null);
			setCachedResourceSync(resource, null);
			changedResources.add(resource);
		} finally {
			endOperation(null);
		}
	}

	public String[] getIgnored(IResource resource) throws CVSException {
		IContainer parent = resource.getParent();
		if(parent==null || parent.getType()==IResource.ROOT) return null;
		String[] ignores = getCachedFolderIgnores(parent);
		if(ignores==null) {
			ICVSFile ignoreFile = CVSWorkspaceRoot.getCVSFileFor(parent.getFile(new Path(SyncFileWriter.IGNORE_FILE)));
			if(ignoreFile.exists()) {
				ignores = SyncFileWriter.readLines(ignoreFile);
				setCachedFolderIgnores(parent, ignores);
			}
		}
		return ignores;
	}
	
	public void setIgnored(IResource resource, String pattern) throws CVSException {
		SyncFileWriter.addCvsIgnoreEntry(CVSWorkspaceRoot.getCVSResourceFor(resource), pattern);
	}
		
	public IResource[] members(IContainer folder) throws CVSException {
		try {
			// initialize cache if needed, this will create phantoms
			FolderSyncInfo info = getCachedFolderSync(folder);
			if(info==null) {
				getMetaResourceSyncForFolder(folder, null);
			}
			IResource[] children = folder.members(true);
			List list = new ArrayList(children.length);
			for (int i = 0; i < children.length; ++i) {
				IResource child = children[i];
				if (!child.isPhantom() || getCachedResourceSync(child) != null) {
					list.add(child);
				}
			}
			return (IResource[]) list.toArray(new IResource[list.size()]);
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}
	
	public void beginOperation(IProgressMonitor monitor) throws CVSException {
		if (nestingCount++ == 0) {
			// any work here?			
		}		
	}
	
	public void endOperation(IProgressMonitor monitor) throws CVSException {
		if (--nestingCount == 0) {	
			if (! changedFolders.isEmpty() || ! changedResources.isEmpty()) {
				try {
					monitor = Policy.monitorFor(monitor);
					int numResources = changedFolders.size() + changedResources.size();
					monitor.beginTask("Updating CVS synchronization information...", numResources);
					// write sync info to disk
					Iterator it = changedFolders.iterator();
					while (it.hasNext()) {
						IContainer folder = (IContainer) it.next();
						FolderSyncInfo info = getCachedFolderSync(folder);
						ICVSFolder cvsFolder = CVSWorkspaceRoot.getCVSFolderFor(folder);
						if (info != null) {
							SyncFileWriter.writeFolderConfig(cvsFolder, info);
						} else {
							SyncFileWriter.deleteFolderSync(cvsFolder);
						}
						monitor.worked(1);
					}
					it = changedResources.iterator();
					while (it.hasNext()) {
						IResource resource = (IResource) it.next();
						ResourceSyncInfo info = getCachedResourceSync(resource);
						ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
						if (info != null) {
							SyncFileWriter.writeResourceSync(cvsResource, info);
						} else {
							SyncFileWriter.deleteSync(cvsResource);
						}
						monitor.worked(1);
					}
					
					// broadcast events
					changedResources.addAll(changedFolders);				
					IResource[] resources = (IResource[]) changedResources.toArray(
						new IResource[changedResources.size()]);
					TeamPlugin.getManager().broadcastResourceStateChanges(resources);
					changedResources.clear();
					changedFolders.clear();
				} finally {
					monitor.done();
				}
			}
		}
		Assert.isTrue(nestingCount>= 0);
	}
	
	private static ISynchronizer getSynchronizer() {
		return ResourcesPlugin.getWorkspace().getSynchronizer();
	}
	
	private ResourceSyncInfo getCachedResourceSync(IResource resource) throws CVSException {
		try {
			byte[] bytes = getSynchronizer().getSyncInfo(RESOURCE_SYNC_KEY, resource);
			if(bytes==null) {
				return null;
			}
			return new ResourceSyncInfo(new String(bytes), null, null);
		} catch(CoreException e) {
			throw CVSException.wrapException(e);
		}
	}
	
	private void setCachedResourceSync(IResource resource, ResourceSyncInfo info) throws CVSException {
		try {
			if(info==null) {
				getSynchronizer().flushSyncInfo(RESOURCE_SYNC_KEY, resource, IResource.DEPTH_ZERO);
			} else {
				getSynchronizer().setSyncInfo(RESOURCE_SYNC_KEY, resource, info.getEntryLine(true).getBytes());
			}
		} catch(CoreException e) {
			throw CVSException.wrapException(e);
		}
	}
	
	private FolderSyncInfo getCachedFolderSync(IContainer folder) throws CVSException {
		try {
			byte[] bytes = getSynchronizer().getSyncInfo(FOLDER_SYNC_KEY, folder);
			if(bytes==null) {
				return null;
			}
			DataInputStream is = new DataInputStream(new ByteArrayInputStream(bytes));
			String repo = is.readUTF();
			String root = is.readUTF();
			String tag = is.readUTF();
			CVSTag cvsTag = null;
			boolean isStatic = is.readBoolean();
			if(!tag.equals("null")) {
				cvsTag = new CVSEntryLineTag(tag);
			}
			return new FolderSyncInfo(repo, root, cvsTag, isStatic);
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		} catch(IOException e) {
			throw CVSException.wrapException(e);
		}	
	}
	
	private void setCachedFolderSync(IContainer folder, FolderSyncInfo info) throws CVSException {
		try {
			if(info==null) {
				getSynchronizer().flushSyncInfo(FOLDER_SYNC_KEY, folder, IResource.DEPTH_INFINITE);
			} else {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				DataOutputStream os = new DataOutputStream(bos);
				os.writeUTF(info.getRepository());
				os.writeUTF(info.getRoot());
				CVSEntryLineTag tag = info.getTag();
				if(tag==null) {
					os.writeUTF("null");
				} else {
					os.writeUTF(info.getTag().toEntryLineFormat(false));
				}
				os.writeBoolean(info.getIsStatic());				
				getSynchronizer().setSyncInfo(FOLDER_SYNC_KEY, folder, bos.toByteArray());
				os.close();
			}
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		} catch(IOException e) {
			throw CVSException.wrapException(e);
		}
	}	
	
	private String[] getCachedFolderIgnores(IContainer folder) throws CVSException {
		try {
			byte[] bytes = getSynchronizer().getSyncInfo(IGNORE_SYNC_KEY, folder);
			if(bytes==null) {
				return null;
			}
			DataInputStream is = new DataInputStream(new ByteArrayInputStream(bytes));
			int count = is.readInt();
			String[] ignoreList = new String[count];
			for(int i = 0; i < count; ++i) {
				ignoreList[i] = is.readUTF();
			}
			return ignoreList;
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		} catch(IOException e) {
			throw CVSException.wrapException(e);
		}				
	}
	
	private void setCachedFolderIgnores(IContainer folder, String[] ignores) throws CVSException {
		try {
			if(ignores==null || ignores.length==0) {
				getSynchronizer().flushSyncInfo(IGNORE_SYNC_KEY, folder, IResource.DEPTH_ZERO);
			} else {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				DataOutputStream os = new DataOutputStream(bos);
				os.writeInt(ignores.length);
				for(int i = 0; i < ignores.length; ++i) {
					os.writeUTF(ignores[i]);
				}
				getSynchronizer().setSyncInfo(IGNORE_SYNC_KEY, folder, bos.toByteArray());
				os.close();
			}
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		} catch(IOException e) {
			throw CVSException.wrapException(e);
		}
	}
	
	/*
	 * Reads and caches the ResourceSyncInfos for this folder. If target is non-null, then 
	 * returns the ResourceSync for this resource is it is found.
	 */
	private ResourceSyncInfo getMetaResourceSyncForFolder(IContainer folder, IResource target) throws CVSException {
		ResourceSyncInfo info = null;
		if (folder!=null) {
			ResourceSyncInfo[] infos = SyncFileWriter.readEntriesFile(CVSWorkspaceRoot.getCVSFolderFor(folder));
			if (infos != null) {
				for (int i = 0; i < infos.length; i++) {
					ResourceSyncInfo syncInfo = infos[i];
					IResource peer;
					if (target!=null && target.getName().equals(syncInfo.getName())) {
						info = syncInfo;
						peer = target;
					} else {
						IPath path = new Path(syncInfo.getName());
						if (syncInfo.isDirectory()) {
							peer = folder.getFolder(path);
						} else {
							peer = folder.getFile(path);
						}
					}
					// may create a phantom if the sibling resource does not exist.
					setCachedResourceSync(peer, syncInfo);
				}
			}
		}
		return info;
	}
}