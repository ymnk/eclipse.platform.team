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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.RDiff;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.client.listeners.RDiffSummaryListener;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFolderTree;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.Policy;

/**
 * Compare the two versions of given remote folders obtained from the two tags specified.
 */
public class RemoteCompareOperation extends RemoteOperation  implements RDiffSummaryListener.IFileDiffListener {

	private CVSTag left;
	private CVSTag right;
	
	private RemoteFolderTree diffTree;

	public RemoteCompareOperation(Shell shell, ICVSRemoteFolder[] remoteFolders, CVSTag left, CVSTag right) {
		super(shell, remoteFolders);
		this.left = left;
		this.right = right;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.CVSOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void execute(IProgressMonitor monitor) throws CVSException {
			ICVSRemoteResource[] resources = getRemoteResources();
			monitor.beginTask("Comparing remote modules", 100 * resources.length);
			for (int i = 0; i < resources.length; i++) {
				ICVSRemoteFolder folder = (ICVSRemoteFolder)resources[i];
				Session session = new Session(folder.getRepository(), folder, false);
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

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.client.listeners.RDiffSummaryListener.IFileDiffListener#fileDiff(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void fileDiff(String remoteFilePath, String leftRevision, String rightRevision) {
		// TODO Auto-generated method stub
		System.out.println(remoteFilePath + leftRevision + rightRevision);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.client.listeners.RDiffSummaryListener.IFileDiffListener#newFile(java.lang.String, java.lang.String)
	 */
	public void newFile(String remoteFilePath, String rightRevision) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.client.listeners.RDiffSummaryListener.IFileDiffListener#deletedFile(java.lang.String)
	 */
	public void deletedFile(String remoteFilePath) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.client.listeners.RDiffSummaryListener.IFileDiffListener#directory(java.lang.String)
	 */
	public void directory(String remoteFolderPath) {
		try {
			ensureExists(diffTree, new Path(remoteFolderPath));
		} catch (CVSException e) {
			CVSUIPlugin.log(e);
		}
	}

	private void ensureExists(RemoteFolderTree tree, IPath remoteFolderPath) throws CVSException {
		String name = remoteFolderPath.segment(0);
		ICVSResource child;
		if (tree.childExists(name)) {
			child = tree.getChild(name);
		}  else {
			child = new RemoteFolderTree(tree, tree.getRepository(), )
		}
		
	}

}
