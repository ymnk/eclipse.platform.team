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
import org.eclipse.compare.CompareConfiguration;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.subscribers.SyncInfo;
/**
 * This compare input supports selection of multiple sync info using
 * checkboxes.
 * 
 * @since 3.0
 */
public class SyncInfoSetCheckboxCompareInput extends SyncInfoSetCompareInput {
	private Object[] checked;
	private ICheckStateListener listener;
	/**
	 * Create a <code>SyncInfoSetCheckboxCompareInput</code> whose diff
	 * viewer is configured using the provided <code>SyncInfoSetCompareConfiguration</code>.
	 * 
	 * @param configuration
	 *            the compare configuration
	 * @param diffViewerConfiguration
	 *            the diff viewer configuration
	 * @param listener
	 *            listener taht is notified whenever the selection changes
	 */
	public SyncInfoSetCheckboxCompareInput(CompareConfiguration configuration, DiffTreeViewerConfiguration diffViewerConfiguration, ICheckStateListener listener) {
		super(configuration, diffViewerConfiguration);
		this.listener = listener;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.ui.synchronize.SyncInfoSetCompareInput#internalCreateDiffViewer(org.eclipse.swt.widgets.Composite,
	 *      org.eclipse.team.ui.synchronize.SyncInfoSetCompareConfiguration)
	 */
	protected StructuredViewer internalCreateDiffViewer(Composite parent, DiffTreeViewerConfiguration diffViewerConfiguration) {
		final SyncInfoDiffCheckboxTreeViewer treeViewer = new SyncInfoDiffCheckboxTreeViewer(parent, diffViewerConfiguration);
		treeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				checked = treeViewer.getCheckedElements();
				if (listener != null) {
					listener.checkStateChanged(event);
				}
			}
		});
		return treeViewer;
	}
	/**
	 * Return the resources that were checked in the diff viewer.
	 * 
	 * @return an array of resources
	 */
	public IResource[] getSelection() {
		List result = new ArrayList();
		if (checked != null) {
			for (int i = 0; i < checked.length; i++) {
				Object element = checked[i];
				if (element instanceof SyncInfoDiffNode) {
					// Only include out-of-sync resources that are checked
					SyncInfoDiffNode node = (SyncInfoDiffNode) element;
					SyncInfo syncInfo = node.getSyncInfo();
					if (syncInfo != null && syncInfo.getKind() != 0) {
						IResource resource = node.getResource();
						if (resource != null) {
							result.add(resource);
						}
					}
				}
			}
		}
		return (IResource[]) result.toArray(new IResource[result.size()]);
	}
}
