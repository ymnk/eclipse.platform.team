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

import org.eclipse.core.resources.IResource;

/**
 * A description of the decorated synchronization state changes that have occurred.
 * The resource changes contained in this event indicate that a change has occurred
 * that has an effect on the decorated state. However, it may be the case that the state
 * did not actually change. Clients that wish to determine if the state ha changed must
 * cache the previous state and re-obtain the stat when they receive this event.
 * 
 * @since 3.2
 */
public interface IDecoratedStateChangeEvent {
	
	/**
	 * Return the set of resources that were previosuly undecorated
	 * but are now decorated.
	 * @return the set of resources that were previosuly undecorated
	 * but are now decorated.
	 */
	public IResource[] getAddedRoots();
	
	/**
	 * Return the set of resources that were previosuly decorated
	 * but are now undecorated.
	 * @return the set of resources that were previosuly decorated
	 * but are now undecorated.
	 */
	public IResource[] getRemovedRoots();
	
	/**
	 * Return the set of resources whose decorated state has changed.
	 * @return the set of resources whose decorated state has changed.
	 */
	public IResource[] getChangedResources();
	
	/**
	 * Return whether the resource has any state changes. This returns
	 * <code>true</code> if the resource is included in the set
	 * of changes returned by {@link #getChangedResources()} or
	 * if it is a decendant of a root that is present in a set
	 * returned by {@link #getAddedRoots()} or {@link #getRemovedRoots()}.
	 * 
	 * @param resource the resource
	 * @return whether the resource has any state changes
	 */
	public boolean hasChange(IResource resource);
	
}
