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
package org.eclipse.team.ui.synchronize.views;

import org.eclipse.core.runtime.CoreException;

/**
 * This interface provides access to the information about a logical view that
 * can be used to display <code>SyncInfoDiffNode</code> instances in a 
 * tree viewer.
 * 
 * <p>
 * This interface is not intended to be implemented by clients. Clients who 
 * want to provide a logical view should implement a <code>LogicalViewProvider</code>.
 * 
 * @see LogicalViewProvider
 * 
 * @since 3.0
 */
public interface ILogicalView {
	
	String getId();
	
	String getLabel();
	
	String getDescription();
	
	LogicalViewProvider getLogicalViewProvider() throws CoreException;

}
