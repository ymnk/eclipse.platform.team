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
package org.eclipse.team.internal.ui.sync.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.SyncInfo;

public class SubscriberEventHandler {
	private SyncSetInputFromSubscriber set;
	
	//	When decorations are computed they are added to this cache via decorated() method
	 private List resultCache = new ArrayList();

	 // Objects that need an icon and text computed for display to the user
	 private List awaitingProcessing = new ArrayList();
	 private boolean shutdown = false;

	 Job eventHandlerJob;
	
	/*
	 * Internal event that is processed by the event handler job
	 */
	class Event {
		static final int REMOVAL = 1;
		static final int CHANGE = 2;
		static final int INITIALIZE = 3;
		IResource resource;
		int type;
		int depth;
		SyncInfo result;
		
		Event(IResource resource, int type, int depth) {
			this.resource = resource;
			this.type = type;
			this.depth = depth;
		}		
		public Event(IResource resource, int type, int depth, SyncInfo result) {
			this(resource, type, depth);
			this.result = result;
		}
		public int getDepth() {
			return depth;
		}		
		public IResource getResource() {
			return resource;
		}		
		public int getType() {
			return type;
		}
		public SyncInfo getResult() {
			return result;
		}
	}
	
	public SubscriberEventHandler(SyncSetInputFromSubscriber set) {
		this.set = set;
		
		IResource[] resources = set.getSubscriber().roots(); 
		for (int i = 0; i < resources.length; i++) {
			queueEvent(new Event(resources[i], Event.INITIALIZE, IResource.DEPTH_INFINITE));
			
		}		
		createEventHandlingJob();
		eventHandlerJob.schedule();
	}
		
	public void change(IResource resource, int depth) {
		queueEvent(new Event(resource, Event.CHANGE, depth));
	}

	public void remove(IResource resource) {
		queueEvent(new Event(resource, Event.REMOVAL, IResource.DEPTH_INFINITE));
	}
		
	 /**
	  * Queue the element and its adapted value if it has not been already.
	  */
	 synchronized void queueEvent(Event event) {
		 awaitingProcessing.add(event);
		 if (shutdown || eventHandlerJob == null || eventHandlerJob.getState() != Job.NONE)
			return;
		 else {
			eventHandlerJob.schedule();
		 }
	 }
	
	 /**
	  * Shutdown the decoration.
	  */
	 void shutdown() {
	 	shutdown = true;
		 eventHandlerJob.cancel();
	 }

	 /**
	  * Get the next resource to be decorated.
	  * @return IResource
	  */
	 synchronized Event nextElement() {
		 if (shutdown || awaitingProcessing.isEmpty()) {
			 return null;
		 }
		 return  (Event)awaitingProcessing.remove(0);
	 }

	 /**
	  * Create the Thread used for running decoration.
	  */
	 private void createEventHandlingJob() {
			 eventHandlerJob = new Job("updating synchronize states") {//$NON-NLS-1$
	
			 public IStatus run(IProgressMonitor monitor) {
				 monitor.beginTask("calculating", 100); //$NON-NLS-1$
				 //will block if there are no resources to be decorated
				 Event event;
				 monitor.worked(20);
				 while ((event = nextElement()) != null) {
				 	
				 	if(monitor.isCanceled()) {
				 		return Status.CANCEL_STATUS;
				 	}
				 	
				 	try {
						int type = event.getType();
						switch(type) {
							case Event.REMOVAL : 
								resultCache.add(new Event(event.getResource(), event.getType(), event.getDepth()));
								break;
							case Event.CHANGE :
								List results = new ArrayList();
								collect(event.getResource(), event.getDepth(), monitor, results);
								resultCache.addAll(results);
								break;
							case Event.INITIALIZE :
								Event[] events = getAllOutOfSync(new IResource[] {event.getResource()}, event.getDepth(), monitor);
								resultCache.addAll(Arrays.asList(events));
								break;				 										
						}
					} catch (TeamException e) {
						// TODO: 
					}
				 	
					 if (awaitingProcessing.isEmpty() || resultCache.size() > 10) {
						 dispatchEvents((Event[])resultCache.toArray(new Event[resultCache.size()]));
						 resultCache.clear();
					 }
				 }
				 monitor.worked(80);
				 return Status.OK_STATUS;
			 }
		 };

		eventHandlerJob.setPriority(Job.SHORT);
	 }
	
	private void collect(IResource resource, int depth, IProgressMonitor monitor, List results) throws TeamException {
		
		if(resource.getType() != IResource.FILE && depth != IResource.DEPTH_ZERO) {
			IResource[] members = set.getSubscriber().members((IContainer) resource);
			for (int i = 0; i < members.length; i++) {
				collect(members[i], depth == IResource.DEPTH_INFINITE ? IResource.DEPTH_INFINITE : IResource.DEPTH_ZERO, monitor, results);
			}
		}
		
		SyncInfo info = set.getSubscriber().getSyncInfo(resource, monitor);
		// resource is no longer under the subscriber control
		if (info == null) {
			results.add(new Event(resource, Event.REMOVAL, IResource.DEPTH_ZERO));
		} else { 
			results.add(new Event(resource, Event.CHANGE, IResource.DEPTH_ZERO, info));
		}
	}
	
	private Event[] getAllOutOfSync(IResource[] resources, int depth, IProgressMonitor monitor) throws TeamException {		
		SyncInfo[] infos = set.getSubscriber().getAllOutOfSync(resources, depth, monitor);
		
		// The subscriber hasn't cached out-of-sync resources. We will have to
		// traverse all resources and calculate their state. 
		if(infos == null) {
			List events = new ArrayList();
			for (int i = 0; i < resources.length; i++) {
				collect(resources[i], IResource.DEPTH_INFINITE, monitor, events);
			}
			return (Event[]) events.toArray(new Event[events.size()]);
		// The subscriber has returned the list of out-of-sync resources.
		} else {
			Event[] events = new Event[infos.length];
			for (int i = 0; i < infos.length; i++) {
				SyncInfo info = infos[i];
				events[i] = new Event(info.getLocal(), Event.CHANGE, depth, info);
			}
			return events;
		}		
	}
	
	private void dispatchEvents(Event[] events) {
		set.getSyncSet().beginInput();
		for (int i = 0; i < events.length; i++) {
			Event event = events[i];
			switch(event.getType()) {
				case Event.CHANGE : 
					System.out.println("EventHandler: changed " + event.getResource().getFullPath().toString());
					set.collect(event.getResult());
					break;
				case Event.REMOVAL :
					if(event.getDepth() == IResource.DEPTH_INFINITE) {
						System.out.println("EventHandler: removeAll " + event.getResource().getFullPath().toString());
						set.getSyncSet().removeAllChildren(event.getResource());
					} else {
						System.out.println("EventHandler: remove " + event.getResource().getFullPath().toString());
						set.remove(event.getResource());
					}
					break;
			}
		}
		set.getSyncSet().endInput();
	};
}
