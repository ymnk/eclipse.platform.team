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
package org.eclipse.team.core.sync;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.core.Policy;
import org.eclipse.team.internal.core.TeamPlugin;

/**
 * TeamProvider
 */
public class TeamProvider {
	
	static private Map subscribers = new HashMap();

	static public SyncTreeSubscriber getSubscriber(QualifiedName id) throws TeamException {
		// if it doesn't exist than try and instantiate
		SyncTreeSubscriber s = (SyncTreeSubscriber)subscribers.get(id);
		if(s == null) {
			ISyncTreeSubscriberFactory factory = create(id);
			if(factory != null) {
				s = factory.getAdapter(id);
				if(s != null) {
					registerSubscriber(s);
				}
			}
		}
		return s;
	}
	
	static public SyncTreeSubscriber[] getSubscribers() {
		return (SyncTreeSubscriber[])subscribers.values().toArray(
						new SyncTreeSubscriber[subscribers.size()]);
	}
	
	static public void registerSubscriber(SyncTreeSubscriber subscriber) {
		subscribers.put(subscriber.getId(), subscriber);
	}
	
	private static ISyncTreeSubscriberFactory create(QualifiedName id) {
			TeamPlugin plugin = TeamPlugin.getPlugin();
			if (plugin != null) {
				IExtensionPoint extension = plugin.getDescriptor().getExtensionPoint(TeamPlugin.REPOSITORY_EXTENSION);
				if (extension != null) {
					IExtension[] extensions =  extension.getExtensions();
					for (int i = 0; i < extensions.length; i++) {
						IConfigurationElement [] configElements = extensions[i].getConfigurationElements();
						for (int j = 0; j < configElements.length; j++) {
							String extensionId = configElements[j].getAttribute("id"); //$NON-NLS-1$
						
							if (extensionId != null && extensionId.equals(id.getQualifier())) {
								try {
									ISyncTreeSubscriberFactory sFactory = null;
									//Its ok not to have a typeClass extension.  In this case, a default instance will be created.
									if(configElements[j].getAttribute("class") != null) { //$NON-NLS-1$
										sFactory = (ISyncTreeSubscriberFactory) configElements[j].createExecutableExtension("class"); //$NON-NLS-1$
									}
									return sFactory;
								} catch (CoreException e) {
									TeamPlugin.log(e.getStatus());
								} catch (ClassCastException e) {
									String className = configElements[j].getAttribute("class"); //$NON-NLS-1$
									TeamPlugin.log(IStatus.ERROR, Policy.bind("RepositoryProviderType.invalidClass", id.toString(), className), e); //$NON-NLS-1$
								}
								return null;
							}
						}
					}
				}		
			}
			return null;
		}	
}
