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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.internal.ccvs.core.CVSMergeSubscriber;
import org.eclipse.team.ui.sync.SubscriberPage;
import org.eclipse.team.ui.sync.actions.RemoveSynchronizeViewPageAction;
import org.eclipse.ui.IActionBars;

public class CVSMergeSubscriberPage extends SubscriberPage {

	private RemoveSynchronizeViewPageAction removeAction;
	
	public CVSMergeSubscriberPage(CVSMergeSubscriber subscriber, String name, ImageDescriptor imageDescriptor) {
		super(subscriber, name, imageDescriptor);
		makeActions();
	}
		
	private void makeActions() {
		removeAction = new RemoveSynchronizeViewPageAction(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.SubscriberPage#setActionsBars(org.eclipse.ui.IActionBars)
	 */
	public void setActionsBars(IActionBars actionBars) {		
		super.setActionsBars(actionBars);
		actionBars.getToolBarManager().add(removeAction);		
	}
}
