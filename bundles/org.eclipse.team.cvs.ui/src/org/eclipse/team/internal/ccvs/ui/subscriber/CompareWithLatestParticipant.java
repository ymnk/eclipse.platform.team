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
package org.eclipse.team.internal.ccvs.ui.subscriber;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.core.subscribers.SyncInfoWorkingSetFilter;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipantDescriptor;

public class CompareWithLatestParticipant extends WorkspaceSynchronizeParticipant {
	public final static String ID = "org.eclipse.team.cvs.ui.comparewithlatest_participant"; //$NON-NLS-1$
	private IResource[] resources;
	
	public CompareWithLatestParticipant(IResource[] resources) {
		this.resources = resources;
		setSubscriber(CVSUIPlugin.getPlugin().getCvsWorkspaceSynchronizeParticipant().getSubscriber());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.subscriber.SubscriberParticipant#setSubscriber(org.eclipse.team.core.subscribers.Subscriber)
	 */
	protected void setSubscriber(Subscriber subscriber) {
		super.setSubscriber(subscriber);
		SyncInfoWorkingSetFilter filter = new SyncInfoWorkingSetFilter();
		filter.setWorkingSet(resources);
		setSyncInfoFilter(filter);
		try {
			ISynchronizeParticipantDescriptor descriptor = TeamUI.getSynchronizeManager().getParticipantDescriptor(ID);
			setInitializationData(descriptor);
			setSecondaryId(Long.toString(System.currentTimeMillis()));
		} catch (CoreException e) {
			CVSUIPlugin.log(e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.AbstractSynchronizeParticipant#getName()
	 */
	public String getName() {
		return "CVS " + Utils.convertSelection(resources, 4);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SubscriberParticipant#getResources()
	 */
	public IResource[] getResources() {
		return resources;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.subscribers.SubscriberParticipant#initializeConfiguration(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration)
	 */
	protected void initializeConfiguration(ISynchronizePageConfiguration configuration) {
		super.initializeConfiguration(configuration);
		configuration.setMenuGroups(ISynchronizePageConfiguration.P_VIEW_MENU, new String[] {
				ISynchronizePageConfiguration.MODE_GROUP,  
				ISynchronizePageConfiguration.LAYOUT_GROUP, 
				ISynchronizePageConfiguration.PREFERENCES_GROUP});
		configuration.addMenuGroup(ISynchronizePageConfiguration.P_TOOLBAR_MENU, ISynchronizePageConfiguration.REMOVE_PARTICPANT_GROUP);
	}
}
