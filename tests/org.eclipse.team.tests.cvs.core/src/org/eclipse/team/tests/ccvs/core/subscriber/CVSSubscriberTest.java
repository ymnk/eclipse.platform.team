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
package org.eclipse.team.tests.ccvs.core.subscriber;

import java.io.IOException;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.sync.RemoteSyncElement;
import org.eclipse.team.core.sync.SyncInfo;
import org.eclipse.team.core.sync.SyncTreeSubscriber;
import org.eclipse.team.core.sync.TeamProvider;
import org.eclipse.team.internal.ccvs.core.CVSSyncInfo;
import org.eclipse.team.internal.ccvs.core.CVSWorkspaceSubscriber;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;
import org.eclipse.team.tests.ccvs.core.EclipseTest;

/**
 * This class tests the CVSWorkspaceSubscriber
 */
public class CVSSubscriberTest extends EclipseTest {

	/**
	 * Constructor for CVSProviderTest
	 */
	public CVSSubscriberTest() {
		super();
	}

	/**
	 * Constructor for CVSProviderTest
	 */
	public CVSSubscriberTest(String name) {
		super(name);
	}

	public static Test suite() {
		String testName = System.getProperty("eclipse.cvs.testName");
		if (testName == null) {
			TestSuite suite = new TestSuite(CVSSubscriberTest.class);
			return new CVSTestSetup(suite);
		} else {
			return new CVSTestSetup(new CVSSubscriberTest(testName));
		}
	}
	
	protected SyncTreeSubscriber getSubscriber() {
		SyncTreeSubscriber subscriber = TeamProvider.getSubscriber(CVSWorkspaceSubscriber.ID);
		if (subscriber == null) fail("The CVS sync subsciber is not registered");
		return subscriber;
	}
	
	/*
	 * Refresh the subscriber for the given resource
	 */
	protected void refresh(IResource resource) throws TeamException {
		SyncTreeSubscriber subscriber = getSubscriber();
		subscriber.refresh(new IResource[] { resource}, IResource.DEPTH_INFINITE, DEFAULT_MONITOR);
	}
	
	/*
	 * Assert that the specified resources in the subscriber have the specified sync kind
	 * Ignore conflict types if they are not specified in the assert statement
	 */
	protected void assertSyncEquals(String message, IContainer root, String[] resourcePaths, int[] syncKinds) throws CoreException, TeamException {
		assertTrue(resourcePaths.length == syncKinds.length);
		refresh(root);
		IResource[] resources = getResources(root, resourcePaths);
		SyncTreeSubscriber subscriber = getSubscriber();
		for (int i=0;i<resources.length;i++) {
			int conflictTypeMask = 0x0F; // ignore manual and auto merge sync types for now.
			IResource resource = resources[i];
			SyncInfo info = subscriber.getSyncInfo(resource, DEFAULT_MONITOR);
			int kind = info.getKind() & conflictTypeMask;
			int kindOther = syncKinds[i] & conflictTypeMask;
			assertTrue(message + ": improper sync state for " + resources[i] + " expected " + 
					   RemoteSyncElement.kindToString(kindOther) + " but was " +
					   RemoteSyncElement.kindToString(kind), kind == kindOther);
		}
		
	}
	
	/* 
	 * Assert that the named resources have no local resource or sync info
	 */
	protected void assertDeleted(String message, IContainer root, String[] resourcePaths) throws CoreException, TeamException {
		IResource[] resources = getResources(root, resourcePaths);
		for (int i=0;i<resources.length;i++) {
			try {
				if (! resources[i].exists())
					break;
			} catch (AssertionFailedError e) {
				break;
			}
			assertTrue(message + ": resource " + resources[i] + " still exists in some form", false);
		}
	}
	
	protected void makeInSync(IContainer root, String[] resourcePaths) throws CoreException, TeamException {
		IResource[] resources = getResources(root, resourcePaths);
		SyncTreeSubscriber subscriber = getSubscriber();
		for (int i=0;i<resources.length;i++) {
			CVSSyncInfo info = (CVSSyncInfo) subscriber.getSyncInfo(resources[i], DEFAULT_MONITOR);
			info.makeInSync(DEFAULT_MONITOR);
		}
	}
	
	/*
	 * Perform a simple test that checks for the different types of incoming changes
	 */
	public void testIncomingChanges() throws TeamException, CoreException, IOException {
		// Create a test project
		IProject project = createProject("testIncomingChanges", new String[] { "file1.txt", "folder1/", "folder1/a.txt", "folder1/b.txt"});
		
		// Checkout and modify a copy
		IProject copy = checkoutCopy(project, "-copy");
		setContentsAndEnsureModified(copy.getFile("folder1/a.txt"));
		addResources(copy, new String[] { "folder2/folder3/add.txt" }, false);
		deleteResources(copy, new String[] {"folder1/b.txt"}, false);
		getProvider(copy).checkin(new IResource[] {copy}, IResource.DEPTH_INFINITE, DEFAULT_MONITOR);

		// Get the sync tree for the project
		assertSyncEquals("testIncomingChanges", project, 
			new String[] { "file1.txt", "folder1/", "folder1/a.txt", "folder1/b.txt", "folder2/", "folder2/folder3/", "folder2/folder3/add.txt"}, 
			new int[] {
				SyncInfo.IN_SYNC,
				SyncInfo.IN_SYNC,
				SyncInfo.INCOMING | SyncInfo.CHANGE,
				SyncInfo.INCOMING | SyncInfo.DELETION,
				SyncInfo.INCOMING | SyncInfo.ADDITION,
				SyncInfo.INCOMING | SyncInfo.ADDITION,
				SyncInfo.INCOMING | SyncInfo.ADDITION});
				
		// Catch up to the incoming changes
		// XXX SPECIAL CASE: Update must be run on a resource whose parent is managed at the time of the update.
		makeInSync(project, new String[] {"folder2/", "folder2/folder3/"});
		updateResources(project, new String[] {"folder1/a.txt", "folder1/b.txt", /* "folder2/", "folder2/folder3/", */ "folder2/folder3/add.txt"}, false);
		
		// Verify that we are in sync (except for "folder1/b.txt", which was deleted)
		assertSyncEquals("testIncomingChanges", project, 
			new String[] { "file1.txt", "folder1/", "folder1/a.txt", "folder2/", "folder2/folder3/", "folder2/folder3/add.txt"}, 
			new int[] {
				SyncInfo.IN_SYNC,
				SyncInfo.IN_SYNC,
				SyncInfo.IN_SYNC,
				SyncInfo.IN_SYNC,
				SyncInfo.IN_SYNC,
				SyncInfo.IN_SYNC});
		
		// Ensure "folder1/b.txt" was deleted
		assertDeleted("testIncomingChanges", project, new String[] {"folder1/b.txt"});
				
		// Verify that the copy equals the original
		assertEquals(project, copy);
	}
}
