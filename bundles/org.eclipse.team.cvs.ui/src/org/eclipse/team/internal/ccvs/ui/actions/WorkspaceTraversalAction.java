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
package org.eclipse.team.internal.ccvs.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.IResourceMapper;
import org.eclipse.core.resources.mapping.ITraversal;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;


/**
 * A specialized workspace actions that operates on resource traversals
 * instead of resources/
 */
public abstract class WorkspaceTraversalAction extends WorkspaceAction {

    /**
     * Override to use the roots of the traversals as the selected resources.
     * On it's own, this would be enough to make the actions work but all the operations
     * would be deep (which is bad) so subclasses will need to look for traversals
     * when executed.
     * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#getSelectedResources()
     */
    protected IResource[] getSelectedResourcesWithOverlap() {
        try {
            // Get all the traversals since enablement may be based on entire selection
            ITraversal[] traversals = getSelectedTraversals(null);
            Set resources = new HashSet();
            for (int i = 0; i < traversals.length; i++) {
                ITraversal traversal = traversals[i];
                resources.addAll(Arrays.asList(traversal.getResources()));
            }
            return (IResource[]) resources.toArray(new IResource[resources.size()]);
        } catch (TeamException e) {
            CVSUIPlugin.log(e);
            return new IResource[0];
        }
    }
    

    /**
     * Return the selected mappings that contain resources 
     * within a CVS managed project.
     * @return the selected mappings that contain resources 
     * within a CVS managed project
     */
    protected IResourceMapper[] getCVSResourceMappings() {
        return getSelectedResourceMappers(CVSProviderPlugin.getTypeId());
    }
    
    /**
     * Return the selected traversals that are on CVS resources.
     * @return the selected traversals that are on CVS resources
     * @throws InvocationTargetException 
     */
    private /* temporarily */ ITraversal[] getSelectedTraversals() throws InvocationTargetException {
        try {
            return getSelectedTraversals(CVSProviderPlugin.getTypeId());
        } catch (TeamException e) {
            throw new InvocationTargetException(e);
        }
    }

}
