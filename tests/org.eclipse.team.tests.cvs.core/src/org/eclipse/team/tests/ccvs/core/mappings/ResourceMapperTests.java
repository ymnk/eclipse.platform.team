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
import org.eclipse.team.internal.ccvs.core.resources.RemoteFolderTree;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFolderTreeBuilder;
import org.eclipse.team.internal.ccvs.ui.operations.WorkspaceResourceMapper;
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
    protected void update(ResourceMapping mapper, LocalOption[] options) throws Exception {
        SyncInfoTree incomingSet = getIncoming(mapper.getProjects());
        update(new ResourceMapping[] { mapper }, options);
        assertUpdate(mapper, incomingSet);
    }

    /**
     * Commit and check that all resources in containing project that should have been committed were and
     * that any not contained by the mappers were not.
     * @throws CoreException 
     * @see org.eclipse.team.tests.ccvs.core.EclipseTest#commit(org.eclipse.core.resources.mapping.IResourceMapper[], java.lang.String)
     */
    protected void commit(ResourceMapping mapper, String message) throws CoreException {
        SyncInfoTree set = getOutgoing(mapper.getProjects());
        commit(new ResourceMapping[] { mapper }, message);
        assertCommit(mapper, set);
    }
    
    /**
     * Tag the given resource mappings and assert that only the resources
     * within the mapping were tagged.
     * @throws CoreException 
     */
    protected void tag(ResourceMapping mapping, CVSTag tag) throws CoreException {
        tag(new ResourceMapping[] { mapping }, tag, false);
        assertTagged(mapping, tag);
    }
    
    private void assertTagged(ResourceMapping mapping, final CVSTag tag) throws CoreException {
        IProject[] projects = mapping.getProjects();
        final Map tagged = getTaggedRemoteFilesByPath(projects, tag);
        // Visit all the resources in the traversal and ensure that they are tagged
        visit(mapping, null, new IResourceVisitor() {
            public boolean visit(IResource resource) throws CoreException {
                if (resource.getType() == IResource.FILE) {
                    ICVSRemoteFile file = popRemote(resource, tagged);
                    assertNotNull("Resource was not tagged: " + resource.getFullPath(), file);
                }
                return true;
            }
        });
        
        // The tagged map should be empty after traversal
        for (Iterator iter = tagged.keySet().iterator(); iter.hasNext();) {
            String path = (String) iter.next();
            fail("Remote file " + path + " was tagged but should not have been.");
        }
    }

    private Map getTaggedRemoteFilesByPath(IProject[] projects, final CVSTag tag) throws CVSException {
        ICVSResource[] remotes = getRemoteTrees(projects, tag);
        final Map tagged = getFilesByPath(remotes);
        return tagged;
    }

    private ICVSResource[] getRemoteTrees(IProject[] projects, CVSTag tag) throws CVSException {
        List result = new ArrayList();
        for (int i = 0; i < projects.length; i++) {
            IProject project = projects[i];
            RemoteFolderTree tree = RemoteFolderTreeBuilder.buildRemoteTree(getRepository(), project, tag, DEFAULT_MONITOR);
            result.add(tree);
        }
        return (ICVSResource[]) result.toArray(new ICVSResource[result.size()]);
    }

    private Map getFilesByPath(ICVSResource[] remotes) throws CVSException {
        Map result = new HashMap();
        for (int i = 0; i < remotes.length; i++) {
            ICVSResource resource = remotes[i];
            collectFiles(resource, result);
        }
        return result;
    }

    private void collectFiles(ICVSResource resource, Map result) throws CVSException {
        if (resource.isFolder()) {
            ICVSResource[] members = ((ICVSFolder)resource).members(ICVSFolder.ALL_EXISTING_MEMBERS);
            for (int i = 0; i < members.length; i++) {
                ICVSResource member = members[i];
                collectFiles(member, result);
            }
        } else {
            result.put(resource.getRepositoryRelativePath(), resource);
        } 
    }

    private ICVSRemoteFile popRemote(IResource resource, Map tagged) throws CVSException {
        ICVSResource cvsResource = getCVSResource(resource);
        ICVSRemoteFile remote = (ICVSRemoteFile)tagged.get(cvsResource.getRepositoryRelativePath());
        if (remote != null) {
            tagged.remove(remote.getRepositoryRelativePath());
        }
        return remote;
    }
    
    private ResourceMapping asResourceMapping(IResource[] resources, int depth) {
        return new WorkspaceResourceMapper(resources, depth);
    }
    
    private void assertUpdate(ResourceMapping mapper, final SyncInfoTree set) throws Exception {
        final Exception[] exception = new Exception[] { null };
        visit(mapper, new SyncInfoSetTraveralContext(set), new IResourceVisitor() {
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

    private void assertCommit(ResourceMapping mapper, final SyncInfoTree set) throws CoreException {
        visit(mapper, new SyncInfoSetTraveralContext(set), new IResourceVisitor() {
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

    private SyncInfoTree getIncoming(IProject[] projects) throws TeamException {
        CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().refresh(projects, IResource.DEPTH_INFINITE, DEFAULT_MONITOR);
        SyncInfoTree set = getAllOutOfSync(projects);
        set.removeOutgoingNodes();
        set.removeConflictingNodes();
        return set;
    }
    
    private SyncInfoTree getOutgoing(IProject[] projects) {
        SyncInfoTree set = getAllOutOfSync(projects);
        set.removeIncomingNodes();
        set.removeConflictingNodes();
        return set;
    }

    private SyncInfoTree getAllOutOfSync(IProject[] projects) {
        SyncInfoTree set = new SyncInfoTree();
        CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().collectOutOfSync(projects, IResource.DEPTH_INFINITE, set, DEFAULT_MONITOR);
        return set;
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
       if (context != null)
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
        commit(asResourceMapping(new IResource[] { copy }, IResource.DEPTH_INFINITE), "A commit message");
        
        // Update the other project using depth one and ensure we got only what was asked for
        update(asResourceMapping(new IResource[] { project }, IResource.DEPTH_ONE), null);
        
        // Update the specific file
        update(asResourceMapping(new IResource[] { project.getFile("folder1/a.txt") }, IResource.DEPTH_ONE), null);
        
        assertEquals(project, copy);
    }

    public void testCommit() throws Exception {
        // Create a test project, import it into cvs and check it out
        IProject project = createProject("testCommit", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt" });

        // Check the project out under a different name
        IProject copy = checkoutCopy(project, "-copy");
        
        // Perform some operations on the copy and commit only the top level
        addResources(copy, new String[] { "added.txt", "folder2/", "folder2/added.txt" }, false);
        setContentsAndEnsureModified(copy.getFile("changed.txt"));
        deleteResources(new IResource[] {copy.getFile("deleted.txt")});
        commit(asResourceMapping(new IResource[] { copy }, IResource.DEPTH_ONE), "A commit message");
        
        // Now commit the file specifically
        commit(asResourceMapping(new IResource[] { copy.getFile("folder1/a.txt") }, IResource.DEPTH_ZERO), "A commit message");
        
        // Update everything
        update(asResourceMapping(new IResource[] { project }, IResource.DEPTH_INFINITE), null);
        assertEquals(project, copy);
    }
    
    public void testTag() throws Exception {
        // Create a test project, import it into cvs and check it out
        IProject project = createProject("testTag", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt" });

        tag(asResourceMapping(new IResource[] { project }, IResource.DEPTH_ONE), new CVSTag("v1", CVSTag.VERSION));
        tag(asResourceMapping(new IResource[] { project.getFile("folder1/a.txt") }, IResource.DEPTH_ZERO), new CVSTag("v2", CVSTag.VERSION));
    }
    
}
