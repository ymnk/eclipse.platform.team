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
package org.eclipse.team.internal.ui.sync.pages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.util.ListenerList;
import org.eclipse.team.core.ISaveContext;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.core.SaveContextXMLWriter;
import org.eclipse.team.internal.ui.IPreferenceIds;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.registry.SynchronizeParticipantRegistry;
import org.eclipse.team.internal.ui.registry.SynchronizePartnerDescriptor;
import org.eclipse.team.ui.ITeamUIConstants;
import org.eclipse.team.ui.sync.ISynchronizeManager;
import org.eclipse.team.ui.sync.ISynchronizeParticipant;
import org.eclipse.team.ui.sync.ISynchronizeParticipantListener;
import org.eclipse.team.ui.sync.ISynchronizeView;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.WorkbenchException;

public class SynchronizeManager implements ISynchronizeManager {
	/**
	 * Synchronize View page listeners
	 */
	private ListenerList fListeners = null;
	
	/**
	 * List of registered synchronize view pages
	 */
	private Map synchronizeParticipants = new HashMap(10); 
	private SynchronizeParticipantRegistry participantRegistry = new SynchronizeParticipantRegistry();
	
	// change notification constants
	private final static int ADDED = 1;
	private final static int REMOVED = 2;
	
	// save context constants
	private final static String CTX_PARTICIPANTS = "syncparticipants";
	private final static String CTX_PARTICIPANT = "participant";
	private final static String CTX_ID = "id";
	
	/**
	 * Notifies a participant listeners of additions or removals
	 */
	class SynchronizeViewPageNotifier implements ISafeRunnable {
		
		private ISynchronizeParticipantListener fListener;
		private int fType;
		private ISynchronizeParticipant[] fChanged;
		
		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.ISafeRunnable#handleException(java.lang.Throwable)
		 */
		public void handleException(Throwable exception) {
			TeamUIPlugin.log(IStatus.ERROR, "", exception);
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.ISafeRunnable#run()
		 */
		public void run() throws Exception {
			switch (fType) {
				case ADDED:
					fListener.participantsAdded(fChanged);
					break;
				case REMOVED:
					fListener.participantsRemoved(fChanged);
					break;
			}
		}
		
		/**
		 * Notifies the given listener of the adds/removes
		 * 
		 * @param participants the participants that changed
		 * @param update the type of change
		 */
		public void notify(ISynchronizeParticipant[] participants, int update) {
			if (fListeners == null) {
				return;
			}
			fChanged = participants;
			fType = update;
			Object[] copiedListeners= fListeners.getListeners();
			for (int i= 0; i < copiedListeners.length; i++) {
				fListener = (ISynchronizeParticipantListener)copiedListeners[i];
				Platform.run(this);
			}	
			fChanged = null;
			fListener = null;			
		}
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.ISynchronizeManager#addSynchronizeParticipantListener(org.eclipse.team.ui.sync.ISynchronizeParticipantListener)
	 */
	public void addSynchronizeParticipantListener(ISynchronizeParticipantListener listener) {
		if (fListeners == null) {
			fListeners = new ListenerList(5);
		}
		fListeners.add(listener);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.ISynchronizeManager#removeSynchronizeParticipantListener(org.eclipse.team.ui.sync.ISynchronizeParticipantListener)
	 */
	public void removeSynchronizeParticipantListener(ISynchronizeParticipantListener listener) {
		if (fListeners != null) {
			fListeners.remove(listener);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.ISynchronizeManager#addSynchronizeParticipants(org.eclipse.team.ui.sync.ISynchronizeParticipant[])
	 */
	public synchronized void addSynchronizeParticipants(ISynchronizeParticipant[] participants) {
		List added = new ArrayList(participants.length);
		for (int i = 0; i < participants.length; i++) {
			ISynchronizeParticipant participant = participants[i];
			if (!synchronizeParticipants.containsValue(participant)) {
				synchronizeParticipants.put(participant.getId(), participant);
				added.add(participant);
			}
		}
		if (!added.isEmpty()) {
			fireUpdate((ISynchronizeParticipant[])added.toArray(new ISynchronizeParticipant[added.size()]), ADDED);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.ISynchronizeManager#removeSynchronizeParticipants(org.eclipse.team.ui.sync.ISynchronizeParticipant[])
	 */
	public synchronized void removeSynchronizeParticipants(ISynchronizeParticipant[] participants) {
		List removed = new ArrayList(participants.length);
		for (int i = 0; i < participants.length; i++) {
			ISynchronizeParticipant participant = participants[i];
			if (synchronizeParticipants.remove(participant.getId()) != null) {
				removed.add(participant);
			}
		}
		if (!removed.isEmpty()) {
			fireUpdate((ISynchronizeParticipant[])removed.toArray(new ISynchronizeParticipant[removed.size()]), REMOVED);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.ISynchronizeManager#getSynchronizeParticipants()
	 */
	public synchronized ISynchronizeParticipant[] getSynchronizeParticipants() {
		return (ISynchronizeParticipant[])synchronizeParticipants.values().toArray(new ISynchronizeParticipant[synchronizeParticipants.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.ISynchronizeManager#find(java.lang.String)
	 */
	public ISynchronizeParticipant find(String id) {
		return (ISynchronizeParticipant)synchronizeParticipants.get(id);
	}
	
	/**
	 * Fires notification.
	 * 
	 * @param participants participants added/removed
	 * @param type ADD or REMOVE
	 */
	private void fireUpdate(ISynchronizeParticipant[] participants, int type) {
		new SynchronizeViewPageNotifier().notify(participants, type);
	}
	
	public ISynchronizeView showSynchronizeViewInActivePage(IWorkbenchPage activePage) {
		IWorkbench workbench= TeamUIPlugin.getPlugin().getWorkbench();
		IWorkbenchWindow window= workbench.getActiveWorkbenchWindow();
		
		if(! TeamUIPlugin.getPlugin().getPreferenceStore().getString(IPreferenceIds.SYNCVIEW_DEFAULT_PERSPECTIVE).equals(IPreferenceIds.SYNCVIEW_DEFAULT_PERSPECTIVE_NONE)) {			
			try {
				String pId = TeamUIPlugin.getPlugin().getPreferenceStore().getString(IPreferenceIds.SYNCVIEW_DEFAULT_PERSPECTIVE);
				activePage = workbench.showPerspective(pId, window);
			} catch (WorkbenchException e) {
				Utils.handleError(window.getShell(), e, Policy.bind("SynchronizeView.14"), e.getMessage()); //$NON-NLS-1$
			}
		}
		try {
			if (activePage == null) {
				activePage = TeamUIPlugin.getActivePage();
				if (activePage == null) return null;
			}
			return (ISynchronizeView)activePage.showView(ISynchronizeView.VIEW_ID);
		} catch (PartInitException pe) {
			Utils.handleError(window.getShell(), pe, Policy.bind("SynchronizeView.16"), pe.getMessage()); //$NON-NLS-1$
			return null;
		}
	}
	
	public void restoreParticipants() throws PartInitException, TeamException, CoreException {
		participantRegistry.readRegistry(Platform.getPluginRegistry(), TeamUIPlugin.ID, ITeamUIConstants.PT_SYNCPARTICIPANTS);
		boolean firstTime = restoreSynchronizeParticipants();
		if(!firstTime) {
			SynchronizePartnerDescriptor[] desc = participantRegistry.getSynchronizeParticipants();
			for (int i = 0; i < desc.length; i++) {
				SynchronizePartnerDescriptor descriptor = desc[i];
				if(descriptor.isStatic()) {
					createParticipant(null, null, descriptor);
				}
			}
		}
	}
	
	public void dispose() {
	}
	
	private boolean restoreSynchronizeParticipants() throws TeamException, PartInitException, CoreException {
		ISaveContext root = SaveContextXMLWriter.readXMLPluginMetaFile(TeamUIPlugin.getPlugin(), "subscribers"); //$NON-NLS-1$
		if(root != null && root.getName().equals(CTX_PARTICIPANTS)) {
			ISaveContext[] contexts = root.getChildren();
			for (int i = 0; i < contexts.length; i++) {
				ISaveContext context = contexts[i];
				if(context.getName().equals(CTX_PARTICIPANT)) {
					String id = context.getAttribute(CTX_ID);
					ISaveContext[] children = context.getChildren();
					SynchronizePartnerDescriptor desc = participantRegistry.find(id);
					if(desc != null) {
						IConfigurationElement cfgElement = desc.getConfigurationElement();
						createParticipant(id, children, desc);
					}
				}
			}
			return true;
		}
		return false;
	}

	private void createParticipant(String id, ISaveContext[] children, SynchronizePartnerDescriptor desc) throws CoreException, PartInitException {
		ISynchronizeParticipant participant = (ISynchronizeParticipant)TeamUIPlugin.createExtension(desc.getConfigurationElement(), SynchronizePartnerDescriptor.ATT_CLASS);
		participant.setInitializationData(desc.getConfigurationElement(), id, null);
		if(children != null) {
			participant.init(children[0]);
		} else {
			participant.init(null);
		}			
		addSynchronizeParticipants(new ISynchronizeParticipant[] {participant});
	}
}