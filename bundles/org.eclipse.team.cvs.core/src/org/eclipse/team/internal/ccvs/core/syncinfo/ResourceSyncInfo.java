package org.eclipse.team.internal.ccvs.core.syncinfo;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
 
import java.text.ParseException;
import java.util.Date;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.ccvs.core.CVSTag;
import org.eclipse.team.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.Policy;
import org.eclipse.team.internal.ccvs.core.resources.CVSEntryLineTag;
import org.eclipse.team.internal.ccvs.core.util.Assert;
import org.eclipse.team.internal.ccvs.core.util.CVSDateFormatter;
import org.eclipse.team.internal.ccvs.core.util.EmptyTokenizer;

/**
 * Value (immutable) object that represents workspace state information about a resource contained in
 * a CVS repository. It is a specialized representation of a line in the CVS/Entry file with the addition of 
 * file permissions.
 * 
 * Example entry line from the CVS/Entry file:
 * 
 * /new.java/1.2/Fri Dec 07 00:17:52 2001/-kb/
 * D/src////
 *  
 * @see ICVSResource#getSyncInfo()
 */
public class ResourceSyncInfo {
	
	// safe default permissions. Permissions are saved separatly so that the correct permissions
	// can be sent back to the server on systems that don't save execute bits (e.g. windows).
	public static final String DEFAULT_PERMISSIONS = "u=rw,g=rw,o=r"; //$NON-NLS-1$

	// use the server's default keyword substitution mode
	public static final String USE_SERVER_MODE = ""; //$NON-NLS-1$
	
	// file sync information can be associated with a local resource that has been deleted. This is
	// noted by prefixing the revision with this character.
	// XXX Should this be private
	public static final String DELETED_PREFIX = "-"; //$NON-NLS-1$
	
	// a sync element with a revision of '0' is considered a new file that has
	// not been comitted to the repo. Is visible so that clients can create sync infos
	// for new files.
	public static final String ADDED_REVISION = "0"; //$NON-NLS-1$
	
	// Timestamp constants used to identify special cases
	public static final int REGULAR_SYNC = 0;
	public static final int DUMMY_SYNC = 1;
	public static final int MERGED_SYNC = 2;
	public static final int MERGED_SYNC_WITH_CONFLICTS = 3;
	public static final int NULL_TIMESTAMP = 0;
	
	private static final String DUMMY_TIMESTAMP = "dummy timestamp"; //$NON-NLS-1$
	private static final String RESULT_OF_MERGE = "Result of merge"; //$NON-NLS-1$
	private static final String RESULT_OF_MERGE_CONFLICT = RESULT_OF_MERGE + "+"; //$NON-NLS-1$
	
	private static final String MERGE_MODIFIED = "+modified"; //$NON-NLS-1$
	private static final String MERGE_UNMODIFIED = "+="; //$NON-NLS-1$
	
	// a directory sync info will have nothing more than a name
	private boolean isDirectory = false;
	private boolean isDeleted = false;
	
	// utility constants
	private static final String DIRECTORY_PREFIX = "D/"; //$NON-NLS-1$
	private static final String SEPERATOR = "/"; //$NON-NLS-1$
	
	// fields describing the synchronization of a resource in CVS parlance
	private String name;
	private String revision;
	private long timeStamp;
	private String keywordMode;
	private CVSEntryLineTag tag;
	private String permissions;
	
	// type of sync
	private int syncType = REGULAR_SYNC;

	/**
	 * Constructor to create a sync object from entry line formats. The entry lines are parsed by this class.
	 * 
	 * @param entryLine the entry line (e.g.  /new.java/1.2/Fri Dec 07 00:17:52 2001/-kb/)
	 * @param permissions the file permission (e.g. u=rw,g=rw,o=r). May be <code>null</code>.
	 * @param timestamp if not included in the entry line. Will overide the value in the entry line. The
	 * timestamp should be in the format specified in ICVSFile#getTimestamp(). May be <code>null</code>.
	 * 
	 * @exception CVSException is thrown if the entry cannot be parsed.
	 */
	public ResourceSyncInfo(String entryLine, String permissions, long timestamp, int syncType) throws CVSException {
		Assert.isNotNull(entryLine);
		
		this.syncType = syncType;
		setEntryLine(entryLine);
		
		if (permissions != null)  {
			this.permissions = permissions;
		}
		// override the timestamp that may of been in entryLine. In some cases the timestamp is not in the
		// entry line (e.g. receiving entry lines from the server versus reading them from the Entry file).
		if(timestamp!=ICVSFile.NULL_TIMESTAMP) {
			this.timeStamp = timestamp;
		}
	}
	
	/**
	 * Constructor to create a sync object from entry line formats. The entry lines are parsed by this class.
	 * 
	 * @param entryLine the entry line (e.g.  /new.java/1.2/Fri Dec 07 00:17:52 2001/-kb/)
	 * @param permissions the file permission (e.g. u=rw,g=rw,o=r). May be <code>null</code>.
	 * @param timestamp if not included in the entry line. Will overide the value in the entry line. The
	 * timestamp should be in the format specified in ICVSFile#getTimestamp(). May be <code>null</code>.
	 * 
	 * @exception CVSException is thrown if the entry cannot be parsed.
	 */
	public ResourceSyncInfo(String entryLine, String permissions, long timestamp) throws CVSException {
		Assert.isNotNull(entryLine);
		
		setEntryLine(entryLine);
		
		if (permissions != null)  {
			this.permissions = permissions;
		}
		// override the timestamp that may of been in entryLine. In some cases the timestamp is not in the
		// entry line (e.g. receiving entry lines from the server versus reading them from the Entry file).
		if(timestamp!=ICVSFile.NULL_TIMESTAMP) {
			this.timeStamp = timestamp;
		}
	}
	
	/**
	 * Constructor to create a sync object from predefined values.
	 * 
	 * @param name of the resource for which this sync state is associated, cannot be <code>null</code>.
	 * @param revision of the resource, cannot be <code>null</code>.
	 * @param timestamp can be <code>null</code>.
	 * @param keywordMode can be <code>null</code>
	 * @param tag can be <code>null</code>
	 * @param permissions can be <code>null</code>
	 */
	public ResourceSyncInfo(String name, String revision, long timestamp, String keywordMode, CVSTag tag, String permissions, int syncType) {
		Assert.isNotNull(name);
		Assert.isNotNull(revision);		
		this.name = name;
		this.timeStamp = timestamp;		
		this.keywordMode = keywordMode;
		this.permissions = permissions;
		this.syncType = syncType;
		setRevision(revision);
		setTag(tag);
	}
	
	/**
	 * Constructor to create a folder sync object.
	 * 
	 * @param name of the resource for which this sync state is associatied, cannot be <code>null</code>.
	 */
	public ResourceSyncInfo(String name) {
		Assert.isNotNull(name);
		this.name = name;
		this.isDirectory = true;
	}

	/**
	 * Answers if this sync information is for a folder in which case only a name is
	 * available.
	 * 
	 * @return <code>true</code> if the sync information is for a folder and <code>false</code>
	 * if it is for a file.
	 */
	public boolean isDirectory() {
		return isDirectory;
	}
	
	/**
	 * Answers if this sync information is for a resource that has been merged by the cvs server with
	 * conflicts.
	 * 
	 * @return <code>true</code> if the sync information is for a file that has been merged and
	 * <code>false</code> for folders and for files that have not been merged.
	 */
	public boolean isNeedsMerge(long otherTimestamp) {
		return syncType == MERGED_SYNC_WITH_CONFLICTS && timeStamp == otherTimestamp;
	}
	
	/**
	 * Answers if this sync information is for a resource that has been merged by the cvs server with
	 * conflicts.
	 * 
	 * @return <code>true</code> if the sync information is for a file that has been merged and
	 * <code>false</code> for folders and for files that have not been merged.
	 */
	public boolean isMerged() {
		return syncType == MERGED_SYNC || syncType == MERGED_SYNC_WITH_CONFLICTS;
	}
		
	/**
	 * Answers if this sync information is for a file that has been added but not comitted
	 * to the CVS repository yet.
	 * 
	 * @return <code>true</code> if the sync information is new or <code>false</code> if 
	 * the sync is for an file that exists remotely. For folder sync info this returns
	 * <code>false</code>.
	 */
	public boolean isAdded() {
		if(!isDirectory) {
			return getRevision().equals(ADDED_REVISION);
		} else {
			return false;
		}
	}
	
	/**
	 * Answers if this sync information is for a file that is scheduled to be deleted
	 * from the repository but the deletion has not yet been comitted.
	 * 
	 * @return <code>true</code> if the sync information is deleted or <code>false</code> if 
	 * the sync is for an file that exists remotely.
	 */
	public boolean isDeleted() {
		return isDeleted;
	}
	
	/**
	 * Answers a CVS compatible entry line. The client can use this line to store in the CVS/Entry file or
	 * sent it to the server.
	 * 
	 * @param includeTimeStamp determines if the timestamp will be included in the returned entry line. In 
	 * some usages the timestamp should not be included in entry lines, for example when sending the entries 
	 * to the server.
	 * 
	 * @return a file or folder entry line reflecting the state of this sync object.
	 */
	public String getEntryLine(boolean includeTimeStamp) {
		return getEntryLine(includeTimeStamp, (String)null);
	}
	
	private String getEntryLine(boolean includeTimeStamp, String timestampOverride) {
		
		StringBuffer result = new StringBuffer();
		
		if(isDirectory) {
			result.append(DIRECTORY_PREFIX);
			result.append(name + "////"); //$NON-NLS-1$
		} else {
			result.append(SEPERATOR);
			result.append(name);
			result.append(SEPERATOR);
			
			if(isDeleted){
				result.append(DELETED_PREFIX); 
			}
				
			result.append(revision);
			result.append(SEPERATOR);

			if(includeTimeStamp) {
				String entryLineTimestamp = "";
				if(timestampOverride!=null) {
					entryLineTimestamp = timestampOverride;
				} else {					
					switch(syncType) {
						case REGULAR_SYNC:
							entryLineTimestamp = CVSDateFormatter.dateToEntryLine(new Date(timeStamp)); break;
						case MERGED_SYNC:
							entryLineTimestamp = RESULT_OF_MERGE; break;
						case MERGED_SYNC_WITH_CONFLICTS:
							entryLineTimestamp = RESULT_OF_MERGE_CONFLICT + CVSDateFormatter.dateToEntryLine(new Date(timeStamp)); break;
						case DUMMY_SYNC:
							entryLineTimestamp = DUMMY_TIMESTAMP; break;
					}						
				}
				result.append(entryLineTimestamp);
			}
			result.append(SEPERATOR);
			result.append(keywordMode == null ? "" : keywordMode); //$NON-NLS-1$
			result.append(SEPERATOR);
			if (tag != null) {
				result.append(tag.toEntryLineFormat(true));
			}
		}

		return result.toString();
	}
	
	/**
	 * Same as <code>getEntryLine</code> except it considers merged files in entry line format. This is only 
	 * valid for sending the file to the server.
	 * 
	 * @param includeTimeStamp determines if the timestamp will be included in the returned entry line. In 
	 * some usages the timestamp should not be included in entry lines, for example when sending the entries 
	 * to the server.
	 * @param isModified is the resource associated with this sync info modified.
	 * 
	 * @return a file or folder entry line reflecting the state of this sync object.
	 */
	public String getEntryLine(boolean includeTimeStamp, long fileTimestamp) {
		String serverTimestamp;
		if(isMerged()) {
			if(isNeedsMerge(fileTimestamp)) {
				serverTimestamp = MERGE_UNMODIFIED;
			} else {
				serverTimestamp = MERGE_MODIFIED;
			}
			return getEntryLine(true, serverTimestamp);
		} else {
			return getEntryLine(includeTimeStamp);
		}		
	}
	
	/**
	 * Anwsers a compatible permissions line for files.
	 * 
	 * @return a permission line for files and <code>null</code> if this sync object is
	 * a directory.
	 */
	public String getPermissionLine() {
		if(isDirectory) {
			return null;
		} else {
			String permissions = this.permissions;
			if (permissions == null)
				permissions = DEFAULT_PERMISSIONS;
			return SEPERATOR + name + SEPERATOR + permissions;
		}
	}
	
	/**
	 * Gets the permissions or <code>null</code> if permissions are not available.
	 * 
	 * @return a string of the format "u=rw,g=rw,o=r"
	 */
	public String getPermissions() {
		if(isDirectory) {
			return null;
		} else {
			if(permissions==null) {
				return DEFAULT_PERMISSIONS;
			} else {
				return permissions;
			}
		}
	}

	/**
	 * Gets the tag or <code>null</code> if a tag is not available.
	 * 
	 * @return Returns a String
	 */
	public CVSTag getTag() {
		return tag;
	}

	/**
	 * Gets the timeStamp or <code>null</code> if a timestamp is not available.
	 * 
	 * @return a string of the format "Thu Oct 18 20:21:13 2001"
	 */
	public long getTimeStamp() {
		return timeStamp;
	}

	/**
	 * Gets the version or <code>null</code> if this is a folder sync info. The returned
	 * revision will never include the DELETED_PREFIX. To found out if this sync info is
	 * for a deleted resource call isDeleted().
	 * 
	 * @return Returns a String
	 */
	public String getRevision() {
		return revision;
	}
	

	/**
	 * Gets the name.
	 * 
	 * @return Returns a String
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the keyword mode or <code>null</code> if a keyword mode is available.
	 * 
	 * @return 
	 */
	public String getKeywordMode() {
		return keywordMode;
	}
	
	/**
	 * Name equality between resource sync info objects.
	 */
	public boolean equals(Object other) {
		if(other instanceof ResourceSyncInfo) {
			ResourceSyncInfo syncInfo = ((ResourceSyncInfo)other);
			if(other == this) return true;
			if(getName() == syncInfo.getName()) return true;
			return getName().equals(syncInfo.getName());
		} else {
			return false;
		}
	}
	
	public int hashCode() {
		return getName().hashCode();
	}
	
	/**
	 * Sets the tag for the resource.
	 */
	private void setTag(CVSTag tag) {
		if(tag!=null) {
			this.tag = new CVSEntryLineTag(tag);
		} else {
			this.tag = null;
		}					
	}

	/**
	 * Set the entry line 
	 * 
	 * @throws CVSException if the entryLine is malformed
	 */
	private void setEntryLine(String entryLine) throws CVSException {
		if(entryLine.startsWith(DIRECTORY_PREFIX)) {
			isDirectory = true;
			entryLine = entryLine.substring(1);
		} else {
			isDirectory = false;
		}

		EmptyTokenizer tokenizer = new EmptyTokenizer(entryLine,SEPERATOR);

		if(tokenizer.countTokens() != 5) {
			throw new CVSException(Policy.bind("Malformed_entry_line___11") + entryLine); //$NON-NLS-1$
		}
		
		name = tokenizer.nextToken();
		
		if(name.length()==0) {
			throw new CVSException(Policy.bind("Malformed_entry_line,_missing_name___12") + entryLine); //$NON-NLS-1$
		}
		
		String rev = tokenizer.nextToken();
		
		if(rev.length()==0 && !isDirectory()) {
			throw new CVSException(Policy.bind("Malformed_entry_line,_missing_revision___13") + entryLine); //$NON-NLS-1$
		} else {
			setRevision(rev);
		}
	
		String date = tokenizer.nextToken();
		
		// possible timestamps are:
		// from server: "+=" and "+modified"
		// from entry line: "Result of Merge+Thu May 25 12:33:33 2002"
		//							 "Result of Merge"
		//							"Thu May 25 12:33:33 2002"
		//
		// The server will send a timestamp of "+=" if
		// the file was merged with conflicts. The '+' indicates that there are conflicts and the
		// '=' indicate that the timestamp for the file should be used. If the merge does not
		// have conflicts, simply add a text only timestamp and the file will be regarded as
		// having outgoing changes.
		// The purpose for having the two different timestamp options for merges is to 
		// dissallow commit of files that have conflicts until they have been manually edited.			
		if(date.indexOf(ResourceSyncInfo.MERGE_MODIFIED) != -1) {
			syncType = MERGED_SYNC;
			date = null;
		} else if(date.indexOf(ResourceSyncInfo.MERGE_UNMODIFIED) != -1) {
			syncType = MERGED_SYNC_WITH_CONFLICTS;
			date = null;
		} else if(date.indexOf(RESULT_OF_MERGE_CONFLICT)!=-1) {
			date = date.substring(date.indexOf("+") + 1); //$NON-NLS-1$
			syncType = MERGED_SYNC_WITH_CONFLICTS;
		} else if(date.indexOf(RESULT_OF_MERGE)!=-1) {
			syncType = MERGED_SYNC;
			date = null;
		}
		
		if(date==null || "".equals(date)) {
			timeStamp = NULL_TIMESTAMP;	
		} else {
			try {	
				timeStamp = CVSDateFormatter.entryLineToDate(date).getTime();
			} catch(ParseException e) {
				// something we don't understand, just make this sync have no timestamp and
				// never be in sync with the server.
				timeStamp = NULL_TIMESTAMP;
				syncType = DUMMY_SYNC;
			}
		}

		keywordMode = tokenizer.nextToken();
		String tagEntry = tokenizer.nextToken();
						
		if(tagEntry.length()>0) {
			tag = new CVSEntryLineTag(tagEntry);
		} else {
			tag = null;
		}
	}
	
	/**
	 * Sets the version and decides if the revision is for a deleted resource the revision field
	 * will not include the deleted prefix '-'.
	 * 
	 * @param version the version to set
	 */
	private void setRevision(String revision) {
		if(revision.startsWith(DELETED_PREFIX)) {
			this.revision = revision.substring(DELETED_PREFIX.length());
			isDeleted = true;
		} else {
			this.revision = revision;
			isDeleted = false;
		}
	}
	
	/**
	 * Returns the type of this sync info
	 */
	public int getType() {
		return syncType;
	}
	
	/*
	 * @see Object#toString()
	 */
	public String toString() {
		return getEntryLine(true);
	}
}