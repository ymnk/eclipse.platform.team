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
package org.eclipse.team.internal.ccvs.ui.subscriber;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFile;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.ui.synchronize.SyncInfoDiffNode;
import org.eclipse.team.ui.synchronize.SyncInfoDiffNodeRoot;
import org.eclipse.team.ui.synchronize.views.SyncInfoDiffNodeBuilder;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.progress.UIJob;

/**
 * It would be very useful to support showing changes grouped logically
 * instead of grouped physically. This could be used for showing incoming
 * changes and also for showing the results of comparisons.
 * 
 * Some problems with this:
 * - how to support logical groupins based on any of the information in
 * the log entry?
 */
public class ChangeLogDiffNodeBuilder extends SyncInfoDiffNodeBuilder {
	
	private Map commentRoots = new HashMap();
	private PendingUpdateAdapter pendingItem;
	private boolean shutdown = false;
	private FetchLogEntriesJob fetchLogEntriesJob;
	
	/**
	 * The PendingUpdateAdapter is a convenience object that can be used
	 * by a BaseWorkbenchContentProvider that wants to show a pending update.
	 */
	public static class PendingUpdateAdapter implements IWorkbenchAdapter, IAdaptable {

		/**
		 * Create a new instance of the receiver.
		 */
		public PendingUpdateAdapter() {
			//No initial behavior
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
		 */
		public Object getAdapter(Class adapter) {
			if (adapter == IWorkbenchAdapter.class)
				return this;
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.model.IWorkbenchAdapter#getChildren(java.lang.Object)
		 */
		public Object[] getChildren(Object o) {
			return new Object[0];
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.model.IWorkbenchAdapter#getImageDescriptor(java.lang.Object)
		 */
		public ImageDescriptor getImageDescriptor(Object object) {
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
		 */
		public String getLabel(Object o) {
			return "Fetching logs from server. Please wait...";
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
		 */
		public Object getParent(Object o) {
			return null;
		}
	}
	
	private class FetchLogEntriesJob extends Job {
		private SyncInfoSet set;
		public FetchLogEntriesJob() {
			super("Fetching CVS logs");  //$NON-NLS-1$;
		}
		public void setSyncInfoSet(SyncInfoSet set) {
			this.set = set;
		}
		public IStatus run(IProgressMonitor monitor) {
			if (set != null && !shutdown) {
				final SyncInfoDiffNode[] nodes = calculateRoots(getRoot().getSyncInfoSet(), monitor);				
				UIJob updateUI = new UIJob("updating change log viewers") {
					public IStatus runInUIThread(IProgressMonitor monitor) {
						AbstractTreeViewer tree = getTreeViewer();	
						if(pendingItem != null && tree != null && !tree.getControl().isDisposed()) {									
							tree.remove(pendingItem);
						}
						for (int i = 0; i < nodes.length; i++) {
							addToViewer(nodes[i]);
							buildTree(nodes[i]);				
						}
						return Status.OK_STATUS;
					}
				};
				updateUI.setSystem(true);
				updateUI.schedule();				
			}
			return Status.OK_STATUS;
		}
	};
	
	public ChangeLogDiffNodeBuilder(SyncInfoDiffNodeRoot root) {
		super(root);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.views.SyncInfoDiffNodeBuilder#buildTree(org.eclipse.compare.structuremergeviewer.DiffNode)
	 */
	protected IDiffElement[] buildTree(DiffNode node) {
		if(node == getRoot()) {
			UIJob job = new UIJob("") {
				public IStatus runInUIThread(IProgressMonitor monitor) {
					AbstractTreeViewer tree = getTreeViewer();			
					if (tree != null) {
						if(pendingItem == null) {
							pendingItem = new PendingUpdateAdapter();
						}
						removeAllFromTree();
						tree.add(getRoot(), pendingItem);
					}
					return Status.OK_STATUS;
				}
			};
			job.schedule();
			
			if(fetchLogEntriesJob == null) {
				fetchLogEntriesJob = new FetchLogEntriesJob();
			}
			if(fetchLogEntriesJob.getState() != Job.NONE) {
				fetchLogEntriesJob.cancel();
				try {
					fetchLogEntriesJob.join();
				} catch (InterruptedException e) {
				}
			}
			fetchLogEntriesJob.setSyncInfoSet(getRoot().getSyncInfoSet());
			fetchLogEntriesJob.schedule();						
		} else {
			return super.buildTree(node);
		}
		return new IDiffElement[0];
	}

	private SyncInfoDiffNode[] calculateRoots(SyncInfoSet set, IProgressMonitor monitor) {
		commentRoots.clear();
		SyncInfo[] infos = set.members();
		monitor.beginTask("fetching from server", set.size() * 100);
		for (int i = 0; i < infos.length; i++) {
			if(monitor.isCanceled()) {
				break;
			}
			ILogEntry logEntry = getSyncInfoComment((CVSSyncInfo) infos[i], monitor);
			if(logEntry != null) {
				String comment = logEntry.getComment();
				ChangeLogDiffNode changeRoot = (ChangeLogDiffNode) commentRoots.get(comment);
				if (changeRoot == null) {
					changeRoot = new ChangeLogDiffNode(getRoot(), logEntry);
					commentRoots.put(comment, changeRoot);
				}
				changeRoot.add(infos[i]);
			}
			monitor.worked(100);
		}		
		return (ChangeLogDiffNode[]) commentRoots.values().toArray(new ChangeLogDiffNode[commentRoots.size()]);
	}
	
	/**
	 * How do we tell which revision has the interesting log message? Use the later
	 * revision, since it probably has the most up-to-date comment.
	 */
	private ILogEntry getSyncInfoComment(CVSSyncInfo info, IProgressMonitor monitor) {
		try {
			if(info.getLocal().getType() != IResource.FILE) {
				return null;
			}
			
			ICVSRemoteResource remote = (ICVSRemoteResource)info.getRemote();
			ICVSRemoteResource base = (ICVSRemoteResource)info.getBase();
			ICVSRemoteResource local = (ICVSRemoteFile)CVSWorkspaceRoot.getRemoteResourceFor(info.getLocal());
			
			String baseRevision = getRevisionString(base);
			String remoteRevision = getRevisionString(remote);
			String localRevision = getRevisionString(local);
				
			boolean useRemote = ResourceSyncInfo.isLaterRevision(remoteRevision, localRevision);
			if (useRemote) {
				return ((RemoteFile) remote).getLogEntry(monitor);
			} else {
				return ((RemoteFile) local).getLogEntry(monitor);
			}
		} catch (CVSException e) {
			CVSUIPlugin.log(e);
			return null;
		}
	}
	
	private String getRevisionString(ICVSRemoteResource remoteFile) {
		if(remoteFile instanceof RemoteFile) {
			return ((RemoteFile)remoteFile).getRevision();
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.views.SyncInfoDiffNodeBuilder#syncSetChanged(org.eclipse.team.core.subscribers.ISyncInfoSetChangeEvent)
	 */
	protected void syncSetChanged(ISyncInfoSetChangeEvent event) {
		reset();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.views.SyncInfoDiffNodeBuilder#dispose()
	 */
	public void dispose() {
		shutdown = true;
		if(fetchLogEntriesJob != null && fetchLogEntriesJob.getState() != Job.NONE) {
			fetchLogEntriesJob.cancel();
		}
		super.dispose();
	}
}
