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

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.ICompareNavigator;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.eclipse.team.ui.synchronize.actions.INavigableControl;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IKeyBindingService;
import org.eclipse.ui.actions.ActionFactory;

/**
 * Action to navigate the changes shown in the Synchronize View. This
 * will coordinate change browsing between the view and the compare 
 * editors.
 *
 * @since 3.0
 */
public class NavigateAction extends Action {
	private final int direction;
	private ISynchronizeView view;
	private Viewer viewer;
	
	public NavigateAction(ISynchronizeView view, Viewer viewer, int direction) {
		this.viewer = viewer;
		this.view = view;
		this.direction = direction;

		IKeyBindingService kbs = view.getSite().getKeyBindingService();		
		if(direction == INavigableControl.NEXT) {
			Utils.initAction(this, "action.navigateNext."); //$NON-NLS-1$
			view.getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.NEXT.getId(), this);			
		} else {
			Utils.initAction(this, "action.navigatePrevious."); //$NON-NLS-1$
			view.getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.PREVIOUS.getId(), this);			
		}
	}
	
	public void run() {
		navigate();
	}
	
	private void navigate() {
		SyncInfo info = getSyncInfoFromSelection();
		INavigableControl navigable = (INavigableControl)viewer;
		if(info == null) {
			if(navigable.gotoDifference(direction)) {
				return;
			} else {
				info = getSyncInfoFromSelection();
				if(info == null) return;
			}
		}
		
		if(info.getLocal().getType() != IResource.FILE) {
			if(! navigable.gotoDifference(direction)) {
				info = getSyncInfoFromSelection();
				OpenInCompareAction.openCompareEditor(view, view.getParticipant(), info, true /* keep focus */);
			}
			return;
		}
		
		IEditorPart editor = OpenInCompareAction.findOpenCompareEditor(view.getSite(), info.getLocal());			
		boolean atEnd = false;
		CompareEditorInput input;
		ICompareNavigator navigator;
		
		if(editor != null) {
			// if an existing editor is open on the current selection, use it			 
			input = (CompareEditorInput)editor.getEditorInput();
			navigator = (ICompareNavigator)input.getAdapter(ICompareNavigator.class);
			if(navigator != null) {
				if(navigator.selectChange(direction == INavigableControl.NEXT)) {
					if(! navigable.gotoDifference(direction)) {
						info = getSyncInfoFromSelection();
						OpenInCompareAction.openCompareEditor(view, view.getParticipant(), info, true /* keep focus */);
					}
				}				
			}
		} else {
			// otherwise, select the next change and open a compare editor which will automatically
			// show the first change.
			OpenInCompareAction.openCompareEditor(view, view.getParticipant(), info, true /* keep focus */);
		}
	}

	private SyncInfo getSyncInfoFromSelection() {
		IStructuredSelection selection = (IStructuredSelection)view.getSite().getPage().getSelection();
		if(selection == null) return null;
		Object obj = selection.getFirstElement();
		SyncInfo info = OpenInCompareAction.getSyncInfo(obj);
		return info;
	}
}