/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.patch;

import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.mapping.SynchronizationLabelProvider;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;

public class PatchSyncLabelProvider extends SynchronizationLabelProvider {

	private PatchWorkbenchLabelProvider delegate;

	public PatchSyncLabelProvider() {
		super();
	}

	public void init(ICommonContentExtensionSite site) {
		super.init(site);
		delegate = new PatchWorkbenchLabelProvider();
		// delegate(site);
	}

	public void dispose() {
		super.dispose();
		if (delegate != null)
			delegate.dispose();
	}

	protected ILabelProvider getDelegateLabelProvider() {
		return delegate;
	}

	protected IDiff getDiff(Object element) {
		if (element instanceof IDiffElement) {
			ResourceMapping mapping = PatchModelProvider
					.getResourceMapping((IDiffElement) element);
			if (mapping != null) {
				// XXX: getting IResource for patch model object
				try {
					IResource resource = mapping.getTraversals(null, new NullProgressMonitor())[0]
							.getResources()[0];
					return getContext().getDiffTree().getDiff(resource);
				} catch (CoreException e) {
					TeamUIPlugin.log(e);
				}
			}
		}
		return super.getDiff(element);
	}
	
	protected Image getCompareImage(Image base, int kind) {
		/*
		 * Need to swap left and right for PatchDiffNodes as done in Apply Patch
		 * wizard. See org.eclipse.compare.structuremergeviewer.DiffTreeViewer.
		 * DiffViewerLabelProvider.getImage(Object).
		 */
		switch (kind & Differencer.DIRECTION_MASK) {
		case Differencer.LEFT:
			kind= (kind &~ Differencer.LEFT) | Differencer.RIGHT;
			break;
		case Differencer.RIGHT:
			kind= (kind &~ Differencer.RIGHT) | Differencer.LEFT;
			break;
		}
		return super.getCompareImage(base, kind);
	}

}
