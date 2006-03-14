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

import org.eclipse.core.runtime.*;
import org.eclipse.team.core.RepositoryProviderType;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;

/**
 * A decorated state provider is used by the {@link SynchronizationStateTester} to obtain
 * the decorated synchronization state for model elements. A decorated state provider is
 * associated with a {@link RepositoryProviderType} using the adaptable mechanism. A default
 * decoration provider that uses the subscriber of the type is provided.
 * 
 * @see IAdapterManager
 * @see RepositoryProviderType
 * @since 3.2
 */
public abstract class DecoratedStateProvider {

	private ListenerList listeners = new ListenerList(ListenerList.IDENTITY);
	
	/**
	 * Return whether decoration is enabled for the given
	 * model element. If decoration is not enabled, the model
	 * does not need to fire label change events when the team state
	 * of the element changes.
	 * @param element the model element
	 * @return whether decoration is enabled for the given
	 * model element
	 */
	public abstract boolean isDecorationEnabled(Object element);
	
	/**
	 * Return whether the given element is decorated by this provider.
	 * @param element the element being decorated
	 * @return whether the given element is decorated by this provider
	 * @throws CoreException 
	 */
	public abstract boolean isDecorated(Object element) throws CoreException;
	
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
	public abstract int getDecoratedStateMask(Object element);
	
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
	public abstract int getState(Object element, int stateMask, IProgressMonitor monitor) throws CoreException;
	
	/**
	 * Add a decorated state change listener to the provider.
	 * @param listener the listener
	 */
	public void addDecoratedStateChangeListener(IDecoratedStateChangeListener listener) {
		listeners.add(listener);
	}
	
	/**
	 * Remove the decorated state change listener to the provider.
	 * @param listener the listener
	 */
	public void removeDecoratedStateChangeListener(IDecoratedStateChangeListener listener) {
		listeners.remove(listener);
	}
	

}
