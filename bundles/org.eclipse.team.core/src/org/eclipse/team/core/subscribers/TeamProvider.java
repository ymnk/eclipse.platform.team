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
package org.eclipse.team.core.subscribers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
import org.eclipse.team.internal.core.VestigeConfigurationItem;
import org.eclipse.team.internal.core.VestigeXMLWriter;

/**
 * TeamProvider
 */
public abstract class TeamProvider {
	
	static private Map subscribers = new HashMap();
	static private List listeners = new ArrayList(1);

	static public SyncTreeSubscriber getSubscriber(QualifiedName id) throws TeamException {
		// if it doesn't exist than try and instantiate
		SyncTreeSubscriber s = (SyncTreeSubscriber)subscribers.get(id);
		if(s == null) {
			ISyncTreeSubscriberFactory factory = create(id);
			if(factory != null) {
				s = factory.createSubscriber(id);
				if(s != null) {
					registerSubscriber(s);
				}
			}
		}
		return s;
	}
			
	static public void shutdown() {
		VestigeConfigurationItem root = new VestigeConfigurationItem();
		root.setName("subscribers");
		List children = new ArrayList();
		
		for (Iterator it = subscribers.values().iterator(); it.hasNext();) {
			SyncTreeSubscriber subscriber = (SyncTreeSubscriber) it.next();
			
			VestigeConfigurationItem child = new VestigeConfigurationItem();
			subscriber.saveState(child);
			if(child.getName() != null) { 
				VestigeConfigurationItem item = new VestigeConfigurationItem();
				
				item.setChildren(new VestigeConfigurationItem[] {child});
				item.setName("subscriber");
				Map attributes = new HashMap();
				attributes.put("qualifier", subscriber.getId().getQualifier());
				attributes.put("localname", subscriber.getId().getLocalName());
				item.setAttributes(attributes);
				
				children.add(item);
			}
		}
		root.setChildren((VestigeConfigurationItem[])children.toArray(new VestigeConfigurationItem[children.size()]));
		try {
			VestigeXMLWriter.writeXMLPluginMetaFile(TeamPlugin.getPlugin(), "subscribers", root);
		} catch (TeamException e) {
			TeamPlugin.log(e.getStatus());
		}
	}
	
	static public SyncTreeSubscriber[] getSubscribers() {
		return (SyncTreeSubscriber[])subscribers.values().toArray(
						new SyncTreeSubscriber[subscribers.size()]);
	}
	
	static public void registerSubscriber(SyncTreeSubscriber subscriber) {
		subscribers.put(subscriber.getId(), subscriber);
		fireTeamResourceChange(new TeamDelta[] {
				new TeamDelta(subscriber, TeamDelta.SUBSCRIBER_CREATED, null)});
	}
	
	/* (non-Javadoc)
	 * Method declared on IBaseLabelProvider.
	 */
	static public void addListener(ITeamResourceChangeListener listener) {
		listeners.add(listener);
	}

	/* (non-Javadoc)
	 * Method declared on IBaseLabelProvider.
	 */
	static public void removeListener(ITeamResourceChangeListener listener) {
		listeners.remove(listener);
	}
	
	/*
	 * Fires a team resource change event to all registered listeners
	 * Only listeners registered at the time this method is called are notified.
	 */
	static protected void fireTeamResourceChange(final TeamDelta[] deltas) {
		for (Iterator it = listeners.iterator(); it.hasNext();) {
			final ITeamResourceChangeListener l = (ITeamResourceChangeListener) it.next();
			l.teamResourceChanged(deltas);	
		}
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
