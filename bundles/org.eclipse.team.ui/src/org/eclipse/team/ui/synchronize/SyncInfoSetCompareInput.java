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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.ui.Utils;

/**
 * A <code>CompareEditorInput</code> whose diff viewer shows the resources contained
 * in a <code>SyncInfoSet</code>. The configuration of the diff viewer is determined by the 
 * <code>SyncInfoDiffTreeViewerConfiguration</code> that is used to create the 
 * <code>SyncInfoSetCompareInput</code>.
 * <p>
 * This class is not intended to be overriden by clients.
 * 
 * @since 3.0
 */
public class SyncInfoSetCompareInput extends CompareEditorInput {

	private SyncInfoDiffTreeViewerConfiguration diffViewerConfiguration;

	/**
	 * Create a <code>SyncInfoSetCompareInput</code> whose diff viewer is configured
	 * using the provided <code>SyncInfoDiffTreeViewerConfiguration</code>.
	 * @param configuration the compare configuration 
	 * @param diffViewerConfiguration the diff viewer configuration 
	 */
	public SyncInfoSetCompareInput(CompareConfiguration configuration, SyncInfoDiffTreeViewerConfiguration diffViewerConfiguration) {
		super(configuration);
		this.diffViewerConfiguration = diffViewerConfiguration;
	}

	public Viewer createDiffViewer(Composite parent) {
		StructuredViewer v = diffViewerConfiguration.createViewer(parent);
		diffViewerConfiguration.updateCompareEditorInput(this);
		v.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				ISelection s = event.getSelection();
				SyncInfoDiffNode node = getElement(s);
				if(node != null) {
					SyncInfo info = node.getSyncInfo();
					if(info != null && info.getLocal().getType() == IResource.FILE) { 
						Utils.updateLabels(node.getSyncInfo(), getCompareConfiguration());
					}
				}
			}
		});
		return v;
	}

	protected Object prepareInput(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		return new SyncInfoDiffNode(diffViewerConfiguration.getSyncSet(), ResourcesPlugin.getWorkspace().getRoot());
	}
	
	private static SyncInfoDiffNode getElement(ISelection selection) {
		if (selection != null && selection instanceof IStructuredSelection) {
			IStructuredSelection ss= (IStructuredSelection) selection;
			if (ss.size() == 1) {
				Object o = ss.getFirstElement();
				if(o instanceof SyncInfoDiffNode) {
					return (SyncInfoDiffNode)o;
				}
			}
		}
		return null;
	}
	
}
