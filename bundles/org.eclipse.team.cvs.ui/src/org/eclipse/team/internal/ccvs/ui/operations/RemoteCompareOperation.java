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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareUI;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFile;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.RDiff;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.client.listeners.RDiffSummaryListener;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFile;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFolderTree;
import org.eclipse.team.internal.ccvs.ui.CVSCompareEditorInput;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.ResourceEditionNode;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.ui.IWorkbenchPage;

/**
 * Compare the two versions of given remote folders obtained from the two tags specified.
 */
public class RemoteCompareOperation extends RemoteOperation  implements RDiffSummaryListener.IFileDiffListener {

	private CVSTag left;
	private CVSTag right;
	
	private RemoteFolderTree leftTree, rightTree;

	public RemoteCompareOperation(Shell shell, ICVSRemoteFolder[] remoteFolders, CVSTag left, CVSTag right) {
		super(shell, remoteFolders);
		this.left = left;
		this.right = right;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.CVSOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void execute(IProgressMonitor monitor) throws CVSException {
		leftTree = rightTree = null;
		Command.QuietOption oldOption= CVSProviderPlugin.getPlugin().getQuietness();
		try {
			CVSProviderPlugin.getPlugin().setQuietness(Command.VERBOSE);
			ICVSRemoteResource[] resources = getRemoteResources();
			monitor.beginTask("Comparing remote modules", 100 * resources.length);
			for (int i = 0; i < resources.length; i++) {
				ICVSRemoteFolder folder = (ICVSRemoteFolder)resources[i];
				Session session = new Session(folder.getRepository(), folder, false);
				try {
					session.open(Policy.subMonitorFor(monitor, 10));
					collectStatus(buildTrees(session, folder, Policy.subMonitorFor(monitor, 90)));
				} finally {
					session.close();
				}
			}
			openCompareEditor(leftTree, rightTree);
		} finally {
			CVSProviderPlugin.getPlugin().setQuietness(oldOption);
			monitor.done();
		}
	}

	/*
	 * Build the two trees uses the reponses from "cvs rdiff -s ...".
	 */
	private IStatus buildTrees(Session session, ICVSRemoteFolder folder, IProgressMonitor monitor) throws CVSException {
		// Initialize the resulting trees
		if (leftTree == null) {
			leftTree = new RemoteFolderTree(null, folder.getRepository(), ICVSRemoteFolder.REPOSITORY_ROOT_FOLDER_NAME, left);
			rightTree = new RemoteFolderTree(null, folder.getRepository(), ICVSRemoteFolder.REPOSITORY_ROOT_FOLDER_NAME, right);
		}
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
		try {
			addFile(rightTree, right, new Path(remoteFilePath), rightRevision);
		} catch (CVSException e) {
			CVSUIPlugin.log(e);
		}
		try {
			addFile(leftTree, left, new Path(remoteFilePath), leftRevision);
		} catch (CVSException e) {
			CVSUIPlugin.log(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.client.listeners.RDiffSummaryListener.IFileDiffListener#newFile(java.lang.String, java.lang.String)
	 */
	public void newFile(String remoteFilePath, String rightRevision) {
		try {
			addFile(rightTree, right, new Path(remoteFilePath), rightRevision);
		} catch (CVSException e) {
			CVSUIPlugin.log(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.client.listeners.RDiffSummaryListener.IFileDiffListener#deletedFile(java.lang.String)
	 */
	public void deletedFile(String remoteFilePath) {
		try {
			addFile(leftTree, left, new Path(remoteFilePath), null);
		} catch (CVSException e) {
			CVSUIPlugin.log(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.client.listeners.RDiffSummaryListener.IFileDiffListener#directory(java.lang.String)
	 */
	public void directory(String remoteFolderPath) {
		try {
			getFolder(leftTree, left, new Path(remoteFolderPath), Path.EMPTY);
		} catch (CVSException e) {
			CVSUIPlugin.log(e);
		}
		try {
			getFolder(rightTree, right, new Path(remoteFolderPath), Path.EMPTY);
		} catch (CVSException e) {
			CVSUIPlugin.log(e);
		}
	}

	/* 
	 * Get the folder at the given path in the given tree, creating any missing folders as needed.
	 */
	private ICVSRemoteFolder getFolder(RemoteFolderTree tree, CVSTag tag, IPath remoteFolderPath, IPath parentPath) throws CVSException {
		if (remoteFolderPath.segmentCount() == 0) return tree;
		String name = remoteFolderPath.segment(0);
		ICVSResource child;
		IPath childPath = parentPath.append(name);
		if (tree.childExists(name)) {
			child = tree.getChild(name);
		}  else {
			child = new RemoteFolderTree(tree, tree.getRepository(), childPath.toString(), tag);
			addChild(tree, (ICVSRemoteResource)child);
		}
		return getFolder((RemoteFolderTree)child, tag, remoteFolderPath.removeFirstSegments(1), childPath);
	}

	private void addChild(RemoteFolderTree tree, ICVSRemoteResource resource) {
		ICVSRemoteResource[] children = tree.getChildren();
		ICVSRemoteResource[] newChildren;
		if (children == null) {
			newChildren = new ICVSRemoteResource[] { resource };
		} else {
			newChildren = new ICVSRemoteResource[children.length + 1];
			System.arraycopy(children, 0, newChildren, 0, children.length);
			newChildren[children.length] = resource;
		}
		tree.setChildren(newChildren);
	}

	private void addFile(RemoteFolderTree tree, CVSTag tag, Path filePath, String revision) throws CVSException {
		RemoteFolderTree parent = (RemoteFolderTree)getFolder(tree, tag, filePath.removeLastSegments(1), Path.EMPTY);
		String name = filePath.lastSegment();
		ICVSRemoteFile file = new RemoteFile(parent, 0, name, revision, null, tag);
		addChild(parent, file);
	}
	
	/*
	 * Only intended to be overridden by test cases.
	 */
	protected void openCompareEditor(final ICVSRemoteFolder leftTree, final ICVSRemoteFolder rightTree) {
		if (leftTree == null || rightTree == null) return;
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				CompareUI.openCompareEditorOnPage(
						new CVSCompareEditorInput(new ResourceEditionNode(leftTree), new ResourceEditionNode(rightTree)),
						getTargetPage());
			}
		});
	}
	
	protected IWorkbenchPage getTargetPage() {
		return TeamUIPlugin.getActivePage();
	}
}
