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

import org.eclipse.jface.action.Action;
import org.eclipse.team.ui.synchronize.views.ILogicalView;

/**
 * Action that selects an <code>ILogicalView</code>.
 */
public class LogicalViewAction extends Action {

	private ILogicalView view;
	private LogicalViewActionGroup group;

	public LogicalViewAction(ILogicalView view, LogicalViewActionGroup group) {
		this.view = view;
		this.group = group;
		setText(view.getLabel());
		setDescription(view.getDescription());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		group.selectView(view);
	}

	public ILogicalView getView() {
		return view;
	}

}
