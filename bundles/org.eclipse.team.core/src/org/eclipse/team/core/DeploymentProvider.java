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
import org.eclipse.core.runtime.*;
import org.eclipse.team.internal.core.registry.DeploymentProviderDescriptor;

/**
 * A deployment provider allows synchronization of workspace resources with a remote location. At a minimum
 * it allows pushing resources in the workspace to a remote location and pulling resources from a
 * remote location into the workspace.
 * <p>
 * The difference between a deployment provider and repository provider is the following:
 * <ul>
 * <li>a deployment provider doesn't have full control of workspace resources whereas the repository
 * provider can hook into the IMoveDeleteHook and IFileModificationValidator.
 * <li>multiple deployment providers can be mapped to the same folder whereas there is only one
 * repository provider per project.
 * <li>a deployment provider can be mapped to any folder as long as the mapping is not overlapping
 * whereas the repository provider must be mapped at the project.
 * </ul>
 * </p>
 * @see RepositoryProvider
 * @see IDeploymentProviderManager
 * @since 3.0
 */
public abstract class DeploymentProvider implements IExecutableExtension, IAdaptable {
	
	private String id;
	private IContainer container;
	private String name;
	
	public String getID() {
		return id;
	}
	
	public String getName() {
		return this.name;
	}
	
	public IContainer getMappedContainer() {
		return this.container;
	}
	
	abstract public void init();
	
	abstract public void dispose();
	
	public void setContainer(IContainer container) {
		this.container = container;
	}
	
	abstract public void saveState(IMemento memento);
	
	abstract public void restoreState(IMemento memento);
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
	 */
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		this.id = config.getAttribute(DeploymentProviderDescriptor.ATT_ID);
		this.name = config.getAttribute(DeploymentProviderDescriptor.ATT_NAME);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {		
		return null;
	}
	
	/**
	 * Returns whether a resource can be mapped to multiple deployment providers
	 * of this type. Even if this method returns <code>false</code>, a resource can 
	 * still be mapped to multiple providers whose id differs. By default,
	 * multiple mappings are not supported. Subclasses must override this method
	 * to change this behavior.
	 * @return whether multiple mappings to providers of this type are supported
	 */
	public boolean isMultipleMappingsSupported() {
		return false;
	}
}