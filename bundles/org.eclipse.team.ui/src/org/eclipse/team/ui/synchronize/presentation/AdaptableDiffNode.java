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
package org.eclipse.team.ui.synchronize.presentation;

import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;

public class AdaptableDiffNode extends DiffNode implements IAdaptable {

	/**
	 * Bit flag which indicates that the diff node is currently
	 * be worked on by a background job.
	 */
	public static final int BUSY = 1;
	
	/**
	 * Bit flag which indicates that this diff node is 
	 * a conflict or is a parent of a conflict
	 */
	public static final int PROPOGATED_CONFLICT = 2;

	// Instance variable containing the flags for this node
	private int flags;
	
	public AdaptableDiffNode(IDiffContainer parent, int kind) {
		super(parent, kind);
	}

	/*
	 * Added as part
	 */
	public ImageDescriptor getImageDescriptor(Object object) {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.structuremergeviewer.DiffContainer#hasChildren()
	 */
	public boolean hasChildren() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	/**
	 * Return the flags associated with this node
	 * @return the flags for this node
	 */
	public int getFlags() {
		return flags;
	}
	
	/**
	 * Return whether this node has the given flag set.
	 * @param flag the flag to test
	 * @return <code>true</code> if the flag is set
	 */
	public boolean hasFlag(int flag) {
		return (getFlags() & flag) != 0;
	}

	/**
	 * Add the flag to the flags for this node
	 * @param flag the flag to add
	 */
	public void addFlag(int flag) {
		flags |= flag;
	}

	/**
	 * Remove the flag from the flags of this node.
	 * @param flag the flag to remove
	 */
	public void removeFlag(int flag) {
		if (hasFlag(flag)) {
			flags ^= flag;
		}
	}
}
