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
package org.eclipse.team.ui.synchronize.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.internal.ui.actions.TeamAction;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.OpenFileAction;
import org.eclipse.ui.actions.OpenWithMenu;
import org.eclipse.ui.views.navigator.ResourceNavigatorMessages;

/**
 * This is the action group for the open actions.
 */
public class OpenWithActionGroup extends ActionGroup {

	private OpenFileAction openFileAction;
	private OpenInCompareAction openInCompareAction;
	private IViewPart part;

	public OpenWithActionGroup(IViewPart part) {
		this.part = part;
		makeActions();
	}

	protected void makeActions() {
		openFileAction = new OpenFileAction(part.getSite().getPage());
		openInCompareAction = new OpenInCompareAction(part);		
	}

	public void fillContextMenu(IMenuManager menu) {
		fillOpenWithMenu(menu, (IStructuredSelection)part.getSite().getPage().getSelection());
	}

	/**
	 * Adds the OpenWith submenu to the context menu.
	 * 
	 * @param menu the context menu
	 * @param selection the current selection
	 */
	private void fillOpenWithMenu(IMenuManager menu, IStructuredSelection selection) {

		// Only supported if exactly one file is selected.
		if (selection == null || selection.size() != 1)
			return;
		Object element = selection.getFirstElement();
		IResource resource = getResource(element);
		if (!(resource instanceof IFile)) {
			return;
		}
				
		menu.add(openInCompareAction);
		
		if(!((resource.exists()))) {
			return;
		}
		
		openFileAction.selectionChanged(selection);
		menu.add(openFileAction);
		
		MenuManager submenu =
			new MenuManager(ResourceNavigatorMessages.getString("ResourceNavigator.openWith")); //$NON-NLS-1$
		submenu.add(new OpenWithMenu(part.getSite().getPage(), (IFile) resource));
		menu.add(submenu);
	}

	/**
	 * Runs the default action (open file).
	 */
	public void runDefaultAction(IStructuredSelection selection) {
		Object element = selection.getFirstElement();
		if (element instanceof IFile) {
			openFileAction.selectionChanged(selection);
			openFileAction.run();
		}
	}
	
	private IResource getResource(Object obj) {
		return (IResource)TeamAction.getAdapter(obj, IResource.class);
	}

	public void openInCompareEditor() {
		openInCompareAction.run();		
	}
}