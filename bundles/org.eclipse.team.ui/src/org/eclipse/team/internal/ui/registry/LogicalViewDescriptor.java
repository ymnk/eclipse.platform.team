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
package org.eclipse.team.internal.ui.registry;

import org.eclipse.core.runtime.*;
import org.eclipse.team.internal.ui.synchronize.views.ILogicalView;
import org.eclipse.team.internal.ui.synchronize.views.LogicalViewProvider;
import org.eclipse.ui.internal.WorkbenchPlugin;

/**
 * 
 * TODO: Should have an icon
 */
public class LogicalViewDescriptor implements ILogicalView {

	public  static final String ATT_ID = "id"; //$NON-NLS-1$
	public  static final String ATT_NAME = "name"; //$NON-NLS-1$
	public  static final String ATT_CLASS = "class"; //$NON-NLS-1$
	
	private IConfigurationElement element;
	private String label;
	private String description;
	private String className;
	private String id;
	private LogicalViewProvider provider;

	public LogicalViewDescriptor(IConfigurationElement element, String descText) throws CoreException {
		this.element = element;
		this.description = descText;
		loadFromExtension();
	}

	/**
	 * load a view descriptor from the registry.
	 */
	private void loadFromExtension() throws CoreException {
		String identifier = element.getAttribute(ATT_ID);
		label = element.getAttribute(ATT_NAME);
		className = element.getAttribute(ATT_CLASS);

		// Sanity check.
		if ((label == null) || (className == null) || (identifier == null)) {
			throw new CoreException(new Status(IStatus.ERROR, element.getDeclaringExtension().getDeclaringPluginDescriptor().getUniqueIdentifier(), 0, "Invalid extension (missing label or class name): " + identifier, //$NON-NLS-1$
					null));
		}
		
		id = identifier;
	}
	
	private LogicalViewProvider createProvider() throws CoreException {
		Object obj = WorkbenchPlugin.createExtension(element, ATT_CLASS);
		return (LogicalViewProvider) obj;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.content.ILogicalView#getId()
	 */
	public String getId() {
		return id;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.content.ILogicalView#getLabel()
	 */
	public String getLabel() {
		return label;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.content.ILogicalView#getDescription()
	 */
	public String getDescription() {
		return description;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.content.ILogicalView#getLogicalViewProvider()
	 */
	public LogicalViewProvider getLogicalViewProvider() throws CoreException {
		if (provider == null) {
			provider = createProvider();
		}
		return provider;
	}
}
