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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.core.subscribers.TeamSubscriber;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.ui.sync.SubscriberPage;
import org.eclipse.ui.IActionBars;

public class CVSSubscriberPage extends SubscriberPage {

	private SubscriberCommitAction commit;
	private Action commitAdapter;
	private int num;
	
	public CVSSubscriberPage(TeamSubscriber subscriber, String name, ImageDescriptor imageDescriptor, int num) {
		super(subscriber, name, imageDescriptor);
		this.num = num;
		commit = new SubscriberCommitAction();
		commitAdapter = new Action("", CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_NEWLOCATION)) {
			public void run() {
				commit.selectionChanged(this, page.getSite().getPage().getSelection());
				commit.run(this);
			}
		};
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.SubscriberPage#setActionsBars(org.eclipse.ui.IActionBars)
	 */
	public void setActionsBars(IActionBars actionBars) {
		super.setActionsBars(actionBars);
		if(num >= 3) {
			actionBars.getToolBarManager().add(commitAdapter);
		}
	}

}
