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
package org.eclipse.team.ui.synchronize;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.team.core.subscribers.SyncInfoSet;
import org.eclipse.team.internal.ui.synchronize.SyncInfoDiffTreeNavigator;
import org.eclipse.team.ui.synchronize.actions.INavigableControl;
import org.eclipse.ui.internal.dialogs.ContainerCheckedTreeViewer;

// TODO: This is an internal superclass
public class SyncInfoDiffCheckboxTreeViewer extends ContainerCheckedTreeViewer implements INavigableControl, SyncInfoDiffTreeNavigator.INavigationTarget {

	private SyncInfoDiffTreeViewerConfiguration configuration;
	
	/**
	 * Create a <code>SyncInfoDiffCheckboxTreeViewer</code> that uses trhe default configuration for
	 * the given menuId and <code>SyncInfoSet</code>.
	 * @param parent the parent composite
	 * @param menuId the menuId used to determine which objectContributions appear in the popup menu
	 * @param set the set which contains the resources to be displayed by the viewer
	 * @return a <code>SyncInfoDiffTreeViewer</code>
	 */
	public static StructuredViewer createViewer(Composite parent, String menuId, SyncInfoSet set) {
		SyncInfoDiffTreeViewerConfiguration configuration = new SyncInfoDiffTreeViewerConfiguration(menuId, set);
		return configuration.createViewer(parent);
	}
	
	public SyncInfoDiffCheckboxTreeViewer(Composite parent, SyncInfoDiffTreeViewerConfiguration configuration) {
		super(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		this.configuration = configuration;
	}

	protected void handleLabelProviderChanged(LabelProviderChangedEvent event) {
		Object[] changed = configuration.asModelObjects(this, event.getElements());
		if (changed != null) {
			if (changed.length == 0) {
				return;
			}
			event= new LabelProviderChangedEvent((IBaseLabelProvider) event.getSource(), changed);
		}
		super.handleLabelProviderChanged(event);
	}

	/**
	 * Cleanup listeners and call super for content provider and label provider disposal.
	 */	
	protected void handleDispose(DisposeEvent event) {
		super.handleDispose(event);
		configuration.dispose();
	}

	protected void inputChanged(Object in, Object oldInput) {
		super.inputChanged(in, oldInput);		
		if (in != oldInput) {
			configuration.getNavigator().navigate(false, true);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.actions.INavigableControl#gotoDifference(int)
	 */
	public boolean gotoDifference(int direction) {
		boolean next = direction == INavigableControl.NEXT ? true : false;
		return configuration.getNavigator().navigate(next, false);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncInfoDiffTreeNavigator.INavigationTarget#setSelection(org.eclipse.swt.widgets.TreeItem, boolean)
	 */
	public void setSelection(TreeItem ti, boolean fireOpen) {
		if (ti != null) {
			Object data= ti.getData();
			if (data != null) {
				// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=20106
				ISelection selection= new StructuredSelection(data);
				setSelection(selection, true);
				ISelection currentSelection= getSelection();
				if (fireOpen && currentSelection != null && selection.equals(currentSelection)) {
					fireOpen(new OpenEvent(this, selection));
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncInfoDiffTreeNavigator.INavigationTarget#createChildren(org.eclipse.swt.widgets.TreeItem)
	 */
	public void createChildren(TreeItem item) {
		super.createChildren(item);
	}
	
	/**
	 * @return Returns the configuration.
	 */
	public SyncInfoDiffTreeViewerConfiguration getConfiguration() {
		return configuration;
	}
	
}
