/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.subscriber;

import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.team.core.subscribers.SyncInfo;

public class ChangeLogDiffNode extends DiffNode {

	private String comment;

	public ChangeLogDiffNode(String comment) {
		super(SyncInfo.IN_SYNC);
		this.comment = comment;
	}

	/**
	 * @return Returns the comment.
	 */
	public String getComment() {
		return comment;
	}
	
	public boolean equals(Object other) {
		if(other == this) return true;
		if(! (other instanceof ChangeLogDiffNode)) return false;
		return ((ChangeLogDiffNode)other).getComment().equals(getComment());
	}
}
