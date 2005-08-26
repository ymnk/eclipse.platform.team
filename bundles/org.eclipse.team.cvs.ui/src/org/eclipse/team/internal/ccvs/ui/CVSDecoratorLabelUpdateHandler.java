/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.eclipse.core.internal.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.internal.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.ui.actions.WorkspaceTraversalAction;
import org.eclipse.team.internal.core.BackgroundEventHandler;
import org.eclipse.team.ui.IResourceMappingContentProviderFactory;
import org.eclipse.team.ui.IResourceMappingTree;

public class CVSDecoratorLabelUpdateHandler extends BackgroundEventHandler {
	
	private final ILabelUpdater labelUpdater;
	private final Set labelChanges = new HashSet();
	private final Map cache = new WeakHashMap();
	
	private static final int RESOURCE_CHANGE_EVENT = 1;
	
	private class ResourceChangeEvent extends Event {
		
		private final IResource[] resources;
	
		public ResourceChangeEvent(IResource[] resources) {
			super(RESOURCE_CHANGE_EVENT);
			this.resources = resources;
		}

		public IResource[] getResources() {
			return resources;
		}

	}
	
	public interface ILabelUpdater {
		public void updateLabels(Object[] objects);
	}

	protected CVSDecoratorLabelUpdateHandler(String jobName, String errorTitle, ILabelUpdater labelUpdater) {
		super(jobName, errorTitle);
		this.labelUpdater = labelUpdater;
	}

	protected boolean doDispatchEvents(IProgressMonitor monitor) throws TeamException {
		if (labelChanges.isEmpty())
			return false;
		try {
			labelUpdater.updateLabels(labelChanges.toArray());
		} finally {
			labelChanges.clear();
		}
		return true;
	}

	protected void processEvent(Event event, IProgressMonitor monitor) throws CoreException {
		if (event instanceof ResourceChangeEvent) {
			ResourceChangeEvent rce = (ResourceChangeEvent) event;
			IResource[] resources = rce.getResources();
			for (int i = 0; i < resources.length; i++) {
				IResource resource = resources[i];
				accumulateLabelChanges(resource, monitor);
			}
		}

	}

	private void accumulateLabelChanges(IResource resource, IProgressMonitor monitor) {
		if (internalQueueLabelUpdate(resource, monitor)) {
			ResourceMapping[] mappings = getMappings(resource);
			accumulateLabelChanges(mappings, monitor);
		}
	}

	private ResourceMapping[] getMappings(IResource resource) {
		return WorkspaceTraversalAction.convertToParticipantMappings(new ResourceMapping[] { (ResourceMapping)resource.getAdapter(ResourceMapping.class) });
	}

	private void accumulateLabelChanges(ResourceMapping[] mappings, IProgressMonitor monitor) {
		for (int i = 0; i < mappings.length; i++) {
			ResourceMapping mapping = mappings[i];
			if (internalQueueLabelUpdate(mapping, monitor)) {
				ResourceMapping[] parentMappings = getParents(mapping);
				accumulateLabelChanges(parentMappings, monitor);
			}
		}
	}

	private ResourceMapping[] getParents(ResourceMapping mapping) {
		Object o = mapping.getAdapter(IResourceMappingContentProviderFactory.class);
		if (o instanceof IResourceMappingContentProviderFactory) {
			IResourceMappingContentProviderFactory factory = (IResourceMappingContentProviderFactory) o;
			IResourceMappingTree tree = factory.getResourceMappingTree();
			return tree.getParents(mapping);
		}
		return new ResourceMapping[0];
	}

	private boolean internalQueueLabelUpdate(IResource resource, IProgressMonitor monitor) {
		return internalQueueLabelUpdate((ResourceMapping)resource.getAdapter(ResourceMapping.class), monitor);
	}
	
	private boolean internalQueueLabelUpdate(ResourceMapping mapping, IProgressMonitor monitor) {
		if (hasChangedDirtyState(mapping, monitor)) {
			labelChanges.add(mapping.getModelObject());
			return true;
		}
		return false;
	}

	private boolean hasChangedDirtyState(ResourceMapping mapping, IProgressMonitor monitor) {
		try {
			int newChangeState = mapping.calculateChangeState(getContext(), monitor);
			Integer oldChangeState = getCachedChangeState(mapping);
			if (oldChangeState != null && oldChangeState.intValue() == newChangeState) {
				return false;
			}
		    setCacheChangeState(mapping, new Integer(newChangeState));
		    return true;
		} catch (CoreException e) {
			handleException(e);
		}
		return false;
	}

	private void setCacheChangeState(ResourceMapping mapping, Integer integer) {
		cache.put(mapping, integer);
	}

	private Integer getCachedChangeState(ResourceMapping mapping) {
		return (Integer)cache.get(mapping);
	}

	private RemoteResourceMappingContext getContext() {
		return CVSLightweightDecorator.getLocalChangeDeterminationContext(false);
	}

	public void updateLabels(IResource[] changedResources) {
		queueEvent(new ResourceChangeEvent(changedResources), false);
	}


}
