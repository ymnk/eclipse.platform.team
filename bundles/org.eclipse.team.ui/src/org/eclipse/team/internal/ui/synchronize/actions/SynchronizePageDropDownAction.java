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
package org.eclipse.team.internal.ui.synchronize.actions;

import java.util.ArrayList;

import org.eclipse.jface.action.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.texteditor.IUpdate;

public class SynchronizePageDropDownAction extends Action implements IMenuCreator, ISynchronizeParticipantListener, IUpdate {

		private ISynchronizeView fView;
		private Menu fMenu;
	
		/* (non-Javadoc)
		 * @see org.eclipse.ui.texteditor.IUpdate#update()
		 */
		public void update() {
			ISynchronizeParticipantReference[] refs = TeamUI.getSynchronizeManager().getSynchronizeParticipants();
			setEnabled(refs.length >= 1);
		}
		
		protected ISynchronizeParticipantReference[] getParticipants(boolean staticOnly) {
			ArrayList participants = new ArrayList();
		ISynchronizeParticipantReference[] refs = TeamUI.getSynchronizeManager().getSynchronizeParticipants();
		for (int i = 0; i < refs.length; i++) {
			ISynchronizeParticipantReference ref = refs[i];
			if (ref.getDescriptor().isStatic() == staticOnly) {
				participants.add(ref);
			}
		}
		return (ISynchronizeParticipantReference[]) participants.toArray(new ISynchronizeParticipantReference[participants.size()]);
		}
		
		protected boolean select(ISynchronizeParticipantReference ref) {
			return true;
		}

		public SynchronizePageDropDownAction(ISynchronizeView view) {
			fView= view;
			Utils.initAction(this, "action.refreshSubscriber."); //$NON-NLS-1$
			setMenuCreator(this);		
			update();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.action.IMenuCreator#dispose()
		 */
		public void dispose() {
			if (fMenu != null) {
				fMenu.dispose();
				fMenu = null;
			}
			TeamUI.getSynchronizeManager().removeSynchronizeParticipantListener(this);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.action.IMenuCreator#getMenu(org.eclipse.swt.widgets.Menu)
		 */
		public Menu getMenu(Menu parent) {
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.action.IMenuCreator#getMenu(org.eclipse.swt.widgets.Control)
		 */
		public Menu getMenu(Control parent) {
			if (fMenu != null) {
				fMenu.dispose();
			}		
			fMenu= new Menu(parent);
			final ISynchronizeParticipantReference[] staticParticipants = getParticipants(true /*static */);
			final ISynchronizeParticipantReference[] dynamicParticipants = getParticipants(false /*dynamic */);
			addParticipantsToMenu(fMenu, staticParticipants);
			if(dynamicParticipants.length > 0) addMenuSeparator();
			addParticipantsToMenu(fMenu, dynamicParticipants);
			if(dynamicParticipants.length > 0) {
			addMenuSeparator();
			addActionToMenu(fMenu, new Action("Clear Participants") {
				public void run() {
					for (int i = 0; i < dynamicParticipants.length; i++) {
						ISynchronizeParticipantReference reference = dynamicParticipants[i];
						try {
							TeamUI.getSynchronizeManager().removeSynchronizeParticipants(new ISynchronizeParticipant[] {reference.getParticipant()});
						} catch (TeamException e) {
							continue;
						}
					}
				}});
			}
			TeamUI.getSynchronizeManager().addSynchronizeParticipantListener(this);	
			return fMenu;
		}
	
		protected void addParticipantsToMenu(Menu parent, ISynchronizeParticipantReference[] refs) {
			ISynchronizeParticipant current = fView.getParticipant();
			for (int i = 0; i < refs.length; i++) {
				ISynchronizeParticipantReference page = refs[i];
				Action action = new ShowSynchronizeParticipantAction(fView, page);  
				try {
					action.setChecked(page.getParticipant().equals(current));
				} catch (TeamException e) {
					continue;
				}
				addActionToMenu(fMenu, action);
			}
		}
		
		protected void addActionToMenu(Menu parent, Action action) {
			ActionContributionItem item= new ActionContributionItem(action);
			item.fill(parent, -1);
		}

		protected void addMenuSeparator() {
			new MenuItem(fMenu, SWT.SEPARATOR);		
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.action.IAction#run()
		 */
		public void run() {
			// do nothing - this is a menu
		}
	
		/* (non-Javadoc)
		 * @see org.eclipse.team.ui.sync.ISynchronizeParticipantListener#participantsAdded(org.eclipse.team.ui.sync.ISynchronizeParticipant[])
		 */
		public void participantsAdded(ISynchronizeParticipant[] consoles) {
			Display display = TeamUIPlugin.getStandardDisplay();
			display.asyncExec(new Runnable() {
				public void run() {
					update();
				}
			});
		}

		/* (non-Javadoc)
		 * @see org.eclipse.team.ui.sync.ISynchronizeParticipantListener#participantsRemoved(org.eclipse.team.ui.sync.ISynchronizeParticipant[])
		 */
		public void participantsRemoved(ISynchronizeParticipant[] consoles) {
			Display display = TeamUIPlugin.getStandardDisplay();
			display.asyncExec(new Runnable() {
				public void run() {
					if (fMenu != null) {
						fMenu.dispose();
					}
					update();
				}
			});
		}
}