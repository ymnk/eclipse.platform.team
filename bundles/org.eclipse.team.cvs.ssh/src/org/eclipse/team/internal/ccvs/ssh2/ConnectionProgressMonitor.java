/*
 * (c) Copyright IBM Corp. 2002, 2003.
 * All Rights Reserved.
 */

package org.eclipse.team.internal.ccvs.ssh2;

import org.eclipse.core.runtime.IProgressMonitor;
import com.jcraft.jsch.IConnectionProgessMonitor;

class ConnectionProgressMonitor implements IConnectionProgessMonitor {
	private IProgressMonitor monitor;
	public ConnectionProgressMonitor(IProgressMonitor monitor) {
		this.monitor = monitor;
	}
	public void stage(String connectionStage) {
		monitor.subTask(connectionStage);
		monitor.worked(1);
	}
}
