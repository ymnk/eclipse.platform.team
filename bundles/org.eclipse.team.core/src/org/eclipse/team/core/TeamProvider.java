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
package org.eclipse.team.core;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.internal.core.TeamPlugin;
import org.eclipse.team.internal.registry.TeamProviderDescriptor;
import org.eclipse.team.internal.registry.TeamProviderRegistry;

/**
 * A team provider allows synchronization of workspace resources with a remote location. At a minimum
 * it allows pushing resources in the workspace to a remote location and pulling resources from a
 * remote location into the workspace.
 * <p>
 * The difference between a team provider and repository provider is the following:
 * <ul>
 * <li>a team provider doesn't have full control of workspace resources whereas the repository
 * provider can hook into the IMoveDeleteHook and IFileModificationValidator.
 * <li>multiple team providers can be mapped to the same folder whereas there is only one
 * repository provider per project.
 * <li>a team provider can be mapped to any folder as long as the mapping is not self overlapping
 * whereas the repository provider must be mapped at the project.
 * </ul>
 * </p><p>
 * Mapping of a team provider and a container is always deep. This is why overlapping team providers
 * of the same type is not allowed.
 * </p>
 * @see RepositoryProvider
 * @since 3.0
 */
public abstract class TeamProvider {
	
	public final static QualifiedName PROVIDER_SESSION_PROPERTY = new QualifiedName("org.eclipse.team.core", "provider");
	private static TeamProviderRegistry registry = new TeamProviderRegistry();
	
	//  {project -> list of Mapping}
	private static Map mappings = new HashMap(5);

	static class Mapping {
		private TeamProviderDescriptor descriptor;
		private TeamProvider provider;
		private IContainer container;
		
		Mapping(TeamProviderDescriptor descriptor, IContainer container) {
			this.descriptor = descriptor;
			this.container = container;
		}
		public TeamProvider getProvider() throws CoreException {
			if(provider == null) {
				setProvider(descriptor.createProvider());
				// this is where we would restore state and initialize the provider
			}
			return provider;
		}
		public void setProvider(TeamProvider provider) {
			this.provider = provider;
		}
		public IContainer getContainer() {
			return container;
		}
		public TeamProviderDescriptor getDescription() {
			return descriptor;
		}
	}
	
	public static void map(IContainer container, TeamProvider teamProvider) throws TeamException {
		// don't allow is overlapping team providers		
		checkOverlapping(container);
		
		// extension point descriptor should exist
		TeamProviderDescriptor descriptor = registry.find(teamProvider.getID());		
		if(descriptor == null) {
			throw new TeamException("Cannot map provider " + teamProvider.getID() + ". It's extension point description cannot be found.");
		}
		
		// create the new mapping
		Mapping newMapping = new Mapping(descriptor, container);
		newMapping.setProvider(teamProvider);
		IProject project = container.getProject();
		List projectMaps = (List)mappings.get(project);
		if(projectMaps == null) {
			projectMaps = new ArrayList();
			mappings.put(project, projectMaps);
		}
		projectMaps.add(newMapping);
		
		try {
			// install session property
			project.setPersistentProperty();
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}
		
		// initialize provider
		teamProvider.init(container);
		
		// TODO: what kind of event is generated when one is mapped?		
	}
	
	public static void unmap(IContainer container, TeamProvider teamProvider) {
		IProject project = container.getProject();
		List projectMaps = (List)mappings.get(project);
		if(projectMaps != null) {
			projectMaps.remove(container);
			if(projectMaps.isEmpty()) {
				mappings.remove(project);
			}
		}
		
		try {
			// install session property
			project.setSessionProperty(PROVIDER_SESSION_PROPERTY, teamProvider.getID());
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}
		
		// dispose of provider
		teamProvider.dispose();
		
		// TODO: what kind of event is sent when unmapped?
	}
	
	public static TeamProvider getMapping(IResource resource) {
		List projectMappings = (List)mappings.get(resource.getProject());
		String fullPath = resource.getFullPath().toString();
		for (Iterator it = projectMappings.iterator(); it.hasNext();) {
			Mapping m = (Mapping) it.next();
			if(fullPath.startsWith(m.getContainer().getFullPath().toString())) {
				try {
					// lazy initialize of provider must be supported
					return m.getProvider();
				} catch (CoreException e) {
					TeamPlugin.log(e);
				}
			}
		}
		return null;
	}
	
	public static boolean getMappedTo(IResource resource, String id) {
		List projectMappings = (List)mappings.get(resource.getProject());
		String fullPath = resource.getFullPath().toString();
		for (Iterator it = projectMappings.iterator(); it.hasNext();) {
			Mapping m = (Mapping) it.next();
			
			// mapping can be initialize without having provider loaded yet!
			if(m.getDescription().getId().equals(id) && fullPath.startsWith(m.getContainer().getFullPath().toString())) {
				return true;
			}
		}
		return false;
	}
	
	private static void checkOverlapping(IContainer container) throws TeamException {
		List projectMappings = (List)mappings.get(container.getProject());
		String fullPath = container.getFullPath().toString();
		for (Iterator it = projectMappings.iterator(); it.hasNext();) {
			Mapping m = (Mapping) it.next();
			if(fullPath.startsWith(m.getContainer().getFullPath().toString())) {
				throw new TeamException(container.getFullPath().toString() + " is already mapped to " + m.getDescription().getId());
			}
		}
	}
	
	abstract public String getID();
	abstract public IContainer getMappedContainer();	
	abstract protected void init(IContainer container);
	abstract protected void dispose();
	abstract public void saveState();
	abstract public void restoreState();
}