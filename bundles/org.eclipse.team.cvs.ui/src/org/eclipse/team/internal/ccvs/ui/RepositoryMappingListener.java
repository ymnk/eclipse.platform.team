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
package org.eclipse.team.internal.ccvs.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.team.core.IRepositoryMappingListener;

public class RepositoryMappingListener implements IRepositoryMappingListener {

	/**
	 * Constructor for RepositoryMappingListener.
	 */
	public RepositoryMappingListener() {
		super();
	}

	/**
	 * @see org.eclipse.team.core.IRepositoryMappingListener#repositoryProviderMapped(IProject)
	 */
	public void repositoryProviderMapped(IProject project) {
	}

	/**
	 * @see org.eclipse.team.core.IRepositoryMappingListener#repositoryProviderUnmapped(IProject)
	 */
	public void repositoryProviderUnmapped(IProject project) {
		CVSDecorator.getActiveCVSDecorator().refreshDeconfiguredProject(project);
	}

}
