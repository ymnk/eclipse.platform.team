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
package org.eclipse.team.internal.ui.sync.views;

import org.eclipse.core.resources.IResource;

/**
 * This is the UI model object representing a SyncInfo for a file.
 * The main purpose of this class is to allow menu object contributions
 * to be applied to files.
 */
public class SyncFile extends SyncResource {
	
	public SyncFile(SyncSet syncSet, IResource resource) {
		super(syncSet, resource);
	}

}
