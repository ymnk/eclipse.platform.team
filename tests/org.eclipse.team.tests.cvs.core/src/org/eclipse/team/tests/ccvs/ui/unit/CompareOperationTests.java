/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.ccvs.ui.unit;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.ui.operations.RemoteCompareOperation;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;

public class CompareOperationTests extends CVSOperationTest {

	public CompareOperationTests() {
		super();
	}

	public CompareOperationTests(String name) {
		super(name);
	}
	
	public static Test suite() {
		String testName = System.getProperty("eclipse.cvs.testName");
		if (testName == null) {
			TestSuite suite = new TestSuite(CompareOperationTests.class);
			return new CVSTestSetup(suite);
		} else {
			return new CVSTestSetup(new CompareOperationTests(testName));
		}
	}
	
	public void testCompareWithLatest() throws TeamException, CoreException {
		// Create a test project
		IProject project = createProject(new String[] { "file1.txt", "folder1/", "folder1/a.txt", "folder1/b.txt"});
		CVSTag v1 = new CVSTag("v1", CVSTag.VERSION);
		tagProject(project, v1, false);
		
		// Checkout and modify a copy (and commit the changes)
		IProject copy = checkoutCopy(project, "-copy");
		setContentsAndEnsureModified(copy.getFile("folder1/a.txt"));
		addResources(copy, new String[] { "folder1/newFile", "folder2/folder3/add.txt" }, false);
		deleteResources(copy, new String[] {"folder1/b.txt"}, false);
		getProvider(copy).checkin(new IResource[] {copy}, IResource.DEPTH_INFINITE, DEFAULT_MONITOR);

		// Run the compare operation
		ICVSRemoteFolder remoteResource = (ICVSRemoteFolder)CVSWorkspaceRoot.getRemoteResourceFor(project);
		RemoteCompareOperation op = new RemoteCompareOperation(null, new ICVSRemoteFolder[] { remoteResource }, v1, CVSTag.DEFAULT);
		run(op);

	}

}
