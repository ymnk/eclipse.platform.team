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
import org.eclipse.ui.*;

/**
 * A site which provides access to the context in which this page
 * is being displayed. Instances of this interface serve a similar purpose
 * to <code>IWorkbenchSite</code> instances but is provided as a separate
 * objects to allow clients to access the different site types 
 * (view, editor, dialog) using a common interface. This interface also provides
 * access to the part for the site because this is required by some UI
 * components. Clients should not need to access the part.
 * <p>
 * Clients can determine the type of workbench site by doing <code>instanceof</code>
 * checks on the object returned by <code>getWorkbenchSite</code>. Similar
 * <code>instanceof</code> checks can be done with the part.
 * <p>
 * Clients are not intended to implement this interface
 * 
 * @since 3.0
 */
public interface ISynchronizePageSite {

	/**
	 * @return
	 */
	IWorkbenchSite getWorkbenchSite();
	
	IWorkbenchPart getPart();
	
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
