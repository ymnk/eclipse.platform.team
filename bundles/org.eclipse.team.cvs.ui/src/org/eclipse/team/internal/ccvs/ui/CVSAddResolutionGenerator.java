package org.eclipse.team.internal.ccvs.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
 
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.ccvs.core.ICVSResource;
import org.eclipse.team.core.IFileTypeRegistry;
import org.eclipse.team.core.TeamPlugin;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;

/**
 * Generate marker resoultions for a cvs remove marker
 */
public class CVSAddResolutionGenerator implements IMarkerResolutionGenerator {

	private String getKeywordModeFor(String extension) {
		IFileTypeRegistry registry = TeamPlugin.getFileTypeRegistry();
		if ((extension != null) && ("true".equals(registry.getValue(extension, "isText"))))   //$NON-NLS-1$ //$NON-NLS-2$
			return "";
		else
			return ResourceSyncInfo.BINARY_TAG;
	}
	
	/*
	 * @see IMarkerResolutionGenerator#getResolutions(IMarker)
	 */
	public IMarkerResolution[] getResolutions(IMarker marker) {
		return new IMarkerResolution[] {
			new IMarkerResolution() {
				public String getLabel() {
					return "Mark as outgoing addition";
				}
				public void run(IMarker marker) {
					try {
						IResource resource = marker.getResource();
						ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
						if ( ! cvsResource.isManaged()) {
							cvsResource.setSyncInfo(new ResourceSyncInfo(cvsResource.getName(), ResourceSyncInfo.ADDED_REVISION, ResourceSyncInfo.DUMMY_TIMESTAMP, getKeywordModeFor(resource.getFileExtension()), cvsResource.getParent().getFolderSyncInfo().getTag(), ResourceSyncInfo.DEFAULT_PERMISSIONS));
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
					return "Ignore";
				}
				public void run(IMarker marker) {
					try {
						ICVSResource resource = CVSWorkspaceRoot.getCVSResourceFor(marker.getResource());
						if ( resource.isManaged()) {
							resource.unmanage();
						}
						resource.setIgnored();
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
