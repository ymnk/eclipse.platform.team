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

/**
 * This class contains the default server message patterns
 */
public interface IMessagePatterns {

	public static final String SERVER_MESSAGE_PREFIX = "\\w* \\w*: "; //$NON-NLS-1$
	public static final String SERVER_ABORTED_MESSAGE_PREFIX = "\\w* [\\w* aborted]: "; //$NON-NLS-1$

	public static final String UPDATE_SECOND_PARTY_CREATION = "\\w* \\w*: conflict: (filePath:.*:filePath) created independently by second party";
	
	// TODO: It would be better if the prefix was optional but this requires the use of a capturing group which throws the group count off
	public static final String RDIFF_DIRECTORY = "\\w* \\w*: Diffing (remoteFolderPath:.*:remoteFolderPath)"; //$NON-NLS-1$
	public static final String RDIFF_SUMMARY_FILE_DIFF = "File (remoteFilePath:.*:remoteFilePath) changed from revision (leftRevision:.*:leftRevision) to (rightRevision:.*:rightRevision)"; //$NON-NLS-1$
	public static final String RDIFF_SUMMARY_NEW_FILE = "File (remoteFilePath:.*:remoteFilePath) is new; current revision (rightRevision:.*:rightRevision)"; //$NON-NLS-1$
	public static final String RDIFF_SUMMARY_DELETED_FILE = "File (remoteFilePath:.*:remoteFilePath) is removed; not included in release tag .*"; //$NON-NLS-1$
}
