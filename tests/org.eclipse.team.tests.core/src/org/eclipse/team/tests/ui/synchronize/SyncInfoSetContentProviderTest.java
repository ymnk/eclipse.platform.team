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

import junit.framework.Test;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.MutableSyncInfoSet;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.ui.synchronize.views.CompressedFolder;
import org.eclipse.team.tests.core.TeamTest;
import org.eclipse.team.tests.ui.views.ContentProviderTestView;
import org.eclipse.team.tests.ui.views.TestTreeViewer;
import org.eclipse.team.ui.synchronize.SyncInfoDiffNode;

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
		TestTreeViewer viewer = view.getViewer();
		Item[] items = viewer.getRootItems();
		if (resources.length ==  0) {
			assertTrue("There are items visible when there should not be.", items.length == 0);
			return;
		}
		// Test that all items in the tree are expected
		for (int i = 0; i < items.length; i++) {
			Item item = items[i];
			assertItemValid(item, resources);
		}
		// Test that all expected resources and their parents are present
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			SyncInfoDiffNode node = new SyncInfoDiffNode(set, resource);
			assertTrue(
					"Item for " + resource.getFullPath() + " is missing", 
					viewer.hasItemFor(new SyncInfoDiffNode(set, resource)));
			if (resource.getType() == IResource.PROJECT) continue;
			IContainer parent = resource.getParent();
			if (parent.getType() == IResource.PROJECT) continue;
			if (set.getSyncInfo(parent) == null) {
				assertTrue(
						"Compressed parent for " + resource.getFullPath() + " is missing",
						viewer.hasItemFor(new CompressedFolder(set, parent)));
			}
		}
	}
	
	/**
	 * @param item
	 */
	private void assertItemValid(Item item, IResource[] resources) {
		Object data = item.getData();
		if (data instanceof SyncInfoDiffNode) {
			IResource resource = ((SyncInfoDiffNode)data).getResource();
			if (resource.getType() == IResource.PROJECT) {
				assertProjectPresent((IProject)resource, resources);
			} else if (resource.getType() == IResource.FOLDER) {
				if (data instanceof CompressedFolder) {
					assertParentOfResource((IFolder)resource, resources);
				} else {
					assertResourcePresent(resource, resources);
				}
			} else if (resource.getType() == IResource.FILE) {
				assertResourcePresent(resource, resources);
			}
		}
		Item[] children = view.getViewer().getChildren(item);
		for (int i = 0; i < children.length; i++) {
			Item child = children[i];
			assertItemValid(child, resources);
		}
	}

	private void assertParentOfResource(IFolder folder, IResource[] resources) {
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			if (resource.equals(folder)) {
				fail("Folder " + folder.getFullPath() + " is compressed but should not be.");
			}
		}
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			if (resource.getParent().equals(folder)) {
				return;
			}
		}
		fail("Folder " + folder.getFullPath() + " should not be visible but is.");
	}

	private void assertResourcePresent(IResource itemResource, IResource[] resources) {
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			if (resource.equals(itemResource)) {
				return;
			}
		}
		fail("Resource " + itemResource.getFullPath() + " should not be visible but is.");
	}

	private void assertProjectPresent(IProject project, IResource[] resources) {
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			if (resource.getProject().equals(project)) {
				return;
			}
		}
		fail("Project " + project.getName() + " should not be visible but is.");
	}

	public void testSimple() throws CoreException {
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

}
