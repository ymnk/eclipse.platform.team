package org.eclipse.team.core;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IProjectNatureDescriptor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.internal.Policy;

/**
 * Describes a type of repository provider snf provides
 * 
 * @see RepositoryProvider
 */
abstract public class RepositoryProviderType {
	
	private final static String TEAM_SETID = "org.eclipse.team.repository-provider";
	
	/**
	 * Registers a provider type. This method is not intended to be called by clients and should only be
	 * called by the team plugin.
	 * 
	 * @throws TeamException if the provider type is already registered.
	 */
	/*package*/ final static void addProviderType(RepositoryProviderType providerType) throws TeamException {
	}

	/**
	 * Returns all known (registered) <code>RepositoryProviderType</code>.
	 * 
	 * @return an array of registered <code>RepositoryProviderType</code> instances.
	 */
	final public static String[] getAllProviderTypeIds() {
		IProjectNatureDescriptor[] desc = ResourcesPlugin.getWorkspace().getNatureDescriptors();
		List teamSet = new ArrayList();
		for (int i = 0; i < desc.length; i++) {
			List sets = new ArrayList(Arrays.asList(desc[i].getNatureSetIds()));
			if(sets.contains(TEAM_SETID)) {
				teamSet.add(desc[i].getNatureId());
			}
		}
		return (String[]) teamSet.toArray(new String[teamSet.size()]);
	}
	
	/**
	 * Returns the provider for a given IProject or <code>null</code> if a provider is not associated with 
	 * the project. This assumes that only one repository provider can be associated with a project at a
	 * time.
	 * 
	 * @return a repository provider for the project or <code>null</code> if the project is not 
	 * associated with a provider.
	 */
	final public static RepositoryProvider getProvider(IProject project) {
		if(project.isAccessible()) {
			try {
				IProjectDescription projectDesc = project.getDescription();
				String[] natureIds = projectDesc.getNatureIds();
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				for (int i = 0; i < natureIds.length; i++) {
					IProjectNatureDescriptor desc = workspace.getNatureDescriptor(natureIds[i]);
					List sets = new ArrayList(Arrays.asList(desc.getNatureSetIds()));
					if(sets.contains(TEAM_SETID)) {
						return getProvider(project, natureIds[i]);
					}			
				}
			} catch(CoreException e) {
				TeamPlugin.log(new Status(IStatus.ERROR, TeamPlugin.ID, 0, Policy.bind(""), e)); //$NON-NLS-1$
			}
		}
		return null;
	}
	
	/**
	 * Returns a provider of type the receiver if associated with the given project or <code>null</code>
	 * if the project is not associated with a provider of that type.
	 * 
	 * @return the repository provider
	 */
	final public static RepositoryProvider getProvider(IProject project, String id) {
		try {
			if(project.exists() && project.isOpen()) {
				return (RepositoryProvider)project.getNature(id);
			}
		} catch(ClassCastException e) {
			TeamPlugin.log(new Status(IStatus.ERROR, TeamPlugin.ID, 0, Policy.bind("RepositoryProviderTypeRepositoryProvider_assigned_to_the_project_must_be_a_subclass_of_RepositoryProvider___2") + id, e)); //$NON-NLS-1$
		} catch(CoreException ex) {
			// would happen if provider nature id is not registered with the resources plugin
			TeamPlugin.log(new Status(IStatus.WARNING, TeamPlugin.ID, 0, Policy.bind("RepositoryProviderTypeRepositoryProvider_not_registered_as_a_nature_id___3") + id, ex)); //$NON-NLS-1$
		}
		return null;
	}
	
	/**
	 * Returns all instances of the providers of this type.
	 * 
	 * @return an array of repository providers
	 */
	final public static RepositoryProvider[] getAllInstances(String id) {
		// traverse projects in workspace and return the list of project that have our id as the nature id.
		List projectsWithMyId = new ArrayList();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			RepositoryProvider provider = getProvider(projects[i], id);
			if(provider!=null) {
				projectsWithMyId.add(provider);
			}
		}
		return (RepositoryProvider[]) projectsWithMyId.toArray(new RepositoryProvider[projectsWithMyId.size()]);
	}
}