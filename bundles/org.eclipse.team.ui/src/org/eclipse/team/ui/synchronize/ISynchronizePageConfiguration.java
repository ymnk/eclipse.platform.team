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
package org.eclipse.team.ui.synchronize;

import org.eclipse.jface.util.IPropertyChangeListener;

public interface ISynchronizePageConfiguration {

	public abstract ISynchronizeParticipant getParticipant();
	
	public abstract ISynchronizePageSite getSite();

	public abstract void addPropertyChangeListener(IPropertyChangeListener listener);

	public abstract void removePropertyChangeListener(IPropertyChangeListener listener);

	/**
	 * Sets the property with the given name.
	 * If the new value differs from the old a <code>PropertyChangeEvent</code>
	 * is sent to registered listeners.
	 *
	 * @param propertyName the name of the property to set
	 * @param value the new value of the property
	 */
	public abstract void setProperty(String key, Object newValue);

	/**
	 * Returns the property with the given name, or <code>null</code>
	 * if no such property exists.
	 *
	 * @param propertyName the name of the property to retrieve
	 * @return the property with the given name, or <code>null</code> if not found
	 */
	public abstract Object getProperty(String key);

	/**
	 * Register the action contribution with the configuration. The
	 * registered action contributions will have the opertunity to add
	 * actions to the action bars and context menu of the synchronize
	 * page created using the configuration.
	 * @param contribution an action contribution
	 */
	public abstract void addActionContribution(IActionContribution contribution);
}