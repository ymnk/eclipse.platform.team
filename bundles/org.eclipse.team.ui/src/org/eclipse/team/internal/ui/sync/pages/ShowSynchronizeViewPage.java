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
import org.eclipse.team.ui.sync.INewSynchronizeView;
import org.eclipse.team.ui.sync.ISynchronizeViewPage;

public class ShowSynchronizeViewPage extends Action {
	private ISynchronizeViewPage fPage;
	private INewSynchronizeView fView;
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		if (!fPage.equals(fView.getActivePage())) {
			fView.display(fPage);
		}
	}
	
	/**
	 * Constructs an action to display the given page.
	 * 
	 * @param view the synchronize view in which the given page is contained
	 * @param console the console
	 */
	public ShowSynchronizeViewPage(INewSynchronizeView view, ISynchronizeViewPage page) {
		super(page.getName(), Action.AS_RADIO_BUTTON);
		fPage = page;
		fView = view;
		setImageDescriptor(page.getImageDescriptor());
	}
}
