/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 * IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.team.core;

import org.eclipse.core.resources.IProject;

/**
 * Instances of this interface can be registered with the repositoryMappingNotification
 * extension point to receive notification when a repository provider is mapped/unmapped
 * to/from a project.
 * <p>
 * Instances are registered with Team by placing the following snippet in their plugin.xml, 
 * where <code>RepositoryMappingListener</code> is a class that implements IRepositoryMappingListener.
 * </p>
 * 
 * 	<code>
 *  &lt;extension
 *       point="org.eclipse.team.core.repositoryMappingNotification"&gt;
 *    &lt;repository
 *          class="org.eclipse.component.RepositoryMappingListener"&gt;
 *    &lt;/repository&gt;
 *	&lt;/extension&gt;
 * </code>
 * 
 * @see RepositoryProvider
 *
 * @since 2.0
 */
public interface IRepositoryMappingListener {
	
	/**
	 * This method is invoked when a RepositoryProvider is mapped to a 
	 * project. The repository provider can be obtained using
	 * <code>RepositoryProvider.getProvider(project)</code>.
	 * 
	 * @param project the project that was mapped
	 */
	public void repositoryProviderMapped(IProject project);

	/**
	 * This method is invoked when a RepositoryProvider is unmapped from a 
	 * project.
	 * 
	 * @param project the project that was unmapped
	 */
	public void repositoryProviderUnmapped(IProject project);
}
