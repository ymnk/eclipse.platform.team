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

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.RDiff;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.client.listeners.RDiffSummaryListener;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.ui.Policy;

/**
 * Compare the two versions of given remote folders obtained from the two tags specified.
 */
public class RemoteCompareOperation extends RemoteOperation {

	private CVSTag left;
	private CVSTag right;

	/**
	 * @param shell
	 * @param remoteResources
	 */
	protected RemoteCompareOperation(Shell shell, ICVSRemoteFolder[] remoteFolders, CVSTag left, CVSTag right) {
		super(shell, remoteFolders);
		this.left = left;
		this.right = right;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.CVSOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void execute(IProgressMonitor monitor) throws CVSException {
			ICVSRemoteResource[] resources = getRemoteResources();
			ICVSFolder localRoot = CVSWorkspaceRoot.getCVSFolderFor(ResourcesPlugin.getWorkspace().getRoot());
			monitor.beginTask("Comparing remote modules", 100 * resources.length);
			for (int i = 0; i < resources.length; i++) {
				ICVSRemoteFolder folder = (ICVSRemoteFolder)resources[i];
				Session session = new Session(folder.getRepository(), localRoot, false);
				try {
					session.open(Policy.subMonitorFor(monitor, 10));
					collectStatus(buildDiffTree(session, folder, Policy.subMonitorFor(monitor, 90)));
				} finally {
					session.close();
				}
			}
			monitor.done();
	}

	/**
	 * @param folder
	 * @param monitor
	 * @return
	 */
	private IStatus buildDiffTree(Session session, ICVSRemoteFolder folder, IProgressMonitor monitor) throws CVSException {
		IStatus status = Command.RDIFF.execute(session,
				Command.NO_GLOBAL_OPTIONS,
				getLocalOptions(),
				new ICVSResource[] { folder },
				new RDiffSummaryListener(this),
				monitor);
		return status;
	}

	private LocalOption[] getLocalOptions() {
		return new LocalOption[] {RDiff.SUMMARY, RDiff.makeTagOption(left), RDiff.makeTagOption(right)};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.CVSOperation#getTaskName()
	 */
	protected String getTaskName() {
		return "Comparing";
	}

}
