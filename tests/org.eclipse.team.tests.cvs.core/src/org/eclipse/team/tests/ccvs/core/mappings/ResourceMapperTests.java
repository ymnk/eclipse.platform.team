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

import junit.framework.Test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
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
    
    public void testUpdate() throws CoreException, IOException {
        // Create a test project, import it into cvs and check it out
        IProject project = createProject("testUpdate", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt" });

        // Check the project out under a different name
        IProject copy = checkoutCopy(project, "-copy");
        
        // Perform some operations on the copy and commit them all
        addResources(copy, new String[] { "added.txt", "folder2/", "folder2/added.txt" }, false);
        setContentsAndEnsureModified(copy.getFile("changed.txt"));
        deleteResources(new IResource[] {copy.getFile("deleted.txt")});
        commitResources(asResourceMapper(copy, IResource.DEPTH_INFINITE));
        
        // Update the othe rproject and ensure we got only what was asked for
        updateProject(asResourceMapper(project, IResource.DEPTH_ONE), null, false);
        assertEquals(project, copy);
    }
    
}
