/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 * IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.team.tests.ftp;

import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.internal.ftp.FTPException;
import org.eclipse.team.internal.ftp.client.FTPClient;
import org.eclipse.team.internal.ftp.client.FTPDirectoryEntry;
import org.eclipse.team.tests.core.TeamTest;

public class ClientTest extends TeamTest {

	private static final IProgressMonitor DEFAULT_PROGRESS_MONITOR = new NullProgressMonitor();
	/**
	 * Constructor for ClientTest.
	 * @param name
	 */
	public ClientTest(String name) {
		super(name);
	}
	public ClientTest() {
		super();
	}

	public static Test suite() {
		TestSuite suite = new TestSuite(ClientTest.class);
		return new FTPTestSetup(suite);
		//return new FTPTestSetup(new ClientTest("testName"));
	}
	
	public URL getURL() {
		return FTPTestSetup.ftpURL;
	}
	
	public FTPClient openFTPConnection() throws FTPException {
		return FTPTestSetup.openFTPConnection(getURL());
	}
	
	public void putContainer(IContainer container, FTPClient client) throws FTPException, CoreException {
		client.createDirectory(container.getName(), DEFAULT_PROGRESS_MONITOR);
		client.changeDirectory(container.getName(), DEFAULT_PROGRESS_MONITOR);
		IResource[] members = container.members();
		for (int i = 0; i < members.length; i++) {
			IResource resource = members[i];
			if (resource.getType() == IResource.FILE) {
				client.putFile(resource.getName(), (IFile)resource, false, DEFAULT_PROGRESS_MONITOR);
			} else {
				putContainer((IContainer)resource, client);
			}
		}
		client.changeDirectory(FTPClient.PARENT_DIRECTORY, DEFAULT_PROGRESS_MONITOR);
	}
	
	public void getAll(String sourceFolder, IContainer container, FTPClient client) throws FTPException, CoreException {
		if (sourceFolder.indexOf('/') == 0) {
			sourceFolder = sourceFolder.substring(1);
		}
		client.changeDirectory(sourceFolder, DEFAULT_PROGRESS_MONITOR);
		if ( ! container.exists()) {
			((IFolder)container).create(false, true, DEFAULT_PROGRESS_MONITOR);
		}
		FTPDirectoryEntry[] entries = client.listFiles(null, DEFAULT_PROGRESS_MONITOR);
		for (int i = 0; i < entries.length; i++) {
			FTPDirectoryEntry entry = entries[i];
			if (entry.hasFileSemantics()) {
				client.getFile(entry.getName(), container.getFile(new Path(entry.getName())), false, false, DEFAULT_PROGRESS_MONITOR);
			} else if (entry.hasDirectorySemantics()) {
				getAll(entry.getName(), container.getFolder(new Path(entry.getName())), client);
			}
		}
		// Assumes sourceFolder is a one segment path
		client.changeDirectory(FTPClient.PARENT_DIRECTORY, DEFAULT_PROGRESS_MONITOR);
	}
	
	public void testSimpleFileTransfer() throws FTPException, CoreException {
		String fileName = "file1.txt";
		IProject project = createUniqueTestProject("testSimpleFileTransferSource", new String[] {fileName});
		FTPClient client = openFTPConnection();
		try {
			client.createDirectory(project.getName(), DEFAULT_PROGRESS_MONITOR);
			client.changeDirectory(project.getName(), DEFAULT_PROGRESS_MONITOR);
			client.putFile(fileName, project.getFile(fileName), false, DEFAULT_PROGRESS_MONITOR);
			client.getFile(fileName, project.getFile("CopyOf" + fileName), false, false, DEFAULT_PROGRESS_MONITOR);
		} finally {
			client.close(DEFAULT_PROGRESS_MONITOR);
		}
		assertTrue(compareContent(project.getFile(fileName).getContents(), project.getFile("CopyOf" + fileName).getContents()));
	}

	public void testDeepFileTransfer() throws FTPException, CoreException {
		String deepFileName = "folder2/folder3/deep.txt";
		IProject project = createUniqueTestProject("testDeepFileTransfer", 
			new String[] {"file1.txt", "folder1/a.txt", "folder1/b.txt", deepFileName});
		FTPClient client = openFTPConnection();
		try {
			putContainer(project, client);
			client.changeDirectory(project.getName(), DEFAULT_PROGRESS_MONITOR);
			client.putFile(deepFileName, project.getFile(deepFileName), false, DEFAULT_PROGRESS_MONITOR);
			client.getFile(deepFileName, project.getFile("deepFile"), false, false, DEFAULT_PROGRESS_MONITOR);
		} finally {
			client.close(DEFAULT_PROGRESS_MONITOR);
		}
		assertTrue(compareContent(project.getFile(deepFileName).getContents(), project.getFile("deepFile").getContents()));
	}
		
	public void testProjectTransfer() throws FTPException, CoreException {
		IProject project = createUniqueTestProject("testProjectTransfer", 
			new String[] {"file1.txt", "folder1/a.txt", "folder1/b.txt", "folder2/folder3/"});
		IProject copy = getUniqueTestProject("testProjectTransferDest");
		FTPClient client = openFTPConnection();
		try {
			putContainer(project, client);
			getAll(project.getName(), copy, client);
		} finally {
			client.close(DEFAULT_PROGRESS_MONITOR);
		}
		assertEquals(project, copy);
	}
	
	public void testFolderTransfer() throws FTPException, CoreException {
		IProject project = createUniqueTestProject("testFolderTransfer", 
			new String[] {"file1.txt", "folder1/a.txt", "folder1/b.txt", "folder2/folder3/"});
		IProject copy = getUniqueTestProject("testProjectTransferDest");
		FTPClient client = openFTPConnection();
		try {
			putContainer(project, client);
			getAll(project.getFolder("folder1").getFullPath().toString(), copy.getFolder("folder1"), client);
		} finally {
			client.close(DEFAULT_PROGRESS_MONITOR);
		}
		assertEquals(project.getFolder("folder1"), copy.getFolder("folder1"));
	}
	
}
