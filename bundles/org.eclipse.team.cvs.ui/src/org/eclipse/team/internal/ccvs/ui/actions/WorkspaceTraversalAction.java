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
package org.eclipse.team.internal.ccvs.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.core.internal.resources.mapping.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.*;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.core.subscribers.SubscriberResourceMappingContext;
import org.eclipse.team.internal.ui.dialogs.AdditionalMappingsDialog;
import org.eclipse.ui.PlatformUI;


/**
 * A specialized workspace actions that operates on resource traversals
 * instead of resources/
 */
public abstract class WorkspaceTraversalAction extends WorkspaceAction {

    /**
     * Return the selected mappings that contain resources 
     * within a CVS managed project.
     * @return the selected mappings that contain resources 
     * within a CVS managed project
     */
    protected ResourceMapping[] getCVSResourceMappings() {
        ResourceMapping[] selectedMappings = getSelectedResourceMappings(CVSProviderPlugin.getTypeId());
        ResourceMapping[] allMappings = convertToParticipantMappings(selectedMappings);
        return showAllMappings(selectedMappings, allMappings);
    }
    
    private ResourceMapping[] showAllMappings(final ResourceMapping[] selectedMappings, final ResourceMapping[] allMappings) {
    	if (isEqualArrays(selectedMappings, allMappings))
    		return allMappings;
    	
        final boolean[] canceled = new boolean[] { false };
        getShell().getDisplay().syncExec(new Runnable() {
            public void run() {
                AdditionalMappingsDialog dialog = new AdditionalMappingsDialog(getShell(), "Participating Elements", selectedMappings, allMappings);
                int result = dialog.open();
                canceled[0] = result != Dialog.OK;
            }
        
        });
        
        if (canceled[0]) {
            return new ResourceMapping[0];
        }
        return allMappings;
    }

    private boolean isEqualArrays(ResourceMapping[] selectedMappings, ResourceMapping[] allMappings) {
    	if (selectedMappings.length != allMappings.length)
    		return false;
		for (int i = 0; i < allMappings.length; i++) {
			ResourceMapping mapping = allMappings[i];
			boolean matchFound = false;
			for (int j = 0; j < selectedMappings.length; j++) {
				ResourceMapping selected = selectedMappings[j];
				if (selected.equals(mapping)) {
					matchFound = true;
					break;
				}
			}
			if (!matchFound) return false;
		}
		return true;
	}

	/*
     * Use the registered teamParticpants to determine if additional mappings should be included
     * in the operation.
     */
    public static ResourceMapping[] convertToParticipantMappings(ResourceMapping[] selectedMappings) {
        TeamProcessor processor = new TeamProcessor() {
            public String getIdentifier() {
                return "org.eclipse.team.cvs.ui.teamProcessor";
            }
        };
        RefactoringStatus status = new RefactoringStatus();
        try {
            Map result = new HashMap();
            for (int i = 0; i < selectedMappings.length; i++) {
                ResourceMapping mapping = selectedMappings[i];
                result.put(mapping.getModelObject(), mapping);
            }
            TeamParticipant[] participants = loadParticipants(status, processor, selectedMappings);
            for (int i = 0; i < participants.length; i++) {
                TeamParticipant participant = participants[i];
                ResourceMapping[] mappings = participant.getMappings(ResourceMappingContext.LOCAL_CONTEXT, new NullProgressMonitor());
                for (int j = 0; j < mappings.length; j++) {
                    ResourceMapping mapping = mappings[j];
                    result.put(mapping.getModelObject(), mapping);
                }
            }
            return (ResourceMapping[]) result.values().toArray(new ResourceMapping[result.size()]);
        } catch (CoreException e) {
            // TODO: Should notify user directly of error
            CVSUIPlugin.log(e);
        }
        return selectedMappings;
    }

    private static TeamParticipant[] loadParticipants(RefactoringStatus status, TeamProcessor processor, ResourceMapping[] selectedMappings) throws CoreException {
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
                    CVSUIPlugin.log(e);
                }
            }
        }
        return (String[]) result.toArray(new String[result.size()]);
    }

    protected static IResource[] getRootTraversalResources(ResourceMapping[] mappings, ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
        List result = new ArrayList();
        for (int i = 0; i < mappings.length; i++) {
            ResourceMapping mapping = mappings[i];
            ResourceTraversal[] traversals = mapping.getTraversals(context, monitor);
            for (int j = 0; j < traversals.length; j++) {
                ResourceTraversal traversal = traversals[j];
                IResource[] resources = traversal.getResources();
                for (int k = 0; k < resources.length; k++) {
                    IResource resource = resources[k];
                    if (RepositoryProvider.getProvider(resource.getProject(), CVSProviderPlugin.getTypeId()) != null) {
                        result.add(resource);
                    }
                }
            }
        }
        return (IResource[]) result.toArray(new IResource[result.size()]);
    }

    protected Subscriber getWorkspaceSubscriber() {
        return CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber();
    }
    
    protected IResource[] getResourcesToCompare(final Subscriber subscriber) throws InvocationTargetException {
        return getResourcesToCompare(getCVSResourceMappings(), subscriber);
    }
    
    public static IResource[] getResourcesToCompare(final ResourceMapping[] mappings, final Subscriber subscriber) throws InvocationTargetException {
        // Determine what resources need to be synchronized.
        // Use a resource mapping context to include any relevant remote resources
        final IResource[][] resources = new IResource[][] { null };
        try {
            PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        resources[0] = getRootTraversalResources(
                                mappings, 
                                SubscriberResourceMappingContext.getCompareContext(subscriber), 
                                monitor);
                    } catch (CoreException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            
            });
        } catch (InterruptedException e) {
            // Canceled
            return null;
        }
        return resources[0];
    }
    
    public static IResource[] getProjects(IResource[] resources) {
        Set projects = new HashSet();
        for (int i = 0; i < resources.length; i++) {
            IResource resource = resources[i];
            projects.add(resource.getProject());
        }
        return (IResource[]) projects.toArray(new IResource[projects.size()]);
    }
    
    public static boolean isLogicalModel(ResourceMapping[] mappings) {
        for (int i = 0; i < mappings.length; i++) {
            ResourceMapping mapping = mappings[i];
            if (! (mapping.getModelObject() instanceof IResource) ) {
                return true;
            }
        }
        return false;
    }
}
