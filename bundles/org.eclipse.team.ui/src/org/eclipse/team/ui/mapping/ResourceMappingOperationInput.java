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
package org.eclipse.team.ui.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.internal.resources.mapping.ResourceMapping;
import org.eclipse.core.internal.resources.mapping.ResourceMappingContext;
import org.eclipse.core.internal.resources.mapping.ResourceTraversal;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.IModelProviderDescriptor;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIPlugin;

/**
 * Transform the set of selected resource mappings into the
 * complete set of resource mappings affected by the operation.
 * <p>
 * Here's a summary of the input determination scheme
 * <ol>
 * <li>Obtain selected mappings
 * <li>Project mappings onto resources using the appropriate
 * context(s) in order to obtain a set of ResourceTraverals
 * <li>Determine what model providers are interested in the targeted resources
 * <li>From those model providers, obtain the set of affected resource mappings
 * <li>If the original set is the same as the new set, we are done.
 * <li>if the set differs from the original selection, rerun the mapping process
 * for any new mappings
 *     <ul>
 *     <li>Only need to query model providers for mappings for new resources
 *     <li>If new mappings are obtained, 
 *     ask model provider to compress the mappings?
 *     <li>keep repeating until no new mappings or resources are added
 *     </ul> 
 * <li>Compress the mappings from each provider
 * <li>flag overlapping mappings from independent providers
 * <li>Display the original set and the new set with an explanation
 *     <ul>
 *     <li>The original set and final set may involve mappings from
 *     multiple providers.
 *     <li>The number of providers can be reduced by assuming that
 *     extending models can display the elements of extended models.
 *     Then we are only left with conflicting models.
 *     <li>Could use a content provider approach a.k.a. Common Navigator
 *     or component based approach
 *     </ul> 
 * </ol>
 * @since 3.2
 */
public class ResourceMappingOperationInput {

	private final ResourceMapping[] inputMappings;
	private final ResourceMappingContext[] contexts;
	private final Map inputMappingsToResources = new HashMap();
	private final Map targetMappingsToResources = new HashMap();

	protected static ResourceTraversal[] combineTraversals(ResourceTraversal[] allTraversals) {
		Set zero = new HashSet();
		Set shallow = new HashSet();
		Set deep = new HashSet();
		for (int i = 0; i < allTraversals.length; i++) {
			ResourceTraversal traversal = allTraversals[i];
			switch (traversal.getDepth()) {
			case IResource.DEPTH_ZERO:
				zero.addAll(Arrays.asList(traversal.getResources()));
				break;
			case IResource.DEPTH_ONE:
				shallow.addAll(Arrays.asList(traversal.getResources()));
				break;
			case IResource.DEPTH_INFINITE:
				deep.addAll(Arrays.asList(traversal.getResources()));
				break;
			}
		}
		List result = new ArrayList();
		if (!zero.isEmpty()) {
			result.add(new ResourceTraversal((IResource[]) zero.toArray(new IResource[zero.size()]), IResource.DEPTH_ZERO, IResource.NONE));
		}
		if (!shallow.isEmpty()) {
			result.add(new ResourceTraversal((IResource[]) shallow.toArray(new IResource[shallow.size()]), IResource.DEPTH_ONE, IResource.NONE));
		}
		if (!deep.isEmpty()) {
			result.add(new ResourceTraversal((IResource[]) deep.toArray(new IResource[deep.size()]), IResource.DEPTH_INFINITE, IResource.NONE));
		}
		return (ResourceTraversal[]) result.toArray(new ResourceTraversal[result.size()]);
	}
	
	public ResourceMappingOperationInput(ResourceMapping[] mappings, ResourceMappingContext[] contexts) {
		this.inputMappings = mappings;
		this.contexts = contexts;
	}
	
	public void buildInput(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(null,	IProgressMonitor.UNKNOWN);
		buildInputMappingToResourcesMap(Policy.subMonitorFor(monitor, IProgressMonitor.UNKNOWN));
		Set targetMappings = inputMappingsToResources.keySet();
		Set newResources;
		do {
			newResources = addToTargetMappingToResourceMap(targetMappings, Policy.subMonitorFor(monitor, IProgressMonitor.UNKNOWN));
			targetMappings = internalGetMappingsFromProviders((IResource[]) newResources.toArray(new IResource[newResources.size()]), getAffectedNatures(targetMappings), Policy.subMonitorFor(monitor, IProgressMonitor.UNKNOWN));
		} while (!newResources.isEmpty());
	}

	private String[] getAffectedNatures(Set targetMappings) {
		Set result = new HashSet();
		for (Iterator iter = targetMappings.iterator(); iter.hasNext();) {
			ResourceMapping mapping = (ResourceMapping) iter.next();
            IProject[] projects = mapping.getProjects();
            for (int j = 0; j < projects.length; j++) {
                IProject project = projects[j];
                try {
                    result.addAll(Arrays.asList(project.getDescription().getNatureIds()));
                } catch (CoreException e) {
                    TeamUIPlugin.log(e);
                }
            }
        }
        return (String[]) result.toArray(new String[result.size()]);
	}

	private Set internalGetMappingsFromProviders(IResource[] resources, String[] affectedNatures, IProgressMonitor monitor) throws CoreException {
		Set result = new HashSet();
		IModelProviderDescriptor[] descriptors = ModelProvider.getModelProviderDescriptors();
		for (int i = 0; i < descriptors.length; i++) {
			IModelProviderDescriptor descriptor = descriptors[i];
			ResourceMapping[] mappings = descriptor.getMappings(resources, affectedNatures, contexts, monitor);
			result.addAll(Arrays.asList(mappings));
		}
		return result;
	}

	private Set addToTargetMappingToResourceMap(Set targetMappings, IProgressMonitor monitor) throws CoreException {
		Set newResources = new HashSet();
		for (Iterator iter = targetMappings.iterator(); iter.hasNext();) {
			ResourceMapping mapping = (ResourceMapping) iter.next();
			if (!targetMappingsToResources.containsKey(mapping)) {
				ResourceTraversal[] traversals = getTraversals(mapping, Policy.subMonitorFor(monitor, 100));
				targetMappingsToResources.put(mapping, traversals);
				newResources.addAll(internalGetResources(traversals));
			}
		}
		return newResources;
	}

	private Collection internalGetResources(ResourceTraversal[] traversals) {
		Set result = new HashSet();
		for (int i = 0; i < traversals.length; i++) {
			ResourceTraversal traversal = traversals[i];
			IResource[] resources = traversal.getResources();
			for (int j = 0; j < resources.length; j++) {
				IResource resource = resources[j];
				//TODO: should we check for parent/child relationships?
				result.add(resource);
			}
		}
		return result;
	}

	private void buildInputMappingToResourcesMap(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(null,	inputMappings.length * 100);
		for (int i = 0; i < inputMappings.length; i++) {
			ResourceMapping mapping = inputMappings[i];
			ResourceTraversal[] traversals = getTraversals(mapping, Policy.subMonitorFor(monitor, 100));
			inputMappingsToResources.put(mapping, traversals);
		}
		monitor.done();
	}

	private ResourceTraversal[] getTraversals(ResourceMapping mapping, IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(null, contexts.length * 100);
			List traversals = new ArrayList();
			for (int i = 0; i < contexts.length; i++) {
				ResourceMappingContext context = contexts[i];
				traversals.addAll(Arrays.asList(mapping.getTraversals(context, Policy.subMonitorFor(monitor, 100))));
				Policy.checkCanceled(monitor);
			}
			return combineTraversals((ResourceTraversal[]) traversals.toArray(new ResourceTraversal[traversals.size()]));
		} finally {
			monitor.done();
		}
	}

}
