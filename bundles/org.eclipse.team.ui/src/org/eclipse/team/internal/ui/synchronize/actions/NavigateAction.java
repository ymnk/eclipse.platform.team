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
import org.eclipse.compare.internal.INavigatable;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.SyncInfoModelElement;
import org.eclipse.team.ui.synchronize.subscribers.SubscriberConfiguration;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.ActionFactory;

/**
 * Action to navigate the changes shown in the Synchronize View. This
 * will coordinate change browsing between the view and the compare 
 * editors.
 *
 * @since 3.0
 */
public class NavigateAction extends Action {
	private final boolean next;
	private SubscriberConfiguration configuration;
	private INavigatable navigator;
	
	/**
	 * Direction to navigate
	 */
	final public static int NEXT = 1;
	final public static int PREVIOUS = 2;
	
	public NavigateAction(SubscriberConfiguration configuration, INavigatable navigator, boolean next) {
		this.navigator = navigator;
		this.configuration = configuration;
		this.next = next;
		IWorkbenchPart part = configuration.getPart();
		if (part != null && part.getSite() instanceof IViewSite) {
			IViewSite viewSite = (IViewSite)part.getSite();
			IKeyBindingService kbs = part.getSite().getKeyBindingService();
			if (next) {
				Utils.initAction(this, "action.navigateNext."); //$NON-NLS-1$
				viewSite.getActionBars().setGlobalActionHandler(ActionFactory.NEXT.getId(), this);
			} else {
				Utils.initAction(this, "action.navigatePrevious."); //$NON-NLS-1$
				viewSite.getActionBars().setGlobalActionHandler(ActionFactory.PREVIOUS.getId(), this);
			}
		}
	}
	
	public void run() {
		navigate();
	}
	
	private void navigate() {
		SyncInfo info = getSyncInfoFromSelection();
		if(info == null) {
			if(navigator.gotoDifference(next)) {
				return;
			} else {
				info = getSyncInfoFromSelection();
				if(info == null) return;
			}
		}
		
		if(info.getLocal().getType() != IResource.FILE) {
			if(! navigator.gotoDifference(next)) {
				info = getSyncInfoFromSelection();
				OpenInCompareAction.openCompareEditor(configuration.getPart(), configuration.getParticipant().getName(), info, true /* keep focus */);
			}
			return;
		}
		
		IEditorPart editor = OpenInCompareAction.findOpenCompareEditor(configuration.getPart().getSite(), info.getLocal());			
		boolean atEnd = false;
		CompareEditorInput input;
		ICompareNavigator navigator;
		
		if(editor != null) {
			// if an existing editor is open on the current selection, use it			 
			input = (CompareEditorInput)editor.getEditorInput();
			navigator = (ICompareNavigator)input.getAdapter(ICompareNavigator.class);
			if(navigator != null) {
				if(navigator.selectChange(next)) {
					if(! this.navigator.gotoDifference(next)) {
						info = getSyncInfoFromSelection();
						OpenInCompareAction.openCompareEditor(configuration.getPart(), getTitle(), info, true /* keep focus */);
					}
				}				
			}
		} else {
			// otherwise, select the next change and open a compare editor which will automatically
			// show the first change.
			OpenInCompareAction.openCompareEditor(configuration.getPart(), getTitle(), info, true /* keep focus */);
		}
	}

	private SyncInfo getSyncInfoFromSelection() {
		IStructuredSelection selection = (IStructuredSelection)configuration.getPart().getSite().getPage().getSelection();
		if(selection == null) return null;
		Object obj = selection.getFirstElement();
		if (obj instanceof SyncInfoModelElement) {
			return ((SyncInfoModelElement) obj).getSyncInfo();
		} else {
			return null;
		}
	}
	
	private String getTitle() {
		return configuration.getParticipant().getName();
	}
}