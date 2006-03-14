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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.RepositoryProviderType;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.internal.core.IRepositoryProviderListener;
import org.eclipse.team.internal.core.RepositoryProviderManager;
import org.eclipse.team.internal.ui.Utils;

public class WorkspaceDecoratedStateProvider extends DecoratedStateProvider {

	private Map providers = new HashMap();
	private IRepositoryProviderListener sharingListener;
	
	public WorkspaceDecoratedStateProvider() {
		sharingListener = new IRepositoryProviderListener() {
			public void providerUnmapped(IProject project) {
				// We don't need to worry about this
			}
		
			public void providerMapped(RepositoryProvider provider) {
				String id = provider.getID();
				listenerForStateChangesForId(id);
			}
		
		};
		RepositoryProviderManager.getInstance().addListener(sharingListener);
	}

	/**
	 * Return whether decoration is enabled for the given
	 * model element. If decoration is not enabled, the model
	 * does not need to fire label change events when the team state
	 * of the element changes.
	 * @param element the model element
	 * @return whether decoration is enabled for the given
	 * model element
	 */
	public final boolean isDecorationEnabled(Object element) {
		DecoratedStateProvider provider = getDecoratedStateProvider(element);
		if (provider != null)
			provider.isDecorationEnabled(element);
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.DecoratedStateProvider#isDecorated(java.lang.Object)
	 */
	public boolean isDecorated(Object element) throws CoreException {
		DecoratedStateProvider provider = getDecoratedStateProvider(element);
		if (provider != null)
			provider.isDecorated(element);
		return false;
	}
	
	/**
	 * Return the mask that indicates what state the appropriate team decorator
	 * is capable of decorating. The state is determined by querying the
	 * <code>org.eclipse.team.ui.teamDecorators</code> extension point.
	 * 
	 * <p>
	 * The state mask can consist of the following flags:
	 * <ul>
	 * <li>The diff kinds of {@link IDiff#ADD}, {@link IDiff#REMOVE}
	 * and {@link IDiff#CHANGE}.
	 * <li>The directions {@link IThreeWayDiff#INCOMING} and
	 * {@link IThreeWayDiff#OUTGOING}.
	 * </ul>
	 * For convenience sake, if there are no kind flags but there is at least
	 * one direction flag then all kinds are assumed.
	 * 
	 * @param element
	 *            the model element to be decorated
	 * @return the mask that indicates what state the appropriate team decorator
	 *         will decorate
	 * @see IDiff
	 * @see IThreeWayDiff
	 */
	public final int getDecoratedStateMask(Object element) {
		DecoratedStateProvider provider = getDecoratedStateProvider(element);
		if (provider != null)
			return provider.getDecoratedStateMask(element);
		return 0;
	}

	/**
	 * Return the synchronization state of the given element. Only the portion
	 * of the synchronization state covered by <code>stateMask</code> is
	 * returned. By default, this method calls
	 * {@link Subscriber#getState(ResourceMapping, int, IProgressMonitor)}.
	 * <p>
	 * Team decorators will use this method to detemine how to decorate the
	 * provided element. The {@link #getDecoratedStateMask(Object)} returns the
	 * state that the corresponding team decorator is capable of decorating but
	 * the decorator may be configured to decorate only a portion of that state.
	 * When the team decorator invokes this method, it will pass the stateMask that
	 * it is currently configured to show. If a mask of zero is provided, this indicates
	 * that the team decorator is not configured to decorate the synchronization state
	 * of model elements.
	 * <p>
	 * Subclasses may want to override this method in the following cases:
	 * <ol>
	 * <li>The subclass wishes to fire appropriate label change events when the
	 * decorated state of a model element changes. In this case the subclass
	 * can override this method to record the stateMask and returned state. It can
	 * use this recorded information to determine whether local changes or subscriber changes
	 * result in a change in the deocrated sstate of the model element.
	 * <li>The subclasses wishes to provide a more accurate change description for a model
	 * element that represents only a portion of the file. In this case, the subclass can
	 * use the remote file contents available from the subscriber to determine whether
	 * </ol>
	 * 
	 * @param element the model element
	 * @param stateMask the mask that identifies which state flags are desired if
	 *            present
	 * @param monitor a progress monitor
	 * @return the synchronization state of the given element
	 * @throws CoreException
	 * @see Subscriber#getState(ResourceMapping, int, IProgressMonitor)
	 */
	public int getState(Object element, int stateMask, IProgressMonitor monitor) throws CoreException {
		DecoratedStateProvider provider = getDecoratedStateProvider(element);
		if (provider != null)
			return provider.getState(element, stateMask, monitor);
		return 0;
	}
	
	private DecoratedStateProvider getDecoratedStateProvider(Object element) {
		RepositoryProviderType type = getProviderType(element);
		if (type != null)
			return (DecoratedStateProvider)Utils.getAdapter(type, DecoratedStateProvider.class);
		return null;
	}
	
	private DecoratedStateProvider getDecoratedStateProviderForId(String id) {
		RepositoryProviderType type = getProviderTypeForId(id);
		if (type != null)
			return (DecoratedStateProvider)Utils.getAdapter(type, DecoratedStateProvider.class);
		return null;
	}

	private RepositoryProviderType getProviderType(Object element) {
		ResourceMapping mapping = Utils.getResourceMapping(element);
		if (mapping != null) {
			String providerId = getProviderId(mapping.getProjects());
			if (providerId != null)
				return getProviderTypeForId(providerId);
		}
		return null;
	}

	private String getProviderId(IProject[] projects) {
		String id = null;
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			String nextId = getProviderId(project);
			if (id == null)
				id = nextId;
			else if (!id.equals(nextId))
				return null;
		}
		return id;
	}

	private String getProviderId(IProject project) {
		RepositoryProvider provider = RepositoryProvider.getProvider(project);
		if (provider != null)
			return provider.getID();
		return null;
	}
	
	private RepositoryProviderType getProviderTypeForId(String providerId) {
		return RepositoryProviderType.getProviderType(providerId);
	}
	
	/* private */ void listenerForStateChangesForId(String id) {
		if (!providers.containsKey(id)) {
			DecoratedStateProvider provider = getDecoratedStateProviderForId(id);
			if (provider != null) {
				providers.put(id, provider);
				provider.addDecoratedStateChangeListener(listener);
			}
		}
	}

}
