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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.synchronize.views.ILogicalView;

public class LogicalViewRegistry extends RegistryReader {

	private static final String TAG_LOGICAL_VIEW = "logicalView";
	private Map views = new HashMap();

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.registry.RegistryReader#readElement(org.eclipse.core.runtime.IConfigurationElement)
	 */
	protected boolean readElement(IConfigurationElement element) {
		if (element.getName().equals(TAG_LOGICAL_VIEW)) {
			String descText = getDescription(element);
			LogicalViewDescriptor desc;
			try {
				desc = new LogicalViewDescriptor(element, descText);
				views.put(desc.getId(), desc);
			} catch (CoreException e) {
				TeamUIPlugin.log(e);
			}
			return true;
		}
		return false;
	}
	
	public ILogicalView[] getLogicalViews() {
		return (ILogicalView[])views.values().toArray(new ILogicalView[views.size()]);
	}
	
	public ILogicalView getLogicalView(String id) {
		return (ILogicalView)views.get(id);
	}

}
