/*
 * (c) Copyright IBM Corp. 2000, 2003.
 * All Rights Reserved.
 */

package org.eclipse.team.internal.ccvs.ssh2;

import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.IConnectionMethod;
import org.eclipse.team.internal.ccvs.core.IServerConnection;
import org.eclipse.team.internal.ccvs.ssh.SSHPlugin;

public class SSHMethod implements IConnectionMethod {
	/**
	 * @see IConnectionMethod#getName
	 */
	public String getName() {
		return "extssh2";//$NON-NLS-1$
	}
	
	/**
	 * @see IConnectionMethod#createConnection
	 */
	public IServerConnection createConnection(ICVSRepositoryLocation repositoryRoot, String password) {
		return new SSH2ServerConnection(repositoryRoot, password);
	}

	public void disconnect(ICVSRepositoryLocation location) {
		SSHPlugin.getPlugin().getPool().removeLocation(location);
	}
}
