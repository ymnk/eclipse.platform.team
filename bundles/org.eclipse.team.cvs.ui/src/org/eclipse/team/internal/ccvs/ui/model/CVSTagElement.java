/*******************************************************************************
 * Copyright (c) 2000, 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 * IBM - Initial implementation
 ******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.model;
 
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class CVSTagElement extends CVSModelElement implements IAdaptable {
	CVSTag tag;
	ICVSRepositoryLocation root;
	
	/**
	 * Create a branch tag
	 */
	public CVSTagElement(CVSTag tag, ICVSRepositoryLocation root) {
		this.tag = tag;
		this.root = root;
	}
	public ICVSRepositoryLocation getRoot() {
		return root;
	}
	public Object getAdapter(Class adapter) {
		if (adapter == IWorkbenchAdapter.class) return this;
		return null;
	}
	public CVSTag getTag() {
		return tag;
	}
	public boolean equals(Object o) {
		if (!(o instanceof CVSTagElement)) return false;
		CVSTagElement t = (CVSTagElement)o;
		if (!tag.equals(t.tag)) return false;
		return root.equals(t.root);
	}
	public int hashCode() {
		return root.hashCode() ^ tag.hashCode();
	}
	/**
	 * Return children of the root with this tag.
	 */
	public Object[] getChildren(Object o) {
		// Return the remote elements for the tag
		final Object[][] result = new Object[1][];
		try {
			CVSUIPlugin.runWithProgress(null, true /*cancelable*/, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						result[0] = CVSUIPlugin.getPlugin().getRepositoryManager().getFoldersForTag(root, tag, monitor);
					} catch (TeamException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InterruptedException e) {
			return new Object[0];
		} catch (InvocationTargetException e) {
			handle(e.getTargetException());
		}
		return result[0];
	}
	public ImageDescriptor getImageDescriptor(Object object) {
		if (!(object instanceof CVSTagElement)) return null;
		if (tag.getType() == tag.BRANCH || tag.getType() == tag.HEAD) {
			return CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_TAG);
		} else if (tag.getType() == tag.VERSION) {
			return CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_PROJECT_VERSION);
		} else {
			// This could be a Date tag
			return null;
		}
	}
	public String getLabel(Object o) {
		if (!(o instanceof CVSTagElement)) return null;
		return ((CVSTagElement)o).tag.getName();
	}
	public Object getParent(Object o) {
		if (!(o instanceof CVSTagElement)) return null;
		return ((CVSTagElement)o).root;
	}
}