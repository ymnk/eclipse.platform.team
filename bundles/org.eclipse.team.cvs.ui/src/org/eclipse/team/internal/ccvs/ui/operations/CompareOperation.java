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
package org.eclipse.team.internal.ccvs.ui.operations;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.CVSTeamProvider;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Diff;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.client.Update;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.client.listeners.CompareDiffListener;

/**
 * Compare operations are performed using "cvs diff --brief"
 */
public class CompareOperation extends SingleCommandOperation implements CompareDiffListener.IFileDiffListener {

	public CompareOperation(Shell shell, IResource[] resources, CVSTag tag1, CVSTag tag2) {
		super(shell, resources, new LocalOption[] {
			Diff.BRIEF
		});
		if (tag1 != null) {
			addLocalOption(Update.makeTagOption(tag1));
		}
		if (tag2 != null) {
			addLocalOption(Update.makeTagOption(tag2));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.SingleCommandOperation#executeCommand(org.eclipse.team.internal.ccvs.core.client.Session, org.eclipse.team.internal.ccvs.core.CVSTeamProvider, org.eclipse.core.resources.IResource[], org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IStatus executeCommand(
		Session session,
		CVSTeamProvider provider,
		ICVSResource[] resources,
		IProgressMonitor monitor)
		throws CVSException, InterruptedException {
			
		IStatus status = Command.DIFF.execute(session,
				Command.NO_GLOBAL_OPTIONS,
				getLocalOptions(),
				resources,
				new CompareDiffListener(this),
				monitor);
		return status;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.CVSOperation#getTaskName()
	 */
	protected String getTaskName() {
		return "Comparing";
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.client.listeners.CompareDiffListener.IFileDiffListener#fileDiff(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	public void fileDiff(String localFilePath, String remoteFilePath, String leftRevision, String rightRevision) {
		System.out.println(localFilePath + " " + remoteFilePath + " " +leftRevision + " " +rightRevision);
	}

}
