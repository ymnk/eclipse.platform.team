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
package org.eclipse.team.ui.sync;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * A synchronization target provides synchronization between local resources and a 
 * remote location that is used to share those resources. The Synchronize View 
 * displays synchronize targets.
 * 
 * @since 3.0
 */
public interface ISynchronizeViewPage {
	/**
	 * Returns the name of this synchronize target.
	 * 
	 * @return the name of this synchronize target
	 */
	public String getName();
	
	/**
	 * Returns an image descriptor for this synchronize target, or <code>null</code>
	 * if none.
	 * 
	 * @return an image descriptor for this synchronize target, or <code>null</code>
	 *  if none
	 */
	public ImageDescriptor getImageDescriptor();
	
	/**
	 * Creates and returns a new page for this synchronize target. The page is displayed
	 * for this synchronize target in the given synchronize view.
	 * 
	 * @param view the view in which the page is to be created
	 * @return a page book view page representation of this synchronize target
	 */
	public IPageBookViewPage createPage(INewSynchronizeView view);
	
	/**
	 * Adds a listener for changes to properties of this synchronize target.
	 * Has no effect if an identical listener is already registered.
	 * <p>
	 * The changes supported by the synchronize view are as follows:
	 * <ul>
	 *   <li><code>IBasicPropertyConstants.P_TEXT</code> - indicates the name
	 *      of a synchronize target has changed</li>
	 * 	 <li><code>IBasicPropertyConstants.P_IMAGE</code> - indicates the image
	 *      of a synchronize target has changed</li>
	 * </ul>
	 * </p>
	 * <p>
	 * Clients may define additional properties as required.
	 * </p>
	 *
	 * @param listener a property change listener
	 */
	public void addPropertyChangeListener(IPropertyChangeListener listener);
	
	/**
	 * Removes the given property listener from this synchronize target.
	 * Has no effect if an identical listener is not alread registered.
	 * 
	 * @param listener a property listener
	 */
	public void removePropertyChangeListener(IPropertyChangeListener listener);	
}