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
package org.eclipse.team.ui.synchronize.content;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.internal.ui.synchronize.views.SyncSetContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Provides text and image labels for <code>SyncInfo</code> and <code>SyncInfoDiffNode</code>
 * instances by delegating to a <code>WorkbenchLabelProvider</code>.
 */
public class SyncInfoLabelProvider extends LabelProvider {

	WorkbenchLabelProvider workbenchLabelProvider = new WorkbenchLabelProvider();
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	public void dispose() {
		super.dispose();
		workbenchLabelProvider.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		IResource resource = SyncSetContentProvider.getResource(element);
		if (resource != null) {
			return workbenchLabelProvider.getImage(resource);
		}
		return super.getImage(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		IResource resource = SyncSetContentProvider.getResource(element);
		if (resource != null) {
			return workbenchLabelProvider.getText(resource);
		}
		return super.getText(element);
	}
}
