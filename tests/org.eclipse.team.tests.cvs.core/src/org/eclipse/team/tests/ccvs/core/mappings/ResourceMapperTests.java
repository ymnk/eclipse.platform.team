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
package org.eclipse.team.tests.ccvs.core.mappings;

import java.io.IOException;
import java.util.*;

import junit.framework.Test;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoTree;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.tests.ccvs.core.EclipseTest;

/**
 * Tests for using CVS operations with deep and shallow resource mappings.
 */
public class ResourceMapperTests extends EclipseTest {

    public ResourceMapperTests() {
        super();
    }

    public ResourceMapperTests(String name) {
        super(name);
    }

    public static Test suite() {
        return suite(ResourceMapperTests.class);
    }
    
    /**
     * Update the resources contained in the given mappers and ensure that the
     * update was performed properly by comparing the result with the reference projects.
     * @throws Exception 
     */
    protected void update(ResourceMapping[] mappers, IProject targetProject, LocalOption[] options) throws Exception {
        SyncInfoTree incomingSet = getIncoming(targetProject);
        update(mappers, options);
        assertUpdate(mappers, incomingSet);
    }

    /**
     * Commit and check that all resources in containing project that should have been committed were and
     * that any not contained by the mappers were not.
     * @throws CoreException 
     * @see org.eclipse.team.tests.ccvs.core.EclipseTest#commit(org.eclipse.core.resources.mapping.IResourceMapper[], java.lang.String)
     */
    protected void commit(ResourceMapping[] mappers, IProject containingProject, String message) throws CoreException {
        SyncInfoTree set = getOutgoing(containingProject);
        commit(mappers, message);
        assertCommit(mappers, set);
    }
    
    private void assertUpdate(ResourceMapping[] mappers, final SyncInfoTree set) throws Exception {
        final Exception[] exception = new Exception[] { null };
        visit(mappers, new SyncInfoSetTraveralContext(set), new IResourceVisitor() {
            public boolean visit(IResource resource) throws CoreException {
                SyncInfo info = set.getSyncInfo(resource);
                if (info != null) {
                    set.remove(resource);
                    try {
                        // Assert that the local sync info matches the remote info
                        assertEquals(resource.getParent().getFullPath(), getCVSResource(resource), (ICVSResource)info.getRemote(), false, false);
                    } catch (CVSException e) {
                        exception[0] = e;
                    } catch (CoreException e) {
                        exception[0] = e;
                    } catch (IOException e) {
                        exception[0] = e;
                    }
                }
                return true;
            }
        });
        if (exception[0] != null) throw exception[0];
        
        // check the the state of the remaining resources has not changed
        assertUnchanged(set);
    }

    private void assertCommit(ResourceMapping[] mappers, final SyncInfoTree set) throws CoreException {
        visit(mappers, new SyncInfoSetTraveralContext(set), new IResourceVisitor() {
            public boolean visit(IResource resource) throws CoreException {
                SyncInfo info = set.getSyncInfo(resource);
                if (info != null) {
                    set.remove(resource);
                    assertTrue("Committed resource is not in-sync: " + resource.getFullPath(), getSyncInfo(resource).getKind() == SyncInfo.IN_SYNC);
                }
                return true;
            }
        });
        // check the the state of the remaining resources has not changed
        assertUnchanged(set);
    }

    /*
     * Assert that the state of the resources in the set have not changed
     */
    private void assertUnchanged(SyncInfoTree set) throws TeamException {
        SyncInfo[] infos = set.getSyncInfos();
        for (int i = 0; i < infos.length; i++) {
            SyncInfo info = infos[i];
            assertUnchanged(info);
        }
    }

    private void assertUnchanged(SyncInfo info) throws TeamException {
        SyncInfo current = getSyncInfo(info.getLocal());
        assertEquals("The sync info changed for " + info.getLocal().getFullPath(), info, current);
    }

    private SyncInfo getSyncInfo(IResource local) throws TeamException {
        return CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().getSyncInfo(local);
    }

    private SyncInfoTree getIncoming(IProject targetProject) throws TeamException {
        CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().refresh(new IResource[] { targetProject }, IResource.DEPTH_INFINITE, DEFAULT_MONITOR);
        SyncInfoTree set = getAllOutOfSync(targetProject);
        set.removeOutgoingNodes();
        set.removeConflictingNodes();
        return set;
    }
    
    private SyncInfoTree getOutgoing(IProject project) {
        SyncInfoTree set = getAllOutOfSync(project);
        set.removeIncomingNodes();
        set.removeConflictingNodes();
        return set;
    }

    private SyncInfoTree getAllOutOfSync(IProject project) {
        SyncInfoTree set = new SyncInfoTree();
        CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().collectOutOfSync(new IResource[] { project }, IResource.DEPTH_INFINITE, set, DEFAULT_MONITOR);
        return set;
    }

    private void visit(ResourceMapping[] mappers, ResourceMappingContext context, IResourceVisitor visitor) throws CoreException {
        for (int i = 0; i < mappers.length; i++) {
            ResourceMapping mapper = mappers[i];
            visit(mapper, context, visitor);
        }
    }
    
    private void visit(ResourceMapping mapper, ResourceMappingContext context, IResourceVisitor visitor) throws CoreException {
        ResourceTraversal[] traversals = mapper.getTraversals(context, null);
        for (int i = 0; i < traversals.length; i++) {
            ResourceTraversal traversal = traversals[i];
            visit(traversal, context, visitor);
        }
    }

    private void visit(ResourceTraversal traversal, ResourceMappingContext context, IResourceVisitor visitor) throws CoreException {
        IResource[] resources = traversal.getResources();
        for (int i = 0; i < resources.length; i++) {
            IResource resource = resources[i];
            visit(resource, visitor, context, traversal.getDepth());
        }
    }

    private void visit(IResource resource, IResourceVisitor visitor, ResourceMappingContext context, int depth) throws CoreException {
       if (!visitor.visit(resource) || depth == IResource.DEPTH_ZERO || resource.getType() == IResource.FILE) return;
       Set members = new HashSet();
       members.addAll(Arrays.asList(((IContainer)resource).members(false)));
       members.addAll(Arrays.asList(context.fetchMembers((IContainer)resource, DEFAULT_MONITOR)));
       for (Iterator iter = members.iterator(); iter.hasNext();) {
           IResource member = (IResource) iter.next();
           visit(member, visitor, context, depth == IResource.DEPTH_ONE ? IResource.DEPTH_ZERO : IResource.DEPTH_INFINITE);
       }
    }

    public void testUpdate() throws Exception {
        // Create a test project, import it into cvs and check it out
        IProject project = createProject("testUpdate", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt" });

        // Check the project out under a different name
        IProject copy = checkoutCopy(project, "-copy");
        
        // Perform some operations on the copy and commit them all
        addResources(copy, new String[] { "added.txt", "folder2/", "folder2/added.txt" }, false);
        setContentsAndEnsureModified(copy.getFile("changed.txt"));
        deleteResources(new IResource[] {copy.getFile("deleted.txt")});
        commit(asResourceMappers(new IResource[] { copy }, IResource.DEPTH_INFINITE), copy, "A commit message");
        
        // Update the other project using depth one and ensure we got only what was asked for
        update(asResourceMappers(new IResource[] { project }, IResource.DEPTH_ONE), project, null);
    }
    
}
