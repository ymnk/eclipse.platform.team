package org.eclipse.team.internal.ccvs.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
 
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.ResourceDeltaSyncHandler;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;

/**
 * Generate marker resoultions for a cvs remove marker
 */
public class CVSRemoveResolutionGenerator implements IMarkerResolutionGenerator {

	/*
	 * @see IMarkerResolutionGenerator#getResolutions(IMarker)
	 */
	public IMarkerResolution[] getResolutions(IMarker marker) {
		return new IMarkerResolution[] {
			new IMarkerResolution() {
				public String getLabel() {
					return "Mark as outgoing deletion";
				}
				public void run(IMarker marker) {
					try {
						IContainer parent = (IContainer)marker.getResource();
						String childName = (String)marker.getAttribute(ResourceDeltaSyncHandler.NAME_ATTRIBUTE);
						ICVSFile mFile = CVSWorkspaceRoot.getCVSFileFor(parent.getFile(new Path(childName)));
						if (mFile.isManaged()) {
							ResourceSyncInfo info = mFile.getSyncInfo();
							if (info.isAdded()) {
								mFile.unmanage();
								marker.delete();
							} else {
								mFile.setSyncInfo(new ResourceSyncInfo(info.getName(), info.DELETED_PREFIX + info.getRevision(), info.getTimeStamp(), info.getKeywordMode(), info.getTag(), info.getPermissions()));
								marker.setAttribute(IMarker.MESSAGE, childName + " is marked as an outgoing deletion");
							}
						}
					} catch (CVSException e) {
						CVSUIPlugin.log(e.getStatus());
					} catch (CoreException e) {
						CVSUIPlugin.log(e.getStatus());
					}
				}

			},
			new IMarkerResolution() {
				public String getLabel() {
					return "Commit deletion";
				}
				public void run(IMarker marker) {
					try {
						IContainer parent = (IContainer)marker.getResource();
						String childName = (String)marker.getAttribute(ResourceDeltaSyncHandler.NAME_ATTRIBUTE);
						ICVSFile mFile = CVSWorkspaceRoot.getCVSFileFor(parent.getFile(new Path(childName)));
						if (mFile.isManaged()) {
							ResourceSyncInfo info = mFile.getSyncInfo();
							if (info.isAdded()) {
								mFile.unmanage();
							} else {
								mFile.setSyncInfo(new ResourceSyncInfo(info.getName(), info.DELETED_PREFIX + info.getRevision(), info.getTimeStamp(), info.getKeywordMode(), info.getTag(), info.getPermissions()));
								// XXX Should commit!
							}
						}
						marker.delete();
					} catch (CVSException e) {
						CVSUIPlugin.log(e.getStatus());
					} catch (CoreException e) {
						CVSUIPlugin.log(e.getStatus());
					}
				}

			},
			new IMarkerResolution() {
				public String getLabel() {
					return "Undo deletion";
				}
				public void run(IMarker marker) {
					try {
						IContainer parent = (IContainer)marker.getResource();
						String childName = (String)marker.getAttribute(ResourceDeltaSyncHandler.NAME_ATTRIBUTE);
						ICVSFile mFile = CVSWorkspaceRoot.getCVSFileFor(parent.getFile(new Path(childName)));
						if (mFile.isManaged()) {
							ResourceSyncInfo info = mFile.getSyncInfo();
							if (info.isDeleted()) {
								mFile.setSyncInfo(new ResourceSyncInfo(info.getName(), info.getRevision(), info.getTimeStamp(), info.getKeywordMode(), info.getTag(), info.getPermissions()));
								// XXX Should refetch resource!!
							}
						}
						marker.delete();
					} catch (CVSException e) {
						CVSUIPlugin.log(e.getStatus());
					} catch (CoreException e) {
						CVSUIPlugin.log(e.getStatus());
					}
				}

			}
		};
	}

}
