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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.core.subscribers.SyncInfoSet;

/**
 * Configuration that provides a <code>CheckboxTreeViewer</code> as the diff viewer.
 */
public class SyncInfoDiffCheckboxTreeViewerConfiguration extends SyncInfoDiffTreeViewerConfiguration {

	private Object[] checked;
	private ICheckStateListener listener;
	
	/**
	 * Create a <code>SyncInfoDiffCheckboxTreeViewerConfiguration</code> on the given sync set.
	 * @param menuId the id of objectContributions to be shown in the viewer
	 * @param set the set containing the resources to be shown
	 */
	public SyncInfoDiffCheckboxTreeViewerConfiguration(String menuId, SyncInfoSet set, ICheckStateListener listener) {
		super(menuId, set);
		this.listener = listener;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncInfoDiffTreeViewerConfiguration#createViewer(org.eclipse.swt.widgets.Composite)
	 */
	public StructuredViewer createViewer(Composite parent) {
		final SyncInfoDiffCheckboxTreeViewer treeViewer = new SyncInfoDiffCheckboxTreeViewer(parent, this);
		initializeViewer(parent, treeViewer);
		treeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				checked = treeViewer.getCheckedElements();
				listener.checkStateChanged(event);
			}
		});
		return treeViewer;
	}
	
	/**
	 * Return the resources that were checked in the diff viewer.
	 * @return an array of resoruces
	 */
	public IResource[] getChecked() {
		List result = new ArrayList();
		if (checked != null) {
			for (int i = 0; i < checked.length; i++) {
				Object element = checked[i];
				if (element instanceof SyncInfoDiffNode) {
					// Only include out-of-sync resources that are checked
					SyncInfoDiffNode node = (SyncInfoDiffNode)element;
					SyncInfo syncInfo = node.getSyncInfo();
					if (syncInfo != null && syncInfo.getKind() != 0) {
						result.add(node.getResource());
					}
				}
			}
		}
		return (IResource[]) result.toArray(new IResource[result.size()]);
	}

}
