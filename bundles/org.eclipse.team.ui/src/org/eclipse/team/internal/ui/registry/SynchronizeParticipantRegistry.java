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

public class SynchronizeParticipantRegistry extends RegistryReader {

	private static final String TAG_SYNCPARTICIPANT = "participant";
	private Map participants = new HashMap();
	
	public SynchronizeParticipantRegistry() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.registry.RegistryReader#readElement(org.eclipse.core.runtime.IConfigurationElement)
	 */
	protected boolean readElement(IConfigurationElement element) {
		if (element.getName().equals(TAG_SYNCPARTICIPANT)) {
			String descText = getDescription(element);
			SynchronizePartnerDescriptor desc;
			try {
				desc = new SynchronizePartnerDescriptor(element, descText);
				participants.put(desc.getId(), desc);
			} catch (CoreException e) {
				TeamUIPlugin.log(e);
			}
			return true;
		}
		return false;
	}
	
	public SynchronizePartnerDescriptor[] getSynchronizeParticipants() {
		return (SynchronizePartnerDescriptor[])participants.values().toArray(new SynchronizePartnerDescriptor[participants.size()]);
	}
	
	public SynchronizePartnerDescriptor find(String id) {
		return (SynchronizePartnerDescriptor)participants.get(id);
	}
}
