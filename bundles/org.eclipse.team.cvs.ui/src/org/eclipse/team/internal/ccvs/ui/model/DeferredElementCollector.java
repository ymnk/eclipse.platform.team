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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.progress.IElementCollector;
import org.eclipse.jface.progress.UIJob;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.team.internal.ccvs.core.Policy;

abstract public class DeferredElementCollector implements IElementCollector {
	
	private int BATCH_SIZE = 5;
	private Viewer viewer;
	private boolean DEBUG = true;
	private long FAKE_LATENCY = 100; // milliseconds
	boolean hasAddedChildren = false;
	
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
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			
			if(DEBUG) {
				System.out.print("DeferredCollector adding to viewer [" + newElements.length + "]: ");
				for (int i = 0; i < newElements.length; i++) {
					System.out.print(newElements[i].toString() + ",");
				}	
				System.out.print("\n");
			}
			hasAddedChildren = true;
			if(viewer instanceof AbstractTreeViewer) {
				((AbstractTreeViewer) viewer).add(parent, newElements);
			}
			return Status.OK_STATUS;
		}
		void runBatch(Object[] elements) {
			if(elements.length == 0) {
				if(viewer instanceof AbstractTreeViewer) {
					//((AbstractTreeViewer) viewer).
				}
			}
			working = true;
			this.newElements = elements;
			schedule();
		}
		synchronized void setWorking(boolean working) {
			this.working = working;
		}
	}

	public DeferredElementCollector(Viewer viewer) {
		this.viewer = viewer;
	}
	
	public void addChildren(final Object parent, final Object[] children, IProgressMonitor monitor) {
		monitor = Policy.monitorFor(monitor);
		if (viewer instanceof AbstractTreeViewer) {
			RemoteJob remoteJob = new RemoteJob(parent);
			int batchStart = 0;
			int batchEnd = children.length > BATCH_SIZE ? BATCH_SIZE : children.length;
			//process children until all children have been sent to the UI
			while (batchStart < children.length) {	
				if (monitor.isCanceled()) {
					remoteJob.cancel();
					return;
				}				
				
				if(DEBUG) slowDown(FAKE_LATENCY);
			
				//only send a new batch when the last batch is finished
				if (!remoteJob.isWorking()) {
					int batchLength = batchEnd - batchStart;
					Object[] batch = new Object[batchLength];
					System.arraycopy(children, batchStart, batch, 0, batchLength);
					
					remoteJob.runBatch(batch);
					
					batchStart += batchLength;
					batchEnd = (batchStart + BATCH_SIZE);
					if(batchEnd >= children.length) {
						batchEnd = children.length;
					} 
				}
			}
		} else {
			viewer.refresh();
		}
	}
	
	public void done(Object parent) {
//		if (viewer instanceof AbstractTreeViewer) {
//			RemoteJob remoteJob = new RemoteJob(parent);
//			remoteJob.runBatch(new Object[0]);
//		}
	}
	
	private void slowDown(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
		}
	}
}
