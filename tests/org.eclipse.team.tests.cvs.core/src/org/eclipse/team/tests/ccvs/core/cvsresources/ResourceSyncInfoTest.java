package org.eclipse.team.tests.ccvs.core.cvsresources;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Date;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;
import org.eclipse.team.tests.ccvs.core.EclipseTest;

public class ResourceSyncInfoTest extends EclipseTest {

	public ResourceSyncInfoTest() {
		super();
	}
	
	public ResourceSyncInfoTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		TestSuite suite = new TestSuite(ResourceSyncInfoTest.class);
		return new CVSTestSetup(suite, true /*don't need to connect for these tests*/);
	}
		
	public void testEntryLineParsing() {
		String entryLine;
		
		// testing malformed entry lines first
		try {
			new ResourceSyncInfo("//////", null, null);			
			fail();
		} catch(CVSException e) {
		}
		try {
			new ResourceSyncInfo("//1.1///", null, null);			
			fail();
		} catch(CVSException e) {
		}
		try {
			new ResourceSyncInfo("/file.txt////", null, null);			
			fail();
		} catch(CVSException e) {
		}
		try {
			new ResourceSyncInfo("/file.txt//////////", null, null);			
			fail();
		} catch(CVSException e) {
		}
	}
	
	public void testEntryLineConstructor() throws CVSException {		
		ResourceSyncInfo info;
		info = new ResourceSyncInfo("/file.java/-1.1/Mon Feb 25 21:44:02 2002/-k/", null, null);
		assertTrue(info.isDeleted());
		
		info = new ResourceSyncInfo("/file.java/0/something/-k/", null, null);
		assertTrue(info.isAdded());
		
		info = new ResourceSyncInfo("/file.java/1.0/Mon Feb 25 21:44:02 2002/-k/Tv1", null, null);
		assertTrue(info.getTag() != null);
		
		Date timestamp = new Date(123000);
		info = new ResourceSyncInfo("/file.java/1.0/Mon Feb 25 21:44:02 2002/-k/Tv1", null, timestamp);
		assertTrue(info.getTimeStamp().equals(timestamp));
		
		info = new ResourceSyncInfo("/file.java/0/Mon Feb 25 21:44:02 2002/-k/", null, timestamp);
		assertTrue(info.getTimeStamp().equals(timestamp));
		
		String permissions = "u=rwx,g=rwx,o=rwx";
		info = new ResourceSyncInfo("/file.java/2.0/Mon Feb 25 21:44:02 2002/-k/Tv1", permissions, null);
		assertTrue(info.getPermissions().equals(permissions));
		
		info = new ResourceSyncInfo("D/file.java////", null, null);
		assertTrue(info.isDirectory());
	}
	
	public void testConstructor() throws CVSException {
		ResourceSyncInfo info;
		
		info = new ResourceSyncInfo("folder");
		assertTrue(info.isDirectory());
		
		Date timestamp = new Date(123000);
		info = new ResourceSyncInfo("file.txt", "-2.34", timestamp, "", null, "");
		assertTrue(info.isDeleted());
		assertTrue(info.getRevision().equals("2.34"));
		
		info = new ResourceSyncInfo("file.txt", "0", ResourceSyncInfo.DUMMY_DATE, "", null, "");
		String entry = info.getEntryLine(true);
		assertTrue(info.isAdded());
		assertTrue(entry.indexOf("dummy") != -1);
		
		CVSTag tag = new CVSTag("v1", CVSTag.VERSION);
		info = new ResourceSyncInfo("file.txt", "1.1", timestamp, "", tag, "");
		CVSTag newTag = info.getTag();
		assertTrue(newTag.getName().equals(tag.getName()));
		assertTrue(info.getRevision().equals("1.1"));
	}
	
	public void testMergeTimestamps() throws CVSException {
		ResourceSyncInfo info, info2;
		Date timestamp = new Date(123000);
		Date timestamp2 = new Date(654000);
				
		info = new ResourceSyncInfo("/file.java/1.1//-kb/", null, timestamp);
		assertTrue(!info.isMerged());
		assertTrue(!info.isNeedsMerge(timestamp));		
		
		// test merged entry lines the server and ensure that their entry line format is compatible
		info = new ResourceSyncInfo("/file.java/1.1/+=/-kb/", null, timestamp);
		String entryLine = info.getEntryLine(true);
		info2 = new ResourceSyncInfo(entryLine, null, null);
		assertTrue(info.isMerged() && info2.isMerged());
		assertTrue(info.isNeedsMerge(timestamp) && info2.isNeedsMerge(timestamp));
		assertTrue(!info.isNeedsMerge(timestamp2) && !info2.isNeedsMerge(timestamp2));
		assertTrue(info.getTimeStamp().equals(timestamp) && info2.getTimeStamp().equals(timestamp));		

		info = new ResourceSyncInfo("/file.java/1.1/+modified/-kb/", null, null);
		entryLine = info.getEntryLine(true);
		info2 = new ResourceSyncInfo(entryLine, null, null);	
		assertTrue(info.isMerged() && info2.isMerged());
		assertTrue(!info.isNeedsMerge(timestamp) && !info2.isNeedsMerge(timestamp));
		assertTrue(!info.isNeedsMerge(timestamp2) && !info2.isNeedsMerge(timestamp2));
		assertTrue(info.getTimeStamp()==null && info2.getTimeStamp()==null);
	}
	
	public void testTimestampCompatibility() throws CVSException, CoreException {
		String entryLine1 = "/a.bin/1.1/Mon Feb  9 21:44:02 2002/-kb/";
		String entryLine2 = "/a.bin/1.1/Mon Feb 9 21:44:02 2002/-kb/";
		String entryLine3 = "/a.bin/1.1/Mon Feb 09 21:44:02 2002/-kb/";		
		ResourceSyncInfo info1 = new ResourceSyncInfo(entryLine1, null, null);
		ResourceSyncInfo info2 = new ResourceSyncInfo(entryLine2, null, null);
		ResourceSyncInfo info3 = new ResourceSyncInfo(entryLine3, null, null);
		Date date1 = info1.getTimeStamp();
		Date date2 = info2.getTimeStamp();
		Date date3 = info3.getTimeStamp();
		assertTrue(date1.equals(date2));
		assertTrue(date1.equals(date3));
		assertTrue(date2.equals(date3));
		
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("testTimeStamp");
		project.create(null); project.open(null);
		IFile file = project.getFile("test.txt");
		file.create(new ByteArrayInputStream("one".getBytes()), true, null);
		File ioFile = file.getLocation().toFile();
		ioFile.setLastModified(info1.getTimeStamp().getTime());
		Date ioFileDate = new Date(ioFile.lastModified());
		assertTrue(ioFileDate.equals(info1.getTimeStamp()));
		
		ioFile.setLastModified(info2.getTimeStamp().getTime());
		ioFileDate = new Date(ioFile.lastModified());
		assertTrue(ioFileDate.equals(info2.getTimeStamp()));
		
		String entryLine4 = "/a.bin/1.1/Tue Mar 12 20:59:01 2002/-kb/";		
		ResourceSyncInfo info4 = new ResourceSyncInfo(entryLine4, null, null);
		
		ioFile.setLastModified(info4.getTimeStamp().getTime());
		ioFileDate = new Date(ioFile.lastModified());
		//assertTrue(ioFileDate.equals(info4.getTimeStamp()));
		
		for(int i=0 ; i < 1000; i++) {			
			Date date = new Date(info4.getTimeStamp().getTime() + (1000*i));
			ioFile.setLastModified(date.getTime());
			if(ioFile.lastModified() != date.getTime()) {
				System.out.println("Different date: " + date + " file: " + new Date(ioFile.lastModified()));
			}
		}
		
	}
}