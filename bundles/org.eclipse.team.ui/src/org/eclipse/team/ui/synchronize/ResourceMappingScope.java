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
package org.eclipse.team.ui.synchronize;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.internal.resources.mapping.ResourceMapping;
import org.eclipse.core.internal.resources.mapping.ResourceTraversal;
import org.eclipse.core.resources.IResource;

/**
 * A synchronize scope whose roots are defined by the traversals
 * obtained from a set of resource mappings.
 * @since 3.2
 */
public class ResourceMappingScope extends AbstractSynchronizeScope {

	private ResourceMapping[] mappings;
	private ResourceTraversal[] traversals;
	private String name;
	private IResource[] roots;
	
	public ResourceMappingScope(String name, ResourceMapping[] mappings, ResourceTraversal[] traversals) {
		this.name = name;
		this.mappings = mappings;
		this.traversals = traversals;
	}

	public String getName() {
		return name;
	}

	public IResource[] getRoots() {
		if (roots == null) {
			Set result = new HashSet();
			for (int i = 0; i < traversals.length; i++) {
				ResourceTraversal traversal = traversals[i];
				IResource[] resources = traversal.getResources();
				for (int j = 0; j < resources.length; j++) {
					IResource resource = resources[j];
					//TODO: should we check for parent/child relationships?
					result.add(resource);
				}
			}
			roots = (IResource[]) result.toArray(new IResource[result.size()]);
		}
		return roots;
	}

	public ResourceMapping[] getMappings() {
		return mappings;
	}

}
