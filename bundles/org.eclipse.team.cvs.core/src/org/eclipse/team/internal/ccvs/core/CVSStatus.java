/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core;
 
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
	
public class CVSStatus extends Status {

	/*** Status codes ***/
	public static final int SERVER_ERROR = -10;
	public static final int NO_SUCH_TAG = -11;
	public static final int CONFLICT = -12;
	public static final int ERROR_LINE = -14; // generic uninterpreted E line from the server
	public static final int TAG_ALREADY_EXISTS = -15;
	public static final int COMMITTING_SYNC_INFO_FAILED = -16;
	public static final int DOES_NOT_EXIST = -17;
	public static final int FOLDER_NEEDED_FOR_FILE_DELETIONS = -18;
	public static final int CASE_VARIANT_EXISTS = -19;
	public static final int UNSUPPORTED_SERVER_VERSION = -20;
	public static final int SERVER_IS_CVSNT = -21;
	public static final int SERVER_IS_UNKNOWN = -22;
	public static final int PROTOCOL_ERROR = -23;
	public static final int ERROR_LINE_PARSE_FAILURE = -24;
	public static final int FAILED_TO_CACHE_SYNC_INFO = -25;
	public static final int UNMEGERED_BINARY_CONFLICT = -26;
	public static final int INVALID_LOCAL_RESOURCE_PATH = -27;
	
	/*
	 * Status code that indicates that the status only contains a hyperlink
	 * and no other relevant status information. Status of this type
	 * can be ignored by most error handling code.
	 */
	public static final int HYPERLINK_DESCRIPTION_ONLY = -40;
	
	// Path for resource related status
	private ICVSFolder commandRoot;

	public CVSStatus(int severity, int code, String message, Throwable t) {
		super(severity, CVSProviderPlugin.ID, code, message, t);
	}
	
	public CVSStatus(int severity, int code, String message) {
		this(severity, code, message, null);
	}
	
	public CVSStatus(int severity, int code, ICVSFolder commandRoot, String message) {
		this(severity, code, message, null);
		this.commandRoot = commandRoot;
	}
	
	public CVSStatus(int severity, String message, Throwable t) {
		this(severity, 0, message, t);
	}
	
	public CVSStatus(int severity, String message) {
		this(severity, severity, message, null);
	}

	public String getMessage() {
		String message = super.getMessage();
		if (commandRoot != null) {
			message = Policy.bind("CVSStatus.messageWithRoot", commandRoot.getName(), message); //$NON-NLS-1$
		}
		return message;
	}

    /**
     * Create a status that provides access to a hyperlink descriptor
     * @param desc the hyperlink descriptor
     * @return a status that wraps the descriptor
     */
    public static IStatus createStatusFor(ICVSFolder commandRoot, String outputLine, CVSHyperlinkDescriptor[] descriptors) {
        return new CVSParseStatus(commandRoot, outputLine, descriptors);
    }

}
