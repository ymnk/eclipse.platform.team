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
package org.eclipse.team.tests.ui.synchronize;

import java.util.*;

import junit.framework.Test;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.MutableSyncInfoSet;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.tests.core.TeamTest;
import org.eclipse.team.tests.ui.views.ContentProviderTestView;
import org.eclipse.team.tests.ui.views.TestTreeViewer;
import org.eclipse.team.ui.synchronize.viewers.SyncInfoDiffNode;

/**
 * Tests for the SyncInfoSet content providers.
 */
public class SyncInfoSetContentProviderTest extends TeamTest {
	
	public static final TestSubscriber subscriber = new TestSubscriber();
	private MutableSyncInfoSet set;
	private ContentProviderTestView view;
	
	/**
	 * Constructor for CVSProviderTest
	 */
	public SyncInfoSetContentProviderTest() {
		super();
	}

	/**
	 * Constructor for CVSProviderTest
	 */
	public SyncInfoSetContentProviderTest(String name) {
		super(name);
	}

	public static Test suite() {
		return suite(SyncInfoSetContentProviderTest.class);
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		set = new MutableSyncInfoSet();
		view = ContentProviderTestView.findViewInActivePage(null);
		view.setInput(set);
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		set = null;
		super.tearDown();
	}
	
	/*
	 * This method creates a project with the given resources, imports
	 * it to CVS and checks it out
	 */
	protected IProject createProject(String prefix, String[] resources) throws CoreException {
		IProject project = getUniqueTestProject(prefix);
		buildResources(project, resources, true);
		return project;
	}
	
	/*
	 * Create a test project using the currently running test case as the project name prefix
	 */
	protected IProject createProject(String[] resources) throws CoreException {
		return createProject(getName(), resources);
	}
		
	private void adjustSet(MutableSyncInfoSet set, IProject project, String[] resourceStrings, int[] syncKind) throws TeamException {
		IResource[] resources = buildResources(project, resourceStrings);
		set.beginInput();
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			int kind = syncKind[i];
			if (kind == SyncInfo.IN_SYNC) {
				set.remove(resource);
			} else {
				SyncInfo newInfo = subscriber.getSyncInfo(resource, kind);
				if (set.getSyncInfo(resource) != null) {
					set.changed(newInfo);
				} else {
					set.add(newInfo);
				}
			}
		}
		set.endInput(new NullProgressMonitor());
		// Process any asyncs taht may be updating the view
		while (Display.getCurrent().readAndDispatch()) {}
	}

	/**
	 * Ensure that the resource
	 * @param resources
	 */
	private void assertProperVisibleItems() {
		IResource[] resources = set.getResources();
		List resourceList = new ArrayList();
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			resourceList.add(resource);
		}
		TestTreeViewer viewer = view.getViewer();
		Item[] items = viewer.getRootItems();
		if (resources.length ==  0) {
			assertTrue("There are items visible when there should not be.", items.length == 0);
			return;
		}
		// Test that all items in the tree are expected
		for (int i = 0; i < items.length; i++) {
			Item item = items[i];
			assertItemValid(item, resourceList);
		}
		// Test that all expected resources and their parents are present
		assertTrue("The tree did not contain all expected resources: " + resourceList.toString(), resourceList.isEmpty());
	}
	
	private void assertItemValid(Item item, List resources) {
		Object data = item.getData();
		if (data instanceof SyncInfoDiffNode) {
			IResource resource = ((SyncInfoDiffNode)data).getResource();
			if (resource.getType() == IResource.PROJECT) {
				assertProjectPresent((IProject)resource, resources);
			} else if (resource.getType() == IResource.FOLDER) {
				assertFolderPresent((IFolder)resource, resources);
			} else if (resource.getType() == IResource.FILE) {
				assertFilePresent(resource, resources);
			}
		}
		Item[] children = view.getViewer().getChildren(item);
		for (int i = 0; i < children.length; i++) {
			Item child = children[i];
			assertItemValid(child, resources);
		}
	}

	private void assertFolderPresent(IFolder folder, List resources) {
		// First, if the folder is out-of-sync, it should be visible
		for (Iterator iter = resources.iterator(); iter.hasNext();) {
			IResource resource = (IResource) iter.next();
			if (resource.equals(folder)) {
				// The folder should be present.
				// Remove it since it has been verified
				iter.remove();
				return;
			}
		}
		// If the folder contains a file in the list, it is also OK
		for (Iterator iter = resources.iterator(); iter.hasNext();) {
			IResource resource = (IResource) iter.next();
			if (resource.getType() == IResource.FILE && resource.getParent().equals(folder)) {
				// The compressed folder is valid since it contains an out-of-sync file
				// However, the resource is left since it has not been verified (only it's parent)
				return;
			}
		}
		fail("Folder " + folder.getFullPath() + " should not be visible but is.");
	}

	private void assertFilePresent(IResource itemResource, List resources) {
		for (Iterator iter = resources.iterator(); iter.hasNext();) {
			IResource resource = (IResource) iter.next();
			if (resource.equals(itemResource)) {
				// The resource has been verified so it can be removed
				iter.remove();
				return;
			}
		}
		fail("Resource " + itemResource.getFullPath() + " should not be visible but is.");
	}

	private void assertProjectPresent(IProject project, List resources) {
//		First, if the project is out-of-sync, it should be visible
		for (Iterator iter = resources.iterator(); iter.hasNext();) {
			IResource resource = (IResource) iter.next();
			if (resource.equals(project)) {
				// The folder should be present.
				// Remove it since it has been verified
				iter.remove();
				return;
			}
		}
		for (Iterator iter = resources.iterator(); iter.hasNext();) {
			IResource resource = (IResource) iter.next();
			if (resource.getProject().equals(project)) {
				return;
			}
		}
		fail("Project " + project.getName() + " should not be visible but is.");
	}

	public void testNestedCompressedFolder() throws CoreException {
		IProject project = createProject(new String[] { "file.txt", "folder1/file2.txt", "folder1/folder2/file3.txt"});
		adjustSet(
			set,
			project, 
			new String[] { "file.txt" },
			new int[] {SyncInfo.OUTGOING | SyncInfo.CHANGE});
		assertProperVisibleItems();
		
		adjustSet(
			set,
			project,
			new String[] { "folder1/file2.txt", "folder1/folder2/file3.txt" },
			new int[] {
				SyncInfo.OUTGOING | SyncInfo.CHANGE,
				SyncInfo.OUTGOING | SyncInfo.CHANGE});
		assertProperVisibleItems();
		
		adjustSet(
			set,
			project,
			new String[] { "folder1/file2.txt"},
			new int[] {
				SyncInfo.IN_SYNC,
			});
		assertProperVisibleItems();	
	}

	public void testParentRemovalWithChildRemaining() throws CoreException {
		IProject project = createProject(new String[] { "file.txt", "folder1/file2.txt", "folder1/folder2/file3.txt"});
		adjustSet(
			set,
			project, 
			new String[] { "folder1/folder2/", "folder1/folder2/file3.txt" },
			new int[] {
				SyncInfo.CONFLICTING | SyncInfo.CHANGE,
				SyncInfo.CONFLICTING | SyncInfo.CHANGE});
		assertProperVisibleItems();
		
		adjustSet(
			set,
			project,
			new String[] { "folder1/folder2/", "folder1/folder2/file3.txt" },
			new int[] {
				SyncInfo.IN_SYNC,
				SyncInfo.OUTGOING | SyncInfo.CHANGE});
		assertProperVisibleItems();
	}
}
