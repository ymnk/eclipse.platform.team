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
package org.eclipse.team.internal.ui.sync.views;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;

/**
 * This class acts as a wrapper for containers that appear in the SyncViewer.
 * It is required to enable actions to know they are working on a sync element.
 */
public class SyncContainer implements IAdaptable {
	private SyncSet syncSet;
	private IContainer container;
	
	protected SyncContainer(IContainer container) {
		this.container = container;
	}

	/**
	 * @return
	 */
	public IContainer getContainer() {
		return container;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object object) {
		if (object instanceof SyncContainer) {
			SyncContainer syncContainer = (SyncContainer) object;
			return getContainer().equals(syncContainer.getContainer());
		}
		return super.equals(object);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == IResource.class) {
			return getContainer();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getContainer().hashCode();
	}

}
