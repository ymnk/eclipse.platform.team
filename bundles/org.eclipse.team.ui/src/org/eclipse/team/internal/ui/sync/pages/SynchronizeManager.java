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
import java.util.List;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.util.ListenerList;
import org.eclipse.team.internal.ui.IPreferenceIds;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
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
	private List synchronizeParticipants = new ArrayList(10); 
	
	// change notification constants
	private final static int ADDED = 1;
	private final static int REMOVED = 2;
	
	/**
	 * Notifies a console listener of additions or removals
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
		 * @param consoles the consoles that changed
		 * @param update the type of change
		 */
		public void notify(ISynchronizeParticipant[] consoles, int update) {
			if (fListeners == null) {
				return;
			}
			fChanged = consoles;
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
	 * @see org.eclipse.ui.console.IConsoleManager#addConsoleListener(org.eclipse.ui.console.IConsoleListener)
	 */
	public void addSynchronizeParticipantListener(ISynchronizeParticipantListener listener) {
		if (fListeners == null) {
			fListeners = new ListenerList(5);
		}
		fListeners.add(listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.IConsoleManager#removeConsoleListener(org.eclipse.ui.console.IConsoleListener)
	 */
	public void removeSynchronizeParticipantListener(ISynchronizeParticipantListener listener) {
		if (fListeners != null) {
			fListeners.remove(listener);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.IConsoleManager#addConsoles(org.eclipse.ui.console.IConsole[])
	 */
	public synchronized void addSynchronizeParticipants(ISynchronizeParticipant[] consoles) {
		List added = new ArrayList(consoles.length);
		for (int i = 0; i < consoles.length; i++) {
			ISynchronizeParticipant console = consoles[i];
			if (!synchronizeParticipants.contains(console)) {
				synchronizeParticipants.add(console);
				added.add(console);
			}
		}
		if (!added.isEmpty()) {
			fireUpdate((ISynchronizeParticipant[])added.toArray(new ISynchronizeParticipant[added.size()]), ADDED);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.IConsoleManager#removeConsoles(org.eclipse.ui.console.IConsole[])
	 */
	public synchronized void removeSynchronizeParticipants(ISynchronizeParticipant[] consoles) {
		List removed = new ArrayList(consoles.length);
		for (int i = 0; i < consoles.length; i++) {
			ISynchronizeParticipant console = consoles[i];
			if (synchronizeParticipants.remove(console)) {
				removed.add(console);
			}
		}
		if (!removed.isEmpty()) {
			fireUpdate((ISynchronizeParticipant[])removed.toArray(new ISynchronizeParticipant[removed.size()]), REMOVED);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.IConsoleManager#getConsoles()
	 */
	public synchronized ISynchronizeParticipant[] getSynchronizeParticipants() {
		return (ISynchronizeParticipant[])synchronizeParticipants.toArray(new ISynchronizeParticipant[synchronizeParticipants.size()]);
	}

	/**
	 * Fires notification.
	 * 
	 * @param consoles consoles added/removed
	 * @param type ADD or REMOVE
	 */
	private void fireUpdate(ISynchronizeParticipant[] consoles, int type) {
		new SynchronizeViewPageNotifier().notify(consoles, type);
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
}
