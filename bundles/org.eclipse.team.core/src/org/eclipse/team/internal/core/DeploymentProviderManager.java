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
package org.eclipse.team.internal.core;

import java.io.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.*;
import org.eclipse.team.internal.registry.TeamProviderDescriptor;
import org.eclipse.team.internal.registry.TeamProviderRegistry;

public class DeploymentProviderManager implements IDeploymentProviderManager  {
	public final static QualifiedName PROVIDER_SESSION_PROPERTY = new QualifiedName("org.eclipse.team.core", "provider");
	private TeamProviderRegistry registry = new TeamProviderRegistry();
	
	//  {project -> list of Mapping}
	private Map mappings = new HashMap(5);
	
	//	save context constants
	private final static String CTX_PROVIDERS = "deploymentProviders"; //$NON-NLS-1$
	private final static String CTX_PROVIDER = "provider"; //$NON-NLS-1$
	private final static String CTX_ID = "id"; //$NON-NLS-1$
	private final static String CTX_PATH = "container_path"; //$NON-NLS-1$
	private final static String CTX_PROVIDER_DATA = "data"; //$NON-NLS-1$
	private final static String FILENAME = ".deployments"; //$NON-NLS-1$
	
	static class Mapping {
		private TeamProviderDescriptor descriptor;
		private DeploymentProvider provider;
		private IContainer container;
		private IMemento savedState;
		
		Mapping(TeamProviderDescriptor descriptor, IContainer container) {
			this.descriptor = descriptor;
			this.container = container;
		}
		public DeploymentProvider getProvider() throws TeamException {
			if(provider == null) {
				try {
					this.provider = descriptor.createProvider();
					this.provider.restoreState(savedState);
					this.savedState = null;
				} catch (CoreException e) {
					throw TeamException.asTeamException(e);
				}				
			}
			return provider;
		}
		public void setProvider(DeploymentProvider provider) {
			this.provider = provider;
			this.savedState = null;
		}
		public IContainer getContainer() {
			return container;
		}
		public TeamProviderDescriptor getDescription() {
			return descriptor;
		}
		public void setProviderState(IMemento savedState) {
			this.savedState = savedState;
		}
	}
	
	public void map(IContainer container, DeploymentProvider teamProvider) throws TeamException {
		// TODO: make concurrent safe!!
		// don't allow is overlapping team providers		
		checkOverlapping(container);
		
		// extension point descriptor must exist
		TeamProviderDescriptor descriptor = registry.find(teamProvider.getID());		
		if(descriptor == null) {
			throw new TeamException("Cannot map provider " + teamProvider.getID() + ". It's extension point description cannot be found.");
		}
		
		// create the new mapping
		Mapping m = map(container, descriptor);
		m.setProvider(teamProvider);
		
		//try {
		// install session property
		//project.setPersistentProperty();
		//} catch (CoreException e) {
		//	throw TeamException.asTeamException(e);
		//}
		
		// initialize provider
		// teamProvider.init(container);
		
		saveState(container.getProject());
		// TODO: what kind of event is generated when one is mapped?		
	}
	
	public void unmap(IContainer container, DeploymentProvider teamProvider) throws TeamException {
		// TODO: make concurrent safe!!
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
		// teamProvider.dispose();
		saveState(container.getProject());
		
		// TODO: what kind of event is sent when unmapped?
	}
	
	public DeploymentProvider getMapping(IResource resource) {
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
	
	public boolean getMappedTo(IResource resource, String id) {
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
	
	private void checkOverlapping(IContainer container) throws TeamException {
		List projectMappings = (List)mappings.get(container.getProject());
		String fullPath = container.getFullPath().toString();
		for (Iterator it = projectMappings.iterator(); it.hasNext();) {
			Mapping m = (Mapping) it.next();
			if(fullPath.startsWith(m.getContainer().getFullPath().toString())) {
				throw new TeamException(container.getFullPath().toString() + " is already mapped to " + m.getDescription().getId());
			}
		}
	}
	
	private Mapping map(IContainer container, TeamProviderDescriptor description) {
		Mapping newMapping = new Mapping(description, container);
		IProject project = container.getProject();
		List projectMaps = (List)mappings.get(project);
		if(projectMaps == null) {
			projectMaps = new ArrayList();
			mappings.put(project, projectMaps);
		}
		projectMaps.add(newMapping);
		return newMapping;
	}
	
	/**
	 * Saves a file containing the list of participant ids that are registered with this
	 * manager. Each participant is also given the chance to save it's state. 
	 */
	private void saveState(IProject project) throws TeamException {
		// TODO: have to handle the whole overwritten - editing crap with this file!!
		XMLMemento xmlMemento = XMLMemento.createWriteRoot(CTX_PROVIDERS);	
		List children = new ArrayList();
		List providers = (List)mappings.get(project);
		for (Iterator it2 = providers.iterator(); it2.hasNext(); ) {
			Mapping mapping = (Mapping) it2.next();
			IMemento node = xmlMemento.createChild(CTX_PROVIDER);
			node.putString(CTX_ID, mapping.getDescription().getId());
			node.putString(CTX_PATH, mapping.getContainer().getFullPath().toString());
			mapping.getProvider().saveState(node.createChild(CTX_PROVIDER_DATA));
		}
		try {
			IFile settingsFile = project.getFile(FILENAME);
			if(! settingsFile.exists()) {
				settingsFile.create(new ByteArrayInputStream(new byte[0]), true, null);
			}
			Writer writer = new BufferedWriter(new FileWriter(settingsFile.getLocation().toFile()));
			try {
				xmlMemento.save(writer);
			} finally {
				writer.close();
				settingsFile.refreshLocal(IResource.DEPTH_ZERO, null);
			}
		} catch (IOException e) {
			//TeamPlugin.log(e); //$NON-NLS-1$
		} catch(CoreException ce) {
			throw TeamException.asTeamException(ce);
		}
	}
	
	private void restoreState(IProject project) throws TeamException, CoreException {
		// TODO: have to handle the whole overwritten - editing crap with this file!!
		IFile file = project.getFile(FILENAME);	
		if(! file.exists()) return;
		Reader reader;
		try {
			reader = new BufferedReader(new FileReader(file.getLocation().toFile()));
		} catch (FileNotFoundException e) {
			return;
		}
		List participants = new ArrayList();
		IMemento memento = XMLMemento.createReadRoot(reader);
		IMemento[] providers = memento.getChildren(CTX_PROVIDER);
		for (int i = 0; i < providers.length; i++) {
			IMemento memento2 = providers[i];			
			String id = memento2.getString(CTX_ID);
			IPath location = new Path(memento2.getString(CTX_PATH));
			
			if(! project.exists(location)) {
				TeamPlugin.log(IStatus.ERROR, "resource no longer exists", null);
			}
			IContainer container = location.isEmpty() ? (IContainer)project : project.getFolder(location);			
			TeamProviderDescriptor desc = registry.find(id);				
			if(desc != null) {
				Mapping m = map(container, desc);
				m.setProviderState(memento2);				
			} else {
				TeamPlugin.log(IStatus.ERROR, Policy.bind("SynchronizeManager.9", id), null); //$NON-NLS-1$
			}
		}
	}
}