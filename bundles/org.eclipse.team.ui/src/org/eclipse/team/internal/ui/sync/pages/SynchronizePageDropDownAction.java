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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.sync.INewSynchronizeView;
import org.eclipse.team.ui.sync.ISynchronizePageListener;
import org.eclipse.team.ui.sync.ISynchronizeViewPage;
import org.eclipse.ui.IKeyBindingService;
import org.eclipse.ui.texteditor.IUpdate;

public class SynchronizePageDropDownAction extends Action implements IMenuCreator, ISynchronizePageListener, IUpdate {

		private INewSynchronizeView fView;
		private Menu fMenu;
	
		/* (non-Javadoc)
		 * @see org.eclipse.ui.texteditor.IUpdate#update()
		 */
		public void update() {
			ISynchronizeViewPage[] pages = TeamUI.getSynchronizeManager().getSynchronizePages();
			setEnabled(pages.length >= 1);
		}

		public SynchronizePageDropDownAction(INewSynchronizeView view) {
			fView= view;
			Utils.initAction(this, "action.refreshSubscriber."); //$NON-NLS-1$
			IKeyBindingService kbs = view.getSite().getKeyBindingService();
			Utils.registerAction(kbs, this, "org.eclipse.team.ui.syncview.syncAll"); //$NON-NLS-1$
			setMenuCreator(this);
			TeamUI.getSynchronizeManager().addSynchronizePageListener(this);
			update();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.action.IMenuCreator#dispose()
		 */
		public void dispose() {
			if (fMenu != null) {
				fMenu.dispose();
			}
		
			fView= null;
			TeamUI.getSynchronizeManager().removeSynchronizePageListener(this);
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
			ISynchronizeViewPage[] pages = TeamUI.getSynchronizeManager().getSynchronizePages();
			ISynchronizeViewPage current = fView.getActivePage();
			for (int i = 0; i < pages.length; i++) {
				ISynchronizeViewPage page = pages[i];
				Action action = new ShowSynchronizeViewPage(fView, page);  
				action.setChecked(page.equals(current));
				addActionToMenu(fMenu, action);
			}
			return fMenu;
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
		 * @see org.eclipse.ui.console.IConsoleListener#consolesAdded(org.eclipse.ui.console.IConsole[])
		 */
		public void consolesAdded(ISynchronizeViewPage[] consoles) {
			Display display = TeamUIPlugin.getStandardDisplay();
			display.asyncExec(new Runnable() {
				public void run() {
					update();
				}
			});
		}

		/* (non-Javadoc)
		 * 
		 * Dispose the menu when a launch is removed, such that the actions in this
		 * menu do not hang on to associated resources.
		 * 
		 * @see org.eclipse.ui.console.IConsoleListener#consolesRemoved(org.eclipse.ui.console.IConsole[])
		 */
		public void consolesRemoved(ISynchronizeViewPage[] consoles) {
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
