/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.tags;

import java.util.*;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.Policy;

/**
 * Tag source that gets its tags from the projects exist in the workspace
 * and are mapped to the same repository as the seed project
 */
public class LocalProjectTagSource extends TagSource {
    
    public static TagSource create(IProject seedProject) {
        try {
            ICVSRemoteFolder seedFolder = ((ICVSRemoteFolder)CVSWorkspaceRoot.getRemoteResourceFor(seedProject));
            ICVSRemoteFolder[] remoteFolders = getProjectRemoteFolders(seedFolder.getRepository());
            if (remoteFolders.length == 1) {
                // There are no other projects to get tags from so return null
                return null;
            }
            return new LocalProjectTagSource(seedFolder, remoteFolders);
        } catch (CVSException e) {
            // Log and return null
            CVSUIPlugin.log(e);
            return null;
        }
    }
    
    private ICVSRemoteFolder seedFolder;
    private ICVSRemoteFolder[] remoteFolders;

    private  LocalProjectTagSource(ICVSRemoteFolder seedFolder, ICVSRemoteFolder[] remoteFolders) {
        this.seedFolder = seedFolder;
        this.remoteFolders = remoteFolders;
    }

    /*
     * Return the list of remote folders for the projects in the workspace mapped to the given repository
     */
    private static ICVSRemoteFolder[] getProjectRemoteFolders(ICVSRepositoryLocation repository) {
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        List result = new ArrayList();
        for (int i = 0; i < projects.length; i++) {
            IProject project = projects[i];
            try {
                if (project.isAccessible() && RepositoryProvider.isShared(project)) {
                    ICVSRemoteFolder remote = (ICVSRemoteFolder)CVSWorkspaceRoot.getRemoteResourceFor(project);
                    if (remote != null && remote.getRepository().equals(repository)) {
                        result.add(remote);
                    }
                }
            } catch (CVSException e) {
                // Log and continue
                CVSUIPlugin.log(e);
            }
        }
        return (ICVSRemoteFolder[]) result.toArray(new ICVSRemoteFolder[result.size()]);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.tags.TagSource#refresh(boolean, org.eclipse.core.runtime.IProgressMonitor)
     */
    public CVSTag[] refresh(boolean bestEffort, IProgressMonitor monitor) throws TeamException {
        // This tag source should not be refreshed
        return new CVSTag[0];
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.tags.TagSource#getLocation()
     */
    public ICVSRepositoryLocation getLocation() {
        return seedFolder.getRepository();
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.tags.TagSource#getShortDescription()
     */
    public String getShortDescription() {
        return Policy.bind("LocalProjectTagSource.0", Integer.toString(remoteFolders.length)); //$NON-NLS-1$
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.tags.TagSource#commit(org.eclipse.team.internal.ccvs.core.CVSTag[], boolean, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void commit(CVSTag[] tags, boolean replace, IProgressMonitor monitor) throws CVSException {
        // Does not commit tags
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.tags.TagSource#getCVSResources()
     */
    public ICVSResource[] getCVSResources() {
        return remoteFolders;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.tags.TagSource#getTags(int)
     */
    public CVSTag[] getTags(int type) {
        if (type == CVSTag.HEAD || type == BASE) {
            return super.getTags(type);
        }
        // Accumulate the tags for all folders
        Set allTags = new HashSet();
        for (int i = 0; i < remoteFolders.length; i++) {
            ICVSRemoteFolder folder = remoteFolders[i];
            CVSTag[] tags = SingleFolderTagSource.getTags(folder, type);
            allTags.addAll(Arrays.asList(tags));
        }
        // Exclude the tags for the seedFolder
        CVSTag[] tags = SingleFolderTagSource.getTags(seedFolder, type);
        allTags.removeAll(Arrays.asList(tags));
        return (CVSTag[]) allTags.toArray(new CVSTag[allTags.size()]);
    }

}
