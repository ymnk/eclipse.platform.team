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
package org.eclipse.team.ui.sync.actions;

import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.ui.IViewPart;


public class ExpandAllAction extends Action {
	private IViewPart part;
	private AbstractTreeViewer viewer;
	
	public ExpandAllAction(IViewPart part) {
		this.part = part;
		Utils.initAction(this, "action.expandAll."); //$NON-NLS-1$
	}
	
	public void run() {
		expandSelection();
	}
	
	public void setTreeViewer(AbstractTreeViewer viewer) {
		this.viewer = viewer;
	}
	
	public void update() {
		setEnabled(viewer != null && hasSelection());
	}
	
	protected void expandSelection() {
		if (viewer != null) {
			ISelection selection = getSelection();
			if (selection instanceof IStructuredSelection) {
				Iterator elements = ((IStructuredSelection)selection).iterator();
				while (elements.hasNext()) {
					Object next = elements.next();
					viewer.expandToLevel(next, AbstractTreeViewer.ALL_LEVELS);
				}
			}
		}
	}

	private ISelection getSelection() {
		return part.getSite().getPage().getSelection();
	}
	
	private boolean hasSelection() {
		ISelection selection = getSelection();
		return (selection != null && !selection.isEmpty());
	}
}