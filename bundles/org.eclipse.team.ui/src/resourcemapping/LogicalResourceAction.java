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
package resourcemapping;

import java.util.List;
import org.eclipse.core.resources.mapping.IResourceMapper;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ui.actions.TeamAction;

public class LogicalResourceAction extends TeamAction {

	public void run(IAction action) {
		List selection = getSelection().toList();	
		IResourceMapper[] m = (IResourceMapper[])selection.toArray(new IResourceMapper[selection.size()]);
		ResourceMappingSelectionDialog d = new ResourceMappingSelectionDialog(Display.getDefault().getActiveShell(), m);
		d.open();
	}
	
	protected boolean isEnabled() throws TeamException {
		return getSelection().size() > 0;
	}
	
	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);
	}
}
