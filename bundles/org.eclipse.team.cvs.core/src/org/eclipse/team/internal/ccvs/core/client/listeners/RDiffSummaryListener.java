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
package org.eclipse.team.internal.ccvs.core.client.listeners;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.client.CommandOutputListener;

/**
 * This class parses the messages recieved in response to an "cvs rdiff -s ..." command
 */
public class RDiffSummaryListener extends CommandOutputListener {

	private IFileDiffListener listener;
	private static ServerMessageLineMatcher DIRECTORY_MATCHER;
	private static ServerMessageLineMatcher FILE_DIFF_MATCHER;
	private static ServerMessageLineMatcher NEW_FILE_MATCHER;
	private static ServerMessageLineMatcher DELETED_FILE_MATCHER;
	
	static {
		// TODO: temprary until proper lifecycle is defined
		initializePatterns();
	}
	public static void initializePatterns() {
		try {
			DIRECTORY_MATCHER = new ServerMessageLineMatcher(
				IMessagePatterns.RDIFF_DIRECTORY, new String[] {"remoteFolderPath"});
			FILE_DIFF_MATCHER = new ServerMessageLineMatcher(
				IMessagePatterns.RDIFF_SUMMARY_FILE_DIFF, new String[] {"remoteFilePath", "leftRevision", "rightRevision"});
			NEW_FILE_MATCHER = new ServerMessageLineMatcher(
				IMessagePatterns.RDIFF_SUMMARY_NEW_FILE, new String[] {"remoteFilePath", "rightRevision"});
			DELETED_FILE_MATCHER = new ServerMessageLineMatcher(
				IMessagePatterns.RDIFF_SUMMARY_DELETED_FILE, new String[] {"remoteFilePath"});
		} catch (CVSException e) {
			// This is serious as the listener will not function properly
			CVSProviderPlugin.log(e);
		}
	}
	
	public interface IFileDiffListener {
		public void fileDiff(
				String remoteFilePath,
				String leftRevision,
				String rightRevision);
		public void newFile(
				String remoteFilePath,
				String rightRevision);
		public void deletedFile(String remoteFilePath);
		public void directory(String remoteFolderPath);
	}
	
	public RDiffSummaryListener(IFileDiffListener listener) {
		this.listener = listener;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.client.listeners.ICommandOutputListener#messageLine(java.lang.String, org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation, org.eclipse.team.internal.ccvs.core.ICVSFolder, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus messageLine(
		String line,
		ICVSRepositoryLocation location,
		ICVSFolder commandRoot,
		IProgressMonitor monitor) {
		
		Map variables = FILE_DIFF_MATCHER.processServerMessage(line);
		if (variables != null) {
			listener.fileDiff((String)variables.get("remoteFilePath"), (String)variables.get("leftRevision"), (String)variables.get("rightRevision"));
			return OK;
		}
		
		variables = NEW_FILE_MATCHER.processServerMessage(line);
		if (variables != null) {
			listener.newFile((String)variables.get("remoteFilePath"), (String)variables.get("rightRevision"));
			return OK;
		}
		
		variables = DELETED_FILE_MATCHER.processServerMessage(line);
		if (variables != null) {
			listener.deletedFile((String)variables.get("remoteFilePath"));
			return OK;
		}
		
		return super.messageLine(line, location, commandRoot, monitor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.client.listeners.ICommandOutputListener#errorLine(java.lang.String, org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation, org.eclipse.team.internal.ccvs.core.ICVSFolder, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus errorLine(
		String line,
		ICVSRepositoryLocation location,
		ICVSFolder commandRoot,
		IProgressMonitor monitor) {
		
		Map variables = DIRECTORY_MATCHER.processServerMessage(line);
		if (variables != null) {
			listener.directory((String)variables.get("remoteFolderPath"));
			return OK;
		}
			
		return super.errorLine(line, location, commandRoot, monitor);
	}

}
