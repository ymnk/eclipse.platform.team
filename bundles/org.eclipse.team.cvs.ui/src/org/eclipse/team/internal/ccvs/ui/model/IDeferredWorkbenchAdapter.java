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
package org.eclipse.team.internal.ccvs.ui.model;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.progress.IElementCollector;
import org.eclipse.ui.model.IWorkbenchAdapter;

public interface IDeferredWorkbenchAdapter extends IWorkbenchAdapter {
		
	public void fetchDeferredChildren(Object o, IElementCollector collector, IProgressMonitor monitor);
	
	public boolean isDeferred();
	
	public boolean isThreadSafe();
	
	public String getUniqueId();
	
	public boolean isContainer();
}