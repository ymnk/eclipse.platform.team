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
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.team.internal.ui.synchronize.views.ITreeViewerAccessor;
import org.eclipse.team.internal.ui.synchronize.views.TreeViewerUtils;

/**
 * A tree viewer that displays the model provided by a {@link DiffTreeViewerConfiguration}. 
 * 
 * @see DiffTreeViewerConfiguration
 * @see SyncInfoDiffCheckboxTreeViewer
 * @since 3.0
 */
public final class SyncInfoDiffTreeViewer extends TreeViewer implements INavigatable, ITreeViewerAccessor {

	private DiffTreeViewerConfiguration configuration;
	
	/**
	 * Creates a new viewer that is initialized with the provided configuration. The configuration
	 * determines the model, menus, and toolbars for this viewer.
	 * @param parent the parent composite for the viewer
	 * @param configuration the configuration containing the model, menus, and toolbars for this
	 * viewer.
	 */
	public SyncInfoDiffTreeViewer(Composite parent, DiffTreeViewerConfiguration configuration) {
		super(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		GridData data = new GridData(GridData.FILL_BOTH);
		getControl().setLayoutData(data);
		this.configuration = configuration;
		configuration.initializeViewer(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ContentViewer#handleDispose(org.eclipse.swt.events.DisposeEvent)
	 */
	protected void handleDispose(DisposeEvent event) {
		super.handleDispose(event);
		configuration.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.Viewer#inputChanged(java.lang.Object, java.lang.Object)
	 */
	protected void inputChanged(Object in, Object oldInput) {
		super.inputChanged(in, oldInput);		
		if (in != oldInput) {
			gotoDifference(true /*next*/);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.internal.INavigatable#gotoDifference(boolean)
	 */
	public boolean gotoDifference(boolean next) {
		return TreeViewerUtils.navigate(this, next, true /*fire open event*/, false /*set selection*/);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.views.ITreeViewerAccessor#openSelection()
	 */
	public void openSelection() {
		fireOpen(new OpenEvent(this, getSelection()));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.views.ITreeViewerAccessor#createChildren(org.eclipse.swt.widgets.TreeItem)
	 */
	public void createChildren(TreeItem item) {
		super.createChildren(item);
	}
}
