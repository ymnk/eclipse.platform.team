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
package org.eclipse.team.tests.ui.views;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.core.subscribers.SyncInfoSet;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.synchronize.SyncInfoDiffNode;
import org.eclipse.team.ui.synchronize.views.CompressedFolderContentProvider;
import org.eclipse.team.ui.synchronize.views.SyncInfoLabelProvider;
import org.eclipse.ui.*;
import org.eclipse.ui.part.ViewPart;

public class ContentProviderTestView extends ViewPart {
	
	public static final String VIEW_ID = "org.eclipse.team.tests.ui.views.ContentProviderTestView";
	
	private TestTreeViewer viewer;

	public static ContentProviderTestView findViewInActivePage(IWorkbenchPage activePage) {
		try {
			if (activePage == null) {
				activePage = TeamUIPlugin.getActivePage();
				if (activePage == null) return null;
			}
			IViewPart part = activePage.findView(VIEW_ID);
			if (part == null)
				part = activePage.showView(VIEW_ID);
			return (ContentProviderTestView)part;
		} catch (PartInitException pe) {
			return null;
		}
	}
	
	public ContentProviderTestView() {
	}

	public void createPartControl(Composite parent) {
		viewer = new TestTreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new CompressedFolderContentProvider());
		viewer.setLabelProvider(new SyncInfoLabelProvider());
		setInput(new SyncInfoSet(new SyncInfo[0]));
	}

	public void setInput(SyncInfoSet set) {
		viewer.setInput(new SyncInfoDiffNode(set, ResourcesPlugin.getWorkspace().getRoot()));
	}

	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	public TestTreeViewer getViewer() {
		return viewer;
	}

}