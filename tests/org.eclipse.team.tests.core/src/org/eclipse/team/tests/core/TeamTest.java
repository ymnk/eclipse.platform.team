package org.eclipse.team.tests.core;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.tests.harness.EclipseWorkspaceTest;

public class TeamTest extends EclipseWorkspaceTest {
	
	protected static final int RANDOM_CONTENT_SIZE = 3876;
	protected static final String PLATFORM_NEWLINE = System.getProperty("line.separator");
	
	public TeamTest() {
		super();
	}
	public TeamTest(String name) {
		super(name);
	}

	// Assert that the two containers have equal contents
	protected void assertEquals(IContainer container1, IContainer container2) throws CoreException {
		assertEquals(container1.getName(), container2.getName());
		List members1 = new ArrayList();
		members1.addAll(Arrays.asList(container1.members()));
		
		List members2 = new ArrayList();
		members2.addAll(Arrays.asList(container2.members()));
		
		assertTrue(members1.size() == members2.size());
		for (int i=0;i<members1.size();i++) {
			IResource member1 = (IResource)members1.get(i);
			IResource member2 = container2.findMember(member1.getName());
			assertNotNull(member2);
			assertEquals(member1, member2);
		}
	}
	
	// Assert that the two files have equal contents
	protected void assertEquals(IFile file1, IFile file2) throws CoreException {
		assertEquals(file1.getName(), file2.getName());
		assertTrue(compareContent(file1.getContents(), file2.getContents()));
	}
	
	// Assert that the two projects have equal contents ignoreing the project name
	// and the .vcm_meta file
	protected void assertEquals(IProject container1, IProject container2) throws CoreException {
		List members1 = new ArrayList();
		members1.addAll(Arrays.asList(container1.members()));
		members1.remove(container1.findMember(".project"));
		
		List members2 = new ArrayList();
		members2.addAll(Arrays.asList(container2.members()));
		members2.remove(container2.findMember(".project"));
		
		assertTrue("Number of children differs for " + container1.getFullPath(), members1.size() == members2.size());
		for (int i=0;i<members1.size();i++) {
			IResource member1 = (IResource)members1.get(i);
			IResource member2 = container2.findMember(member1.getName());
			assertNotNull(member2);
			assertEquals(member1, member2);
		}
	}
	protected void assertEquals(IResource resource1, IResource resource2) throws CoreException {
		assertEquals(resource1.getType(), resource2.getType());
		if (resource1.getType() == IResource.FILE)
			assertEquals((IFile)resource1, (IFile)resource2);
		else 
			assertEquals((IContainer)resource1, (IContainer)resource2);
	}
	
	protected IProject getNamedTestProject(String name) throws CoreException {
		IProject target = getWorkspace().getRoot().getProject(name);
		if (!target.exists()) {
			target.create(null);
			target.open(null);		
		}
		assertExistsInFileSystem(target);
		return target;
	}
	
	protected IProject getUniqueTestProject(String prefix) throws CoreException {
		// manage and share with the default stream create by this class
		return getNamedTestProject(prefix + "-" + Long.toString(System.currentTimeMillis()));
	}
	
	protected IStatus getTeamTestStatus(int severity) {
		return new Status(severity, "org.eclipse.team.tests.core", 0, "team status", null);
	}
	
	/**
	 * Return the average size of randomly generated contents for files that are created
	 * by the buildResources(IContainer, String[], boolean) methd.
	 * Subclasses can override to change the behavior
	 */
	protected int getAverageRandomContentSize() {
		return RANDOM_CONTENT_SIZE;
	}
	
	/**
	 * genertates Random content meand to be written in a File
	 */
	protected static InputStream getRandomContents(int averageSize) {
		
		StringBuffer content = new StringBuffer();
		int contentSize;
		
		content.append("Random file generated for test" + PLATFORM_NEWLINE);
		
		contentSize = (int) Math.round(averageSize * 2 * Math.random());
		for (int i=0; i < contentSize; i++) {
			
			if (Math.random() > 0.9) {
				content.append(PLATFORM_NEWLINE);
			}
			
			content.append((char)('\u0021' + Math.round(60 * Math.random())));
		}
		
		return new ByteArrayInputStream(content.toString().getBytes());
	}
	
	/**
	 * Return a collection of resources defined by hierarchy. The resources
	 * are added to the workspace and to the file system.
	 */
	protected IResource[] buildResources(IContainer container, String[] hierarchy, boolean includeContainer) throws CoreException {
		List resources = new ArrayList(hierarchy.length + 1);
		resources.addAll(Arrays.asList(buildResources(container, hierarchy)));
		if (includeContainer)
			resources.add(container);
		IResource[] result = (IResource[]) resources.toArray(new IResource[resources.size()]);
		ensureExistsInWorkspace(result, true);
		for (int i = 0; i < result.length; i++) {
			if (result[i].getType() == IResource.FILE)
				// 3786 bytes is the average size of Eclipse Java files!
				 ((IFile) result[i]).setContents(getRandomContents(getAverageRandomContentSize()), true, false, null);
		}
		return result;
	}
	
	protected IProject createUniqueTestProject(String prefix, String[] hierarchy) throws CoreException {
		IProject project = getUniqueTestProject(prefix);
		buildResources(project, hierarchy, true);
		return project;
	}
}
