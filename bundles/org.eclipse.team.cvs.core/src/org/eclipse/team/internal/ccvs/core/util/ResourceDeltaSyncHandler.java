package org.eclipse.team.internal.ccvs.core.util;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
 
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.team.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.ccvs.core.CVSTeamProvider;
import org.eclipse.team.ccvs.core.ICVSFile;
import org.eclipse.team.ccvs.core.ICVSFolder;
import org.eclipse.team.ccvs.core.ICVSResource;
import org.eclipse.team.core.IResourceStateChangeListener;
import org.eclipse.team.core.ITeamProvider;
import org.eclipse.team.core.TeamPlugin;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.Policy;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;

/**
 * Listen for the addition of orphaned subtrees as a result of a copy or move.
 * 
 * Listen in IResourceChangeEvent.PRE_AUTO_BUILD so that other interested parties 
 * (most notably, the file synchronizer) will receive up to date notifications
 */
public class ResourceDeltaSyncHandler implements IResourceDeltaVisitor, IResourceStateChangeListener {

	public static final String DELETION_MARKER = "org.eclipse.team.cvs.core.cvsremove";
	public static final String ADDITION_MARKER = "org.eclipse.team.cvs.core.cvsadd";
	
	public static final String NAME_ATTRIBUTE = "name";
	
	private static IResourceChangeListener listener;
	private static ResourceDeltaSyncHandler visitor;
	
	protected IMarker createDeleteMarker(IResource resource) {
		try {
			IMarker marker = resource.getParent().createMarker(DELETION_MARKER);
			marker.setAttribute("name", resource.getName());
			marker.setAttribute(IMarker.MESSAGE, resource.getName() + " has been deleted locally");
			return marker;
		} catch (CoreException e) {
			Util.logError("Error creating deletion marker", e);
		}
		return null;
	}
	
	protected IMarker createAdditonMarker(IResource resource) {
		try {
			IMarker marker = resource.createMarker(ADDITION_MARKER);
			marker.setAttribute(IMarker.MESSAGE, resource.getName() + " is an unmanaged local addition");
			return marker;
		} catch (CoreException e) {
			Util.logError("Error creating addition marker", e);
		}
		return null;
	}
	
	protected IMarker getAdditionMarker(IResource resource) throws CoreException {
   		IMarker[] markers = resource.findMarkers(ADDITION_MARKER, false, IResource.DEPTH_ZERO);
   		if (markers.length == 1) {
   			return markers[0];
   		}
		return null;
	}
	
	protected IMarker getDeletionMarker(IResource resource) throws CoreException {
		String name = resource.getName();
   		IMarker[] markers = resource.getParent().findMarkers(DELETION_MARKER, false, IResource.DEPTH_ZERO);
   		for (int i = 0; i < markers.length; i++) {
			IMarker iMarker = markers[i];
			String markerName = (String)iMarker.getAttribute(NAME_ATTRIBUTE);
			if (markerName.equals(name))
				return iMarker;
		}

		return null;
	}
	
	public static IResource getResourceFor(IProject container, IResource destination, IPath originating) {
		switch(destination.getType()) {
			case IResource.FILE : return container.getFile(originating); 			
			case IResource.FOLDER: return container.getFolder(originating);
			case IResource.PROJECT: return ResourcesPlugin.getWorkspace().getRoot().getProject(originating.toString());
		}
		return destination;
	}
	
	/**
	 * @see IResourceDeltaVisitor#visit(IResourceDelta)
	 */
	public boolean visit(IResourceDelta delta) throws CoreException {
		IResource resource = delta.getResource();
		IProject project = resource.getProject();
		boolean movedTo = (delta.getFlags() & IResourceDelta.MOVED_TO) > 0;
		boolean movedFrom = (delta.getFlags() & IResourceDelta.MOVED_FROM) > 0;
		switch (delta.getKind()) {
			case IResourceDelta.ADDED :
				if (resource.getType() == IResource.FOLDER) {
					return ! handleOrphanedSubtree((IContainer)resource);
				} else if (resource.getType() == IResource.FILE) {
					handleAddedFile((IFile)resource);
				}
				break;
			case IResourceDelta.REMOVED :
				if (resource.getType() == IResource.FOLDER) {
					// Only record files as there's nothing we can do about folders
				} else if (resource.getType() == IResource.FILE) {
					if (movedTo) {
						IPath target = delta.getMovedToPath();
						if (target.segment(0).equals(project.getName())) {
							handleMovedFile(project, (IFile)resource, project.getFile(target.removeFirstSegments(1)));
						} else {
							handleDeletedFile((IFile)resource);
						}
					} else {
						handleDeletedFile((IFile)resource);
					}
				}
				break;
			case IResourceDelta.CHANGED :
				// This state means there is a resource before and after but changes were made by deleting and moving.
				// For files, we shouldn'd do anything.
				// For folders, we should purge the CVS info
				if (resource.getType() == IResource.FOLDER) {
					// When folders are moved, purge the CVS folders
					if (movedFrom)
						return ! handleOrphanedSubtree((IContainer)resource);
				}
				break;
		}
		return true;
	}
	
	/*
	 * Determine if the container is an orphaned subtree. 
	 * If it is, handle it and return true. 
	 * Otherwise, return false
	 */
	private boolean handleOrphanedSubtree(IContainer resource) {
		try {
			ICVSFolder mFolder = CVSWorkspaceRoot.getCVSFolderFor((IContainer)resource);
			if (mFolder.isCVSFolder() && ! mFolder.isManaged() && mFolder.getParent().isCVSFolder()) {
				mFolder.unmanage();
				return true;
			}
		} catch (CVSException e) {
			CVSProviderPlugin.log(e);
		}
		return false;
	}
	
	/*
	 * Mark deleted managed files as outgoing deletions
	 */
	private void handleDeletedFile(IFile resource) {
		try {
			ICVSFile mFile = CVSWorkspaceRoot.getCVSFileFor((IFile)resource);
			if (mFile.isManaged()) {
				ResourceSyncInfo info = mFile.getSyncInfo();
				if (info.isAdded()) {
					mFile.unmanage();
				} else {
					createDeleteMarker(resource);
					//mFile.setSyncInfo(new ResourceSyncInfo(info.getName(), info.DELETED_PREFIX + info.getRevision(), info.getTimeStamp(), info.getKeywordMode(), info.getTag(), info.getPermissions()));
				}
			}
			// XXX If .cvsignore was deleted, we may have unmanaged additions in the same folder
		} catch (CVSException e) {
			CVSProviderPlugin.log(e);
		}
	}

	/*
	 * Handle the case where an added file has the same name as a "cvs removed" file
	 * by restoring the sync info to what it was before the delete
	 */
	private void handleAddedFile(IFile resource) {
		try {
			ICVSFile mFile = CVSWorkspaceRoot.getCVSFileFor((IFile)resource);
			if (mFile.isManaged()) {
				ResourceSyncInfo info = mFile.getSyncInfo();
				if (info.isDeleted()) {
					// Handle a replaced deletion
					mFile.setSyncInfo(new ResourceSyncInfo(info.getName(), info.getRevision(), info.getTimeStamp(), info.getKeywordMode(), info.getTag(), info.getPermissions()));
					// XXX Need to remove delete marker!
				}
			} else if ( ! mFile.isIgnored()) {
				createAdditonMarker(resource);
			}
		} catch (CVSException e) {
			CVSProviderPlugin.log(e);
		}
	}
	
	/*
	 * Managed new location if old location was managed.
	 * Also ensure that replaced deletions are handled.
	 */
	private void handleMovedFile(IProject project, IFile fromResource, IFile toResource) {
		try {
			
			ResourceSyncInfo fromInfo = null;
			ICVSFile fromFile = CVSWorkspaceRoot.getCVSFileFor(fromResource);
			// If the from file was managed mark it as an outgoing deletion
			if (fromFile.isManaged()) {
				fromInfo = fromFile.getSyncInfo();
				if (fromInfo.isAdded()) {
					fromFile.unmanage();
				} else {
					createDeleteMarker(fromResource);
					//fromFile.setSyncInfo(new ResourceSyncInfo(fromInfo.getName(), fromInfo.DELETED_PREFIX + fromInfo.getRevision(), fromInfo.getTimeStamp(), fromInfo.getKeywordMode(), fromInfo.getTag(), fromInfo.getPermissions()));
				}
			}
			
			ICVSFile toFile = CVSWorkspaceRoot.getCVSFileFor(toResource);
			// If the to file is not managed, mark it as an outgoing addition
			if (toFile.isManaged()) {
				ResourceSyncInfo info = toFile.getSyncInfo();
				if (info.isDeleted()) {
					// Handle a replaced deletion
					toFile.setSyncInfo(new ResourceSyncInfo(info.getName(), info.getRevision(), info.getTimeStamp(), info.getKeywordMode(), info.getTag(), info.getPermissions()));
				}
			} else if (fromInfo != null) {
				toFile.setSyncInfo(new ResourceSyncInfo(toFile.getName(), ResourceSyncInfo.ADDED_REVISION, ResourceSyncInfo.DUMMY_TIMESTAMP, fromInfo.getKeywordMode(), fromInfo.getTag(), fromInfo.getPermissions()));
			}
			
		} catch (CVSException e) {
			CVSProviderPlugin.log(e);
		}
	}
	
	public static void startup() {		
		if (visitor == null)
			visitor = new ResourceDeltaSyncHandler();
		if (listener == null)
			listener = new IResourceChangeListener() {
				public void resourceChanged(IResourceChangeEvent event) {
					try {
						IResourceDelta root = event.getDelta();
						IResourceDelta[] projectDeltas = root.getAffectedChildren();
						for (int i = 0; i < projectDeltas.length; i++) {							
							IResourceDelta delta = projectDeltas[i];
							IResource resource = delta.getResource();
							ITeamProvider provider = TeamPlugin.getManager().getProvider(resource);

							// if a project is moved the originating project will not be associated with the CVS provider
							// however listeners will probably still be interested in the move delta.	
							if ((delta.getFlags() & IResourceDelta.MOVED_TO) > 0) {																
								IResource destination = getResourceFor(resource.getProject(), resource, delta.getMovedToPath());
								provider = TeamPlugin.getManager().getProvider(destination);
							}
							
							if (provider instanceof CVSTeamProvider) {
								delta.accept(visitor);
							}
						}
					} catch (CoreException e) {
						Util.logError(Policy.bind("ResourceDeltaVisitor.visitError"), e);//$NON-NLS-1$
					}
				}
			};
		ResourcesPlugin.getWorkspace().addResourceChangeListener(listener, IResourceChangeEvent.PRE_AUTO_BUILD);
		TeamPlugin.getManager().addResourceStateChangeListener(visitor);
	}
	
	public static void shutdown() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(listener);
	}
	
	/*
	 * @see IResourceStateChangeListener#resourceStateChanged(IResource[])
	 */
	public void resourceStateChanged(IResource[] changedResources) {
		for (int i = 0; i < changedResources.length; i++) {
			try {
				IResource resource = changedResources[i];
				if (resource.getType() == IResource.FILE) {
					ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
					if (cvsResource.isManaged()) {
						IMarker marker = getAdditionMarker(resource);
						if (marker != null)
							marker.delete();
					} else if (cvsResource.isIgnored()) {
						// XXX I doubt that ignore actions result in state changes
						IMarker marker = getAdditionMarker(resource);
						if (marker != null)
							marker.delete();
					} else if ( ! cvsResource.exists()) {
						IMarker marker = getDeletionMarker(resource);
						if (marker != null)
							marker.delete();
					}
				}
			} catch (CVSException e) {
				Util.logError("Error updating marker state", e);
			} catch (CoreException e) {
				Util.logError("Error updating marker state", e);
			}
		}
	}

}