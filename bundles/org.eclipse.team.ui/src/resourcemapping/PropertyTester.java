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
package resourcemapping;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.mapping.IResourceMapper;
import org.eclipse.core.resources.mapping.ITraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.RepositoryProvider;


public class PropertyTester extends org.eclipse.core.expressions.PropertyTester {

	private static final String REPOSITORYID_PROPERTY = "repositoryId"; //$NON-NLS-1$
	
	private boolean sharedWith(IResourceMapper mapper, String id) {
		ITraversal[] traversals;
		try {
			traversals = mapper.getTraversals(null, null);
			for (int j = 0; j < traversals.length; j++) {
				ITraversal traversal = traversals[j];
				IProject[] projects = traversal.getProjects();
				if(projects.length == 0) return false;
				for (int k = 0; k < projects.length; k++) {
					IProject project = projects[k];
					RepositoryProvider provider = RepositoryProvider.getProvider(project);
					if (provider == null || !provider.getID().equals(id))
						return false;
				}
			}
			return true;
		} catch (CoreException e) {
			return false;
		}
	}
	
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		IResourceMapper mapper = (IResourceMapper)receiver;
		if(property.equals(REPOSITORYID_PROPERTY)) {
			// check that all selected resources are shared with the same
			// repository provider.
			if(args == null) return false;
			String repoId = (String)args[0];
			return sharedWith(mapper, repoId);
		}
		return false;
	}
}
