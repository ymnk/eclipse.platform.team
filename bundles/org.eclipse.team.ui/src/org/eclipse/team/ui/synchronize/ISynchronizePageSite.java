/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.synchronize;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IKeyBindingService;
import org.eclipse.ui.IWorkbenchSite;

/**
 * A site which provides access to the context in which this page
 * is being displayed
 * <p>
 * Clients are not intended to implement this interface
 * 
 * @since 3.0
 */
public interface ISynchronizePageSite {

	int VIEW = 1;

	/**
	 * @return
	 */
	int getType();

	/**
	 * @return
	 */
	IWorkbenchSite getWorkbenchSite();
	
	/**
	 * @return
	 */
	Shell getShell();

	/**
	 * @return
	 */
	ISelectionProvider getSelectionProvider();

	/**
	 * @param viewer
	 */
	void setSelectionProvider(ISelectionProvider provider);

	/**
	 * @return
	 */
	IKeyBindingService getKeyBindingService();

	/**
	 * 
	 */
	void setFocus();

}
