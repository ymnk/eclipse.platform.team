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
package org.eclipse.team.internal.ccvs.core.client;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.CVSStatus;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.client.listeners.ICommandOutputListener;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;

public class CommandOutputListener implements ICommandOutputListener {
	public IStatus messageLine(String line, ICVSRepositoryLocation location, ICVSFolder commandRoot, IProgressMonitor monitor) {
		return OK;
	}
	public IStatus errorLine(String line, ICVSRepositoryLocation location, ICVSFolder commandRoot, IProgressMonitor monitor) {
		String protocolError = getProtocolError(line, location);
		if (protocolError != null) {
			return new CVSStatus(CVSStatus.ERROR, CVSStatus.PROTOCOL_ERROR, commandRoot, protocolError);
		}
		return new CVSStatus(CVSStatus.ERROR, CVSStatus.ERROR_LINE, commandRoot, line);
	}
	
	/**
	 * Return the portion of the line that describes the error if the error line
	 * is a protocol error or null if the line is not a protocol error.
	 * 
	 * @param line the error line received from the server
	 * @param location the repository location
	 * @return String the potocol error or null
	 */
	protected String getProtocolError(String line, ICVSRepositoryLocation location) {
		if (line.startsWith("Protocol error:")) { //$NON-NLS-1$
			return line;
		}
		return null;
	}

	public String getServerMessage(String line, ICVSRepositoryLocation location) {
		return ((CVSRepositoryLocation)location).getServerMessageWithoutPrefix(line, SERVER_PREFIX);
	}

	public String getServerAbortedMessage(String line, ICVSRepositoryLocation location) {
		return ((CVSRepositoryLocation)location).getServerMessageWithoutPrefix(line, SERVER_ABORTED_PREFIX);
	}

	public String getServerRTagMessage(String line, ICVSRepositoryLocation location) {
		return ((CVSRepositoryLocation)location).getServerMessageWithoutPrefix(line, RTAG_PREFIX);
	}
	
	/**
	 * Helper method for creating a status that contains a descriptor that can
	 * be used to create hyperlink in the message line.
     * @param commandRoot the local folder used as the root of the CVS command
     * @param line the message or error line recieved from the server
     * @param path the path of the resource relative to the command root
     * @return a status that contains the link descriptor
     */
    protected IStatus getHyperlinkDescriptorStatus(ICVSFolder commandRoot, String line, String path) {
        CVSHyperlinkDescriptor desc = new CVSHyperlinkDescriptor(commandRoot, line, CVSHyperlinkDescriptor.LOCAL_RESOURCE_PATH, path);
        return CVSStatus.createStatusFor(commandRoot, line, new CVSHyperlinkDescriptor[] { desc });
    }
}
