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
package org.eclipse.team.internal.uijobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.PendingUpdateAdapter;
import org.eclipse.ui.model.WorkbenchContentProvider;

public class DeferredWorkbenchContentProvider extends WorkbenchContentProvider {

	public boolean hasChildren(Object o) {
		if(o == null) {
			return false;
		}
		IWorkbenchAdapter adapter = getAdapter(o);
		if (adapter instanceof IDeferredWorkbenchAdapter) {
			IDeferredWorkbenchAdapter element = (IDeferredWorkbenchAdapter) adapter;
			return element.isContainer();
		}
		return super.hasChildren(o);
	}

	public Object[] getChildren(final Object parent) {
		IWorkbenchAdapter adapter = getAdapter(parent);
		if (adapter instanceof IDeferredWorkbenchAdapter) {
			IDeferredWorkbenchAdapter element = (IDeferredWorkbenchAdapter) adapter;
			startFetchingDeferredChildren(parent, element);								
			return new Object[] { new PendingUpdateAdapter()};
		}
		return super.getChildren(parent);
	}

	private void startFetchingDeferredChildren(final Object parent, final IDeferredWorkbenchAdapter adapter) {
		
		final DeferredElementCollector collector = new DeferredElementCollector(viewer) {
			public void add(Object element, IProgressMonitor monitor) {
				add(new Object[] {element}, monitor);
			}
			public void add(Object[] elements, IProgressMonitor monitor) {
				addChildren(parent, elements, monitor);
			}
		};
						
		// cancel any jobs currently fetching children for this parent
		// TODO: wrap parent into an object unique to deferred content providers?
		Platform.getJobManager().cancel(parent);
		Job job = new Job() {
			public IStatus run(IProgressMonitor monitor) {
				try {								
					adapter.fetchDeferredChildren(parent, collector, monitor);
				} catch(OperationCanceledException e) {
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}
			public boolean belongsTo(Object family) {
				return family == parent;
			}
		};		
		
		job.setRule(adapter.getRule());
		job.schedule();
	}	
}