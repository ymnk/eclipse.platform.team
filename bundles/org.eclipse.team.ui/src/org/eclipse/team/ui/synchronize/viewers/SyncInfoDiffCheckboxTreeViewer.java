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
package org.eclipse.team.ui.synchronize.viewers;

import org.eclipse.compare.internal.INavigatable;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.team.internal.ui.synchronize.views.ITreeViewerAccessor;
import org.eclipse.team.internal.ui.synchronize.views.TreeViewerUtils;
import org.eclipse.ui.internal.dialogs.ContainerCheckedTreeViewer;

/**
 * [Note: the superclass is internal but contains much behavior that should
 * be re-used instead of copied. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=48138
 * for more details.]
 */
public final class SyncInfoDiffCheckboxTreeViewer extends ContainerCheckedTreeViewer implements INavigatable, ITreeViewerAccessor {

	private DiffTreeViewerConfiguration configuration;
	
	public SyncInfoDiffCheckboxTreeViewer(Composite parent, DiffTreeViewerConfiguration configuration) {
		super(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		GridData data = new GridData(GridData.FILL_BOTH);
		getControl().setLayoutData(data);
		this.configuration = configuration;
		configuration.initializeViewer(this);
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
			gotoDifference(true /*next*/);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.actions.INavigableControl#gotoDifference(int)
	 */
	public boolean gotoDifference(boolean next) {
		return TreeViewerUtils.navigate(this, next, true /*don't fire open event*/, false /*set selection*/);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncInfoDiffTreeNavigator.INavigationTarget#openSelection()
	 */
	public void openSelection() {
		fireOpen(new OpenEvent(this, getSelection()));
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
	public DiffTreeViewerConfiguration getConfiguration() {
		return configuration;
	}	
}
