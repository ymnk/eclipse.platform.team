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
package org.eclipse.team.internal.ccvs.core.client.listeners;

import org.eclipse.team.internal.ccvs.core.client.CommandOutputListener;

/**
 * This class parses the messages recieved in response to an "cvs rdiff -s ..." command
 */
public class RDiffSummaryListener extends CommandOutputListener {

	private IFileDiffListener listener;
	
	public interface IFileDiffListener {
		public void fileDiff(
				String remoteFilePath,
				String leftRevision,
				String rightRevision);
	}
	
	public RDiffSummaryListener(IFileDiffListener listener) {
		this.listener = listener;
	}
}
