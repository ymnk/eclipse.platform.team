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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
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
	// there will be a team provider listener
	TeamProviderRegistry registry;
	Map projectToTeamProviders;
	
	public static void map(IContainer container, String id) throws TeamException {
		// is overlapping?
		TeamProvider existingProvider = null;
		if( (existing = maps.isOverlapping(container, id)) != null) {
			throw new TeamException("Already mapped to " + id + " at " + existingProvider.getMappedContainer());
		}		
		// what kind of event is generated when one is mapped?
	}
	public static void unmap(IContainer container, TeamProvider teamProvider) {
		// what kind of event is setn when unmapped?
	}
	public static TeamProvider[] getMappings(IContainer container) {
		return null;
	}
	public static TeamProvider getMappedTo(IResource resource, String id) {
				
	}
	abstract public String getID();
	abstract public IContainer getMappedContainer();
	abstract protected void configure(IContainer container);
	abstract protected void deconfigure(IContainer container);
	
	//****************************************************************
	//****************************************************************
	
	private static String isOverlapping(IContainer container, String id) {
		Mapping[] mappings = maps.getProjectMappings(container, id);
		if(mappings == null) {
			return null;
		}
		for (int i = 0; i < mappings.length; i++) {
			Mapping mapping = mappings[i];
			String path = mapping.getContainer().getFullPath().toString();
			if(path.substring(container.getFullPath().toString())) {
				return mapping.getId();
			}
		}
		return null;
	}	
}