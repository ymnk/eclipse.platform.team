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
package org.eclipse.team.internal.ui.synchronize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.internal.core.subscribers.SyncInfoWorkingSetFilter;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipantDescriptor;
import org.eclipse.team.ui.synchronize.SubscriberParticipant;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;

/**
 * subscriber particpant that supports filtering using scopes.
 */
public abstract class ScopableSubscriberParticipant extends SubscriberParticipant {

	private final static String CTX_ROOT = "root"; //$NON-NLS-1$
	private final static String CTX_ROOT_PATH = "root_resource"; //$NON-NLS-1$
	
	/**
	 * Set of explicit roots (null indicates to use subscriber roots
	 */
	private IResource[] resources;
	
	/**
	 * No arg contructor used to create workspace scope and for
	 * creation of persisted participant after startup
	 */
	public ScopableSubscriberParticipant() {
	}
	
	/**
	 * Create a participant scope to the given resources and their descendants
	 * @param resources the resource scope
	 */
	public ScopableSubscriberParticipant(IResource[] resources) {
		this.resources = resources;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#init(org.eclipse.ui.IMemento)
	 */
	public void init(String secondaryId, IMemento memento) throws PartInitException {
		super.init(secondaryId, memento);
		IMemento[] rootNodes = memento.getChildren(CTX_ROOT);
		if(rootNodes != null) {
			List resources = new ArrayList();
			for (int i = 0; i < rootNodes.length; i++) {
				IMemento rootNode = rootNodes[i];
				IPath path = new Path(rootNode.getString(CTX_ROOT_PATH)); //$NON-NLS-1$
				IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path, true /* include phantoms */);
				if(resource != null) {
					resources.add(resource);
				}
			}
			if(!resources.isEmpty()) {
				this.resources = (IResource[]) resources.toArray(new IResource[resources.size()]);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SubscriberParticipant#saveState(org.eclipse.ui.IMemento)
	 */
	public void saveState(IMemento memento) {
		super.saveState(memento);
		if (resources != null) {
			for (int i = 0; i < resources.length; i++) {
				IResource resource = resources[i];
				IMemento rootNode = memento.createChild(CTX_ROOT);
				rootNode.putString(CTX_ROOT_PATH, resource.getFullPath().toString());
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.subscriber.SubscriberParticipant#setSubscriber(org.eclipse.team.core.subscribers.Subscriber)
	 */
	protected void setSubscriber(Subscriber subscriber) {
		super.setSubscriber(subscriber);
		if (this.resources != null && isSameResources(resources, getSubscriber().roots())) {
			this.resources = null;
		}
		if (resources != null) {
			SyncInfoWorkingSetFilter filter = new SyncInfoWorkingSetFilter();
			filter.setWorkingSet(resources);
			setSyncInfoFilter(filter);
		}
		try {
			ISynchronizeParticipantDescriptor descriptor = getDescriptor();
			setInitializationData(descriptor);
			setSecondaryId(Long.toString(System.currentTimeMillis()));
		} catch (CoreException e) {
			TeamUIPlugin.log(e);
		}
	}
	
	/**
	 * Return the descriptor for this participant
	 * @return the descriptor for this participant
	 */
	protected abstract ISynchronizeParticipantDescriptor getDescriptor();

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SubscriberParticipant#getResources()
	 */
	public IResource[] getResources() {
		if (resources == null) {
			return super.getResources();
		}
		return resources;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.AbstractSynchronizeParticipant#getName()
	 */
	public String getName() {
		String name = super.getName();
		if (resources == null) {
			return name + " (Workspace)";
		} else {
			return name + " " + Utils.convertSelection(resources, 4);
		}
	}
	
	private boolean isSameResources(IResource[] resources2, IResource[] resources3) {
		if (resources2.length != resources3.length) return false;
		List checkList = Arrays.asList(resources2);
		for (int i = 0; i < resources3.length; i++) {
			IResource resource = resources3[i];
			if (!checkList.contains(resource)) {
				return false;
			}
		}
		return true;
	}
}
