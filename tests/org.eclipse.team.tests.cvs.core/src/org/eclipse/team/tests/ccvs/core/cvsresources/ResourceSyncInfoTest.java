package org.eclipse.team.tests.ccvs.core.cvsresources;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.team.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;
import org.eclipse.team.tests.ccvs.core.EclipseTest;

public class ResourceSyncInfoTest extends EclipseTest {

	private final static String GOOD_TIMESTAMP = "Mon Feb 25 21:44:02 2002";

	public ResourceSyncInfoTest() {
		super();
	}
	
	public ResourceSyncInfoTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		TestSuite suite = new TestSuite(ResourceSyncInfoTest.class);
		return new CVSTestSetup(suite);
	}
		
	/**
	 * Testing that the resource sync parses entry lines correctly.
	 */
	public void testEntryLineParsing() {
		String entryLine;
		
		// testing malformed entry lines first
		try {
			new ResourceSyncInfo("//////", null, 0, 0);			
			fail();
		} catch(CVSException e) {
		}
		try {
			new ResourceSyncInfo("//1.1///", null, 0, 0);			
			fail();
		} catch(CVSException e) {
		}
		try {
			new ResourceSyncInfo("/file.txt////", null, 0, 0);			
			fail();
		} catch(CVSException e) {
		}
		try {
			new ResourceSyncInfo("/file.txt//////////", null, 0, 0);			
			fail();
		} catch(CVSException e) {
		}
	}
	
	/**
	 * Testing that the entry line constructor
	 */
	public void testEntryLineConstructor() {		
		try {
			ResourceSyncInfo info;
			info = new ResourceSyncInfo("/file.java/-1.1/Mon Feb 25 21:44:02 2002/-k/", null, 0);
			assertTrue(info.isDeleted());
			
			info = new ResourceSyncInfo("/file.java/0/something/-k/", null, 0);
			assertTrue(info.isAdded());
			
			info = new ResourceSyncInfo("/file.java/1.0/Mon Feb 25 21:44:02 2002/-k/Tv1", null, 0);
			assertTrue(info.getTag() != null);
			
			long timestamp = 123456;
			info = new ResourceSyncInfo("/file.java/1.0/Mon Feb 25 21:44:02 2002/-k/Tv1", null, timestamp);
			assertTrue(info.getTimeStamp() == timestamp);
			
			info = new ResourceSyncInfo("/file.java/0/Mon Feb 25 21:44:02 2002/-k/", null, timestamp);
			assertTrue(info.getTimeStamp() == timestamp);
			
			String permissions = "u=rwx,g=rwx,o=rwx";
			info = new ResourceSyncInfo("/file.java/2.0/Mon Feb 25 21:44:02 2002/-k/Tv1", permissions, 0);
			assertTrue(info.getPermissions().equals(permissions));
			
			info = new ResourceSyncInfo("D/file.java////", null, 0);
			assertTrue(info.isDirectory());
			
		} catch(CVSException e) {
			fail("end");
		}
	}
	
	/**
	 * Testing the parameter constructor
	 */
	public void testConstructor() throws CVSException {
		ResourceSyncInfo info;
		
		info = new ResourceSyncInfo("folder");
		assertTrue(info.isDirectory());
		
		long timestamp = 123456;
		info = new ResourceSyncInfo("file.txt", "-2.34", timestamp, "", null, "", 0);
		assertTrue(info.isDeleted());
		assertTrue(info.getRevision().equals("2.34"));
		
		info = new ResourceSyncInfo("file.txt", "0", 0, "", null, "", 0);
		assertTrue(info.isAdded());
		
		info = new ResourceSyncInfo("file.txt", "0", 0, "", null, "", ResourceSyncInfo.DUMMY_SYNC);
		String entry = info.getEntryLine(true);
		info = new ResourceSyncInfo(entry, null, 0);
		assertTrue(info.getTimeStamp() == ResourceSyncInfo.NULL_TIMESTAMP);
		assertTrue(entry.indexOf("dummy") != -1);
		
		CVSTag tag = new CVSTag("v1", CVSTag.VERSION);
		info = new ResourceSyncInfo("file.txt", "1.1", timestamp, "", tag, "", 0);
		CVSTag newTag = info.getTag();
		assertTrue(newTag.getName().equals(tag.getName()) && newTag.getType() == tag.getType());
		assertTrue(info.getRevision().equals("1.1"));
	}
	
	public void testMergeTimestamps() throws CVSException {
		ResourceSyncInfo info, info2;
		long timestamp = 123000;
		long timestamp2 = 654000;
				
		info = new ResourceSyncInfo("/file.java/1.1//-kb/", null, timestamp);
		assertTrue(!info.isMerged());
		assertTrue(!info.isNeedsMerge(timestamp));		
		
		// entry lines the server can send
		info = new ResourceSyncInfo("/file.java/1.1/+=/-kb/", null, timestamp);
		String entryLine = info.getEntryLine(true);
		info2 = new ResourceSyncInfo(entryLine, null, 0);
		assertTrue(info.isMerged());
		assertTrue(info.isNeedsMerge(timestamp));
		assertTrue(!info.isNeedsMerge(timestamp2));
		assertTrue(info.getTimeStamp() == timestamp);		

		assertTrue(info2.isMerged());
		assertTrue(info2.isNeedsMerge(timestamp));
		assertTrue(!info2.isNeedsMerge(timestamp2));
		assertTrue(info2.getTimeStamp() == timestamp);		
		
		info = new ResourceSyncInfo("/file.java/1.1/+modified/-kb/", null, 0);
		entryLine = info.getEntryLine(true);
		info2 = new ResourceSyncInfo(entryLine, null, 0);
		
		assertTrue(info.isMerged());
		assertTrue(!info.isNeedsMerge(timestamp));
		assertTrue(!info.isNeedsMerge(timestamp2));
		assertTrue(info.getTimeStamp() == ResourceSyncInfo.NULL_TIMESTAMP);		
		
		assertTrue(info2.isMerged());
		assertTrue(!info2.isNeedsMerge(timestamp));
		assertTrue(!info2.isNeedsMerge(timestamp2));
		assertTrue(info2.getTimeStamp() == ResourceSyncInfo.NULL_TIMESTAMP);
	}
	
	public void testTimestampCompatibility() throws CVSException {		
	}
}