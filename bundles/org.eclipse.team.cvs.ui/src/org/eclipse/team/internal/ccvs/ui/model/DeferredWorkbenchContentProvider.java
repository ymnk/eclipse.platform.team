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
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.PendingUpdateAdapter;
import org.eclipse.ui.model.WorkbenchContentProvider;

public class DeferredWorkbenchContentProvider extends WorkbenchContentProvider {

	public class SerializeMeRule implements ISchedulingRule {
		public String id;
		public SerializeMeRule(String id) {
			this.id = id;
		}		
		public boolean isConflicting(ISchedulingRule rule) {
			if(rule instanceof SerializeMeRule) {
				return ((SerializeMeRule)rule).id.equals(id);
			}
			return false;
		}
	}
	
	public boolean hasChildren(Object o) {
		if(o == null) {
			return false;
		}
		IWorkbenchAdapter adapter = getAdapter(o);
		if (adapter instanceof IDeferredWorkbenchAdapter) {
			IDeferredWorkbenchAdapter element = (IDeferredWorkbenchAdapter) adapter;
			if(element.isDeferred()) {
				return element.isContainer();
			}
		}
		return super.hasChildren(o);
	}

	public Object[] getChildren(final Object parent) {

		IWorkbenchAdapter adapter = getAdapter(parent);
		if (adapter instanceof IDeferredWorkbenchAdapter) {
			IDeferredWorkbenchAdapter element = (IDeferredWorkbenchAdapter) adapter;
			if (element.isDeferred()) {				
				startFetchingDeferredChildren(parent, element);								
				return new Object[] { new PendingUpdateAdapter()};
			} else {
				return adapter.getChildren(parent);
			}
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
					System.out.println("DeferredJob: fetching children for parent: " + parent.toString());
					adapter.fetchDeferredChildren(parent, collector, monitor);
					System.out.println("DeferredJob: finished children for parent: " + parent.toString());
					//collector.done(parent);
				} catch(OperationCanceledException e) {
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}
			public boolean belongsTo(Object family) {
				return family == parent;
			}
		};		
		
		if(! adapter.isThreadSafe()) {
			job.setRule(new SerializeMeRule(adapter.getUniqueId()));
		}
		job.schedule();
	}	
}