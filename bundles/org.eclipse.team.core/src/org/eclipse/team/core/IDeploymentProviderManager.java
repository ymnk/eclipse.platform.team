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

public interface IDeploymentProviderManager {
	public void map(IContainer container, DeploymentProvider teamProvider) throws TeamException;
	public void unmap(IContainer container, DeploymentProvider teamProvider) throws TeamException;
	public DeploymentProvider getMapping(IResource resource);
	public boolean getMappedTo(IResource resource, String id);
}