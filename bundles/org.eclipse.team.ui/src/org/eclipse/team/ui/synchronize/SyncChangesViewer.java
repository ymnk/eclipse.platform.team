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
package org.eclipse.team.ui.synchronize;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.team.internal.ui.synchronize.sets.SyncSet;
import org.eclipse.team.ui.synchronize.actions.INavigableControl;
import org.eclipse.ui.internal.PluginAction;

public abstract class SyncChangesViewer implements INavigableControl {

	private TeamSubscriberParticipant participant;
	private SyncSet set;
	
	public SyncChangesViewer(TeamSubscriberParticipant participant, SyncSet set) {
		this.set = set;
		this.participant = participant;		
	}
	
	public SyncSet getSyncSet() {
		return set;
	}
	
	public abstract StructuredViewer getViewer();
	
	public abstract void dispose();
	
	protected void hookContextMenu() {
		if(getViewer() != null) {
			final MenuManager menuMgr = new MenuManager(participant.getId()); //$NON-NLS-1$
			menuMgr.setRemoveAllWhenShown(true);
			menuMgr.addMenuListener(new IMenuListener() {
				public void menuAboutToShow(IMenuManager manager) {
					fillContextMenu(manager);
				}
			});
			Menu menu = menuMgr.createContextMenu(getViewer().getControl());
			menu.addMenuListener(new MenuListener() {
				public void menuHidden(MenuEvent e) {
				}
				// Hack to allow action contributions to update their
				// state before the menu is shown. This is required when
				// the state of the selection changes and the contributions
				// need to update enablement based on this. 
				public void menuShown(MenuEvent e) {
					IContributionItem[] items = menuMgr.getItems();
					for (int i = 0; i < items.length; i++) {
						IContributionItem item = items[i];
						if(item instanceof ActionContributionItem) {
							IAction actionItem = ((ActionContributionItem)item).getAction();
							if(actionItem instanceof PluginAction) {
								((PluginAction)actionItem).selectionChanged(getViewer().getSelection());
							}
						}
					}
				}
			});
			getViewer().getControl().setMenu(menu);			
			//view.getSite().registerContextMenu(participant.getId(), menuMgr, getViewer());
		}
	}

	protected abstract void fillContextMenu(IMenuManager manager);
}