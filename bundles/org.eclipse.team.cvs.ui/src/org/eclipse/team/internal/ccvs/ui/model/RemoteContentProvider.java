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
package org.eclipse.team.internal.ccvs.ui.model;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.progress.UIJob;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.RemoteResource;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.model.*;

/**
 * Extension to the generic workbench content provider mechanism
 * to lazily determine whether an element has children.  That is,
 * children for an element aren't fetched until the user clicks
 * on the tree expansion box.
 */
public class RemoteContentProvider extends WorkbenchContentProvider {

	IWorkingSet workingSet;
	Map cache = new HashMap();

	private class RemoteJob extends UIJob {

		Object[] newElements;
		Object parent;
		private boolean working;

		RemoteJob(Object parentElement) {
			super(viewer.getControl().getDisplay());
			parent = parentElement;
			addJobChangeListener(new JobChangeAdapter() {
				public void done(Job job, IStatus result) {
					setWorking(false);
				}
			});
		}
		synchronized boolean isWorking() {
			return working;
		}
		public IStatus runInUIThread(IProgressMonitor monitor) {
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			System.out.println("Running: " + this.toString());
			((AbstractTreeViewer) viewer).add(parent, newElements);
			return Status.OK_STATUS;
		}
		void runBatch(Object[] elements) {
			working = true;
			this.newElements = elements;
			schedule();
		}
		synchronized void setWorking(boolean working) {
			this.working = working;
		}
	}

	/* (non-Javadoc)
	 * Method declared on WorkbenchContentProvider.
	 */
	public boolean hasChildren(Object element) {
		if (element == null) {
			return false;
		}
		// the + box will always appear, but then disappear
		// if not needed after you first click on it.
		if (element instanceof ICVSRemoteResource) {
			if (element instanceof ICVSRemoteFolder) {
				return ((ICVSRemoteFolder) element).isExpandable();
			}
			return ((ICVSRemoteResource) element).isContainer();
		} else if (element instanceof CVSResourceElement) {
			ICVSResource r = ((CVSResourceElement) element).getCVSResource();
			if (r instanceof RemoteResource) {
				return r.isFolder();
			}
		} else if (element instanceof VersionCategory) {
			return true;
		} else if (element instanceof BranchCategory) {
			return true;
		} else if (element instanceof ModulesCategory) {
			return true;
		} else if (element instanceof CVSTagElement) {
			return true;
		} else if (element instanceof RemoteModule) {
			return true;
		}
		return super.hasChildren(element);
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parentElement) {

		IWorkbenchAdapter adapter = getAdapter(parentElement);
		if (adapter instanceof CVSModelElement) {
			CVSModelElement element = (CVSModelElement) adapter;
			if (element.isDeferred()) {
				List result = (List) cache.get(parentElement);
				if (result != null)
					return (Object[]) result.toArray(new Object[result.size()]);
				// otherwise, start the deferred fetch
				element.getChildrenDeferred(this, parentElement, workingSet);
				return new Object[] { new PendingUpdateAdapter()};
			} else {
				return ((CVSModelElement) adapter).getChildren(
					parentElement,
					workingSet);
			}
		}
		return super.getChildren(parentElement);
	}

	/**
	 * Sets the workingSet.
	 * @param workingSet The workingSet to set
	 */
	public void setWorkingSet(IWorkingSet workingSet) {
		this.workingSet = workingSet;
	}

	/**
	 * Returns the workingSet.
	 * @return IWorkingSet
	 */
	public IWorkingSet getWorkingSet() {
		return workingSet;
	}

	/**
	 * @param parent
	 * @param children
	 */
	protected void addChildren(final Object parent, final ICVSRemoteResource[] children, IProgressMonitor monitor) {

		List cachedChildren = (List) cache.get(parent);
		if (cachedChildren == null) {
			cachedChildren = new ArrayList();
			cache.put(parent, cachedChildren);
		}
		cachedChildren.addAll(Arrays.asList(children));

		if (viewer instanceof AbstractTreeViewer) {
			RemoteJob remoteJob = new RemoteJob(parent);
			int batchStart = 0;
			int batchEnd = 0;
			//process children until all children have been sent to the UI
			while (batchStart < children.length) {
				if (monitor.isCanceled()) {
					remoteJob.cancel();
					return;
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
				//only send a new batch when the last batch is finished
				if (!remoteJob.isWorking()) {
					int batchLength = batchEnd - batchStart + 1;
					Object[] batch = new Object[batchLength];
					System.arraycopy(children, batchStart, batch, 0, batchLength);
					remoteJob.runBatch(batch);
					batchStart = batchEnd + 1;
				}
				if (batchEnd < children.length)
					batchEnd++;
			}
		} else
			viewer.refresh();
	}

	public void clearCache() {
		cache.clear();
	}
}
