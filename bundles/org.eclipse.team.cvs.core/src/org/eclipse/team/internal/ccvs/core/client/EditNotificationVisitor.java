/*******************************************************************************
 * Copyright (c) 2000, 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 * IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.team.internal.ccvs.core.client;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.syncinfo.NotifyInfo;

/**
 * Send the contents of the CVS/Notify files to the server
 */
public class EditNotificationVisitor extends AbstractStructureVisitor {

	public EditNotificationVisitor(Session session, IProgressMonitor monitor) {
		// Only send non-empty folders
		super(session, false, false, monitor);
	}
	
	/**
	 * @see org.eclipse.team.internal.ccvs.core.ICVSResourceVisitor#visitFile(ICVSFile)
	 */
	public void visitFile(ICVSFile file) throws CVSException {
		
		// we're only interested in files that have notification information
		NotifyInfo info = file.getNotifyInfo();
		if (info == null) return;
		
		// Send the parent folder if it hasn't been sent already
		sendFolder(file.getParent());

		// Send the notification to the server
		session.sendNotify(info, monitor);
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.core.ICVSResourceVisitor#visitFolder(ICVSFolder)
	 */
	public void visitFolder(ICVSFolder folder) throws CVSException {
		if (folder.isCVSFolder()) {
			folder.acceptChildren(this);
		}
	}

}
