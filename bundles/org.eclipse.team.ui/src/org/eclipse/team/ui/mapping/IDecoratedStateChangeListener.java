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

import java.util.EventListener;

import org.eclipse.core.resources.IWorkspace;

/**
 * A listener registered with an {@link DecoratedStateProvider} and
 * a {@link SynchronizationStateTester} in order to
 * receive change events when the decorated synchronization state of any
 * resources change. It is the responsibility of clients to determine if
 * a label update is required based on the changed resources. 
 * <p>
 * Change events may not be issued if a local change has resulted in a
 * synchronization state change. It is up to clients to check whether
 * a label update is required for a model element when local resources change
 * by using the resource delta mechanism.
 * 
 * @see IWorkspace#addResourceChangeListener(org.eclipse.core.resources.IResourceChangeListener)
 * @since 3.2
 */
public interface IDecoratedStateChangeListener extends EventListener {
	
	/**
	 * Notification that the decorated synchronization state of resources
	 * has changed.
	 * @param event the event that describes which resources have changed
	 */
	public void decoratedStateChanged(IDecoratedStateChangeEvent event);

}
