/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.mapping;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.registry.TeamDecoratorDescription;
import org.eclipse.team.internal.ui.registry.TeamDecoratorManager;
import org.eclipse.ui.PlatformUI;

public class SubscriberDecoratedStateProvider extends
		DecoratedStateProvider {
	
	Subscriber subscriber;
	
	/**
	 * Return whether decoration is enabled for the given
	 * model element. If decoration is not enabled, the model
	 * does not need to fire label change events when the team state
	 * of the element changes.
	 * @param element the model element
	 * @return whether decoration is enabled for the given
	 * model element
	 */
	public boolean isDecorationEnabled(Object element) {
		ResourceMapping mapping = Utils.getResourceMapping(element);
		if (mapping != null) {
			IProject[] projects = mapping.getProjects();
			return internalIsDecorationEnabled(projects);
		}
		return false;
	}
	
	/**
	 * Return whether the given element is decorated by this provider.
	 * @param element the element being decorated
	 * @return whether the given element is decorated by this provider
	 * @throws CoreException 
	 */
	public boolean isDecorated(Object element) throws CoreException {
		ResourceMapping mapping = Utils.getResourceMapping(element);
		if (mapping != null) {
			ResourceTraversal[] traversals = mapping.getTraversals(ResourceMappingContext.LOCAL_CONTEXT, null);
			for (int i = 0; i < traversals.length; i++) {
				ResourceTraversal traversal = traversals[i];
				IResource[] resources = traversal.getResources();
				for (int j = 0; j < resources.length; j++) {
					IResource resource = resources[j];
					if (getSubscriber().isSupervised(resource))
						return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Return the mask that indicates what state the appropriate team decorator
	 * is capable of decorating. 
	 * 
	 * <p>
	 * The state mask can consist of the following standard flags:
	 * <ul>
	 * <li>The diff kinds of {@link IDiff#ADD}, {@link IDiff#REMOVE}
	 * and {@link IDiff#CHANGE}.
	 * <li>The directions {@link IThreeWayDiff#INCOMING} and
	 * {@link IThreeWayDiff#OUTGOING}.
	 * </ul>
	 * For convenience sake, if there are no kind flags but there is at least
	 * one direction flag then all kinds are assumed.
	 * <p>
	 * The mask can also consist of flag bits that are unique to the repository
	 * provider associated with the resources that the element maps to.
	 * 
	 * @param element
	 *            the model element to be decorated
	 * @return the mask that indicates what state the appropriate team decorator
	 *         will decorate
	 * @see IDiff
	 * @see IThreeWayDiff
	 */
	public int getDecoratedStateMask(Object element) {
		ResourceMapping mapping = Utils.getResourceMapping(element);
		if (mapping != null) {
			IProject[] projects = mapping.getProjects();
			return internalGetDecoratedStateMask(projects);
		}
		return 0;
	}
	
	/**
	 * Return the synchronization state of the given element. Only the portion
	 * of the synchronization state covered by <code>stateMask</code> is
	 * returned. The <code>stateMask</code> should be a subset of the flags returned
	 * by {@link #getDecoratedStateMask(Object)}.
	 * 
	 * @param element the model element
	 * @param stateMask the mask that identifies which state flags are desired if
	 *            present
	 * @param monitor a progress monitor
	 * @return the synchronization state of the given element
	 * @throws CoreException
	 */
	public int getState(Object element, int stateMask, IProgressMonitor monitor) throws CoreException {
		ResourceMapping mapping = Utils.getResourceMapping(element);
		if (mapping != null) {
			try {
				return getSubscriber().getState(mapping, stateMask, monitor);
			} catch (CoreException e) {
				IProject[] projects = mapping.getProjects();
				for (int i = 0; i < projects.length; i++) {
					IProject project = projects[i];
					// Only through the exception if the project for the mapping is accessible
					if (project.isAccessible()) {
						throw e;
					}
				}
			}
		}
		return 0;
	}
	
	/**
	 * Return the subscriber associated with this tester.
	 * @return the subscriber associated with this tester.
	 */
	protected Subscriber getSubscriber() {
		return subscriber;
	}
	
	private int internalGetDecoratedStateMask(IProject[] projects) {
		int stateMask = 0;
		String[] providerIds = getProviderIds(projects);
		for (int i = 0; i < providerIds.length; i++) {
			String providerId = providerIds[i];
			stateMask |= internalGetDecoratedStateMask(providerId);
		}
		return stateMask;
	}
	
	private int internalGetDecoratedStateMask(String providerId) {
		TeamDecoratorDescription decoratorDescription = TeamDecoratorManager.getInstance().getDecoratorDescription(providerId);
		if (decoratorDescription != null)
			return decoratorDescription.getDecoratedDirectionFlags();
		return 0;
	}
	
	private String[] getProviderIds(IProject[] projects) {
		Set providerIds = new HashSet();
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			String id = getProviderId(project);
			if (id != null)
				providerIds.add(id);
		}
		return (String[]) providerIds.toArray(new String[providerIds.size()]);
	}
	
	private String getProviderId(IProject project) {
		RepositoryProvider provider = RepositoryProvider.getProvider(project);
		if (provider != null)
			return provider.getID();
		return null;
	}
	
	private boolean internalIsDecorationEnabled(IProject[] projects) {
		String[] providerIds = getProviderIds(projects);
		for (int i = 0; i < providerIds.length; i++) {
			String providerId = providerIds[i];
			if (internalIsDecorationEnabled(providerId)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean internalIsDecorationEnabled(String providerId) {
		String decoratorId = getDecoratorId(providerId);
		if (decoratorId != null) {
			return PlatformUI.getWorkbench().getDecoratorManager().getEnabled(decoratorId);
		}
		return false;
	}
	
	private String getDecoratorId(String providerId) {
		TeamDecoratorDescription decoratorDescription = TeamDecoratorManager.getInstance().getDecoratorDescription(providerId);
		if (decoratorDescription != null)
			return decoratorDescription.getDecoratorId();
		return null;
	}
}
