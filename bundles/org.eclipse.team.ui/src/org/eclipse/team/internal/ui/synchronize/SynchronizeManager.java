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
package org.eclipse.team.internal.ui.synchronize;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.util.ListenerList;
import org.eclipse.team.core.ISaveContext;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.core.*;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.registry.SynchronizeParticipantRegistry;
import org.eclipse.team.internal.ui.registry.SynchronizePartnerDescriptor;
import org.eclipse.team.ui.ITeamUIConstants;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.*;

/**
 * Manages the registered synchronize participants. It handles notification
 * of participant lifecycles, creation of <code>static</code> participants, 
 * and the re-creation of persisted participants.
 * 
 * @since 3.0
 */
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
	private final static String CTX_PARTICIPANTS = "syncparticipants"; //$NON-NLS-1$
	private final static String CTX_PARTICIPANT = "participant"; //$NON-NLS-1$
	private final static String CTX_QUALIFIED_NAME = "qualified_name"; //$NON-NLS-1$
	private final static String CTX_LOCAL_NAME = "local_name"; //$NON-NLS-1$
	private final static String FILENAME = "syncParticipants.xml"; //$NON-NLS-1$
	
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
			TeamUIPlugin.log(IStatus.ERROR, Policy.bind("SynchronizeManager.7"), exception); //$NON-NLS-1$
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
	
	public SynchronizeManager() {
		super();
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
			saveState();
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
			saveState();
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
	public ISynchronizeParticipant find(QualifiedName id) {
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
	
	/**
	 * Called to display the synchronize view in the given page. If the given
	 * page is <code>null</code> the synchronize view is shown in the default
	 * active workbench window.
	 */
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
	
	/**
	 *  
	 */
	public void restoreParticipants() throws PartInitException, TeamException, CoreException {
		participantRegistry.readRegistry(Platform.getPluginRegistry(), TeamUIPlugin.ID, ITeamUIConstants.PT_SYNCPARTICIPANTS);
		restoreSynchronizeParticipants();
		// Create any new participants that had previously not been added (e.g. new plugins being
		// added.
		SynchronizePartnerDescriptor[] desc = participantRegistry.getSynchronizeParticipants();
		List participants = new ArrayList();
		for (int i = 0; i < desc.length; i++) {
			SynchronizePartnerDescriptor descriptor = desc[i];
			if(descriptor.isStatic() && ! synchronizeParticipants.containsKey(descriptor.getId())) {
				participants.add(createParticipant(descriptor.getId(), descriptor));
			}
		}
		if(! participants.isEmpty()) {
			addSynchronizeParticipants((ISynchronizeParticipant[]) participants.toArray(new ISynchronizeParticipant[participants.size()]));
		}
	}
	
	private void restoreSynchronizeParticipants() throws TeamException, PartInitException, CoreException {
		ISaveContext root = SaveContextXMLWriter.readXMLPluginMetaFile(TeamUIPlugin.getPlugin(), FILENAME); //$NON-NLS-1$
		if(root != null && root.getName().equals(CTX_PARTICIPANTS)) {
			List participants = new ArrayList();
			ISaveContext[] contexts = root.getChildren();
			for (int i = 0; i < contexts.length; i++) {
				ISaveContext context = contexts[i];
				if(context.getName().equals(CTX_PARTICIPANT)) {
					String qualified_name = context.getAttribute(CTX_QUALIFIED_NAME);
					String local_name = context.getAttribute(CTX_LOCAL_NAME);
					SynchronizePartnerDescriptor desc = participantRegistry.find(qualified_name);
					if(desc != null) {
						IConfigurationElement cfgElement = desc.getConfigurationElement();
						participants.add(createParticipant(new QualifiedName(qualified_name, local_name), desc));
					}
				}
			}
			if(! participants.isEmpty()) {
				addSynchronizeParticipants((ISynchronizeParticipant[]) participants.toArray(new ISynchronizeParticipant[participants.size()]));
			}
		}
	}
	
	private ISynchronizeParticipant createParticipant(QualifiedName id, SynchronizePartnerDescriptor desc) throws CoreException, PartInitException {
		ISynchronizeParticipant participant = (ISynchronizeParticipant)TeamUIPlugin.createExtension(desc.getConfigurationElement(), SynchronizePartnerDescriptor.ATT_CLASS);
		participant.setInitializationData(desc.getConfigurationElement(), id.toString(), id);
		participant.init(id);
		return participant;
	}
	
	private void saveState() {
		ISaveContext root = new SaveContext();
		root.setName(CTX_PARTICIPANTS);
		List children = new ArrayList();
		try {
			for (Iterator it = synchronizeParticipants.values().iterator(); it.hasNext();) {			
				ISynchronizeParticipant participant = (ISynchronizeParticipant) it.next();			
				QualifiedName id = participant.getId();
				ISaveContext item = new SaveContext();				
				item.setName(CTX_PARTICIPANT);
				Map attributes = new HashMap();
				attributes.put(CTX_QUALIFIED_NAME, id.getQualifier());
				attributes.put(CTX_LOCAL_NAME, id.getLocalName());
				item.setAttributes(attributes);				
				children.add(item);
				participant.saveState();
			}
			root.setChildren((SaveContext[])children.toArray(new SaveContext[children.size()]));
			SaveContextXMLWriter.writeXMLPluginMetaFile(TeamUIPlugin.getPlugin(), FILENAME, (SaveContext)root); //$NON-NLS-1$
		} catch (TeamException e) {
			TeamPlugin.log(e);
		}
	}
}