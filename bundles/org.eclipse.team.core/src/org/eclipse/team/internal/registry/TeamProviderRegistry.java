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
package org.eclipse.team.internal.registry;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.team.internal.core.TeamPlugin;

public class TeamProviderRegistry extends RegistryReader {
	
	private Map participants = new HashMap();
	private String extensionId;
	private String descriptorClassName;
	
	public TeamProviderRegistry(String extensionId) {
		super();
		this.extensionId = extensionId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.registry.RegistryReader#readElement(org.eclipse.core.runtime.IConfigurationElement)
	 */
	protected boolean readElement(IConfigurationElement element) {
		if (element.getName().equals(extensionId)) {
			String descText = getDescription(element);
			TeamProviderDescriptor desc;
			try {
				desc = new TeamProviderDescriptor(element, descText);
				participants.put(desc.getId(), desc);
			} catch (CoreException e) {
				TeamPlugin.log(e);
			}
			return true;
		}
		return false;
	}
	
	public TeamProviderDescriptor[] getTeamProviderDescriptors() {
		return (TeamProviderDescriptor[])participants.values().toArray(new TeamProviderDescriptor[participants.size()]);
	}
	
	public TeamProviderDescriptor find(String id) {
		return (TeamProviderDescriptor)participants.get(id);
	}
}
