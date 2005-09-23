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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.internal.resources.mapping.ResourceMapping;
import org.eclipse.core.internal.resources.mapping.ResourceMappingContext;
import org.eclipse.core.internal.resources.mapping.ResourceTraversal;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.ltk.core.refactoring.participants.TeamArguments;
import org.eclipse.ltk.core.refactoring.participants.TeamParticipant;
import org.eclipse.ltk.core.refactoring.participants.TeamProcessor;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.mapping.DefaultResourceMappingMerger;
import org.eclipse.team.ui.TeamOperation;
import org.eclipse.ui.IWorkbenchPart;

/**
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
 * <li>Use model provider relationships to result?
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
 */
public abstract class ResourceMappingOperation extends TeamOperation {

	private ResourceMapping[] mappings;

    public static TeamParticipant[] loadParticipants(RefactoringStatus status, TeamProcessor processor, ResourceMapping[] selectedMappings) throws CoreException {
        List result= new ArrayList();
        SharableParticipants sharedParticipants = new SharableParticipants();
        TeamArguments arguments = new TeamArguments() {};
        String[] natures = getNatures(selectedMappings);
        for (int i = 0; i < selectedMappings.length; i++) {
            ResourceMapping mapping = selectedMappings[i];
            result.addAll(Arrays.asList(ParticipantManager.loadTeamParticipants(
                status, processor, mapping.getModelObject(), arguments, natures, sharedParticipants)));
            result.addAll(Arrays.asList(ParticipantManager.loadTeamParticipants(
                    status, processor, mapping, arguments, natures, sharedParticipants)));
        }
        IResource[] resources= getResources(selectedMappings);
        for (int i= 0; i < resources.length; i++) {
            IResource resource= resources[i];
            result.addAll(Arrays.asList(ParticipantManager.loadTeamParticipants(
                status, processor, resource, arguments, natures, sharedParticipants)));
            
        }
        return (TeamParticipant[])result.toArray(new TeamParticipant[result.size()]);
    }
    
    private static IResource[] getResources(ResourceMapping[] selectedMappings) throws CoreException {
        Set result = new HashSet();
        for (int i = 0; i < selectedMappings.length; i++) {
            ResourceMapping mapping = selectedMappings[i];
            ResourceTraversal[] traversals = mapping.getTraversals(ResourceMappingContext.LOCAL_CONTEXT, new NullProgressMonitor());
            for (int j = 0; j < traversals.length; j++) {
                ResourceTraversal traversal = traversals[j];
                IResource[] resources = traversal.getResources();
                if (traversal.getDepth() == IResource.DEPTH_INFINITE) {
                    result.addAll(Arrays.asList(resources));
                } else if (traversal.getDepth() == IResource.DEPTH_ONE) {
                    for (int k = 0; k < resources.length; k++) {
                        IResource resource = resources[k];
                        if (resource.getType() == IResource.FILE) {
                            result.add(resource);
                        } else {
                            IResource[] members = ((IContainer)resource).members();
                            for (int index = 0; index < members.length; index++) {
                                IResource member = members[index];
                                if (member.getType() == IResource.FILE) {
                                    result.add(member);
                                }
                            }
                        }
                    }
                } else {
                    for (int k = 0; k < resources.length; k++) {
                        IResource resource = resources[k];
                        if (resource.getType() == IResource.FILE) {
                            result.add(resource);
                        }
                    }
                }
            }
        }
        return (IResource[]) result.toArray(new IResource[result.size()]);
    }

    private static String[] getNatures(ResourceMapping[] selectedMappings) {
        Set result = new HashSet();
        for (int i = 0; i < selectedMappings.length; i++) {
            ResourceMapping mapping = selectedMappings[i];
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
    
	protected ResourceMappingOperation(IWorkbenchPart part, ResourceMapping[] mappings) {
		super(part);
		setMappings(mappings);
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		buildInput(monitor);
		execute(monitor);

	}

	/**
	 * Adjust the input of the operation according to the selected
	 * resource mappings and the set of interested participants
	 * @param monitor 
	 */
	protected void buildInput(IProgressMonitor monitor) throws InvocationTargetException {
		
	}

	protected abstract void execute(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException;

	public ResourceMapping[] getMappings() {
		return mappings;
	}

	public void setMappings(ResourceMapping[] mappings) {
		this.mappings = mappings;
	}

	protected IResourceMappingMerger getMerger(ModelProvider provider) {
		Object o = provider.getAdapter(IResourceMappingMerger.class);
		if (o instanceof IResourceMappingMerger) {
			return (IResourceMappingMerger) o;	
		}
		return new DefaultResourceMappingMerger();
	}
	
	protected IResourceMappingManualMerger getManualMerger(ModelProvider provider) {
		Object o = provider.getAdapter(IResourceMappingManualMerger.class);
		if (o instanceof IResourceMappingManualMerger) {
			return (IResourceMappingManualMerger) o;	
		}
		return getDefaultMaualMerger();
	}

	protected abstract IResourceMappingManualMerger getDefaultMaualMerger();
}
