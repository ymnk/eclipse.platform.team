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
package org.eclipse.team.ui.mapping;

/**
 * Listener that, when registered with a synchronization context, gets invoked
 * when the context is disposed.
 * 
 * @since 3.2
 */
public interface IDisposeListener {

	/**
	 * The given context has been disposed.
	 */
	void contextDisposed(ISynchronizeOperationContext context);
}
