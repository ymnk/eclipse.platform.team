/*
 * (c) Copyright IBM Corp. 2002, 2003.
 * All Rights Reserved.
 */
package org.eclipse.team.internal.ccvs.ssh2;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Hashtable;

import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org
	.eclipse
	.team
	.internal
	.ccvs
	.core
	.connection
	.CVSAuthenticationException;

import com.jcraft.jsch.*;

/**
 * A pool of active SSH connections.
 */
public class SSHSessionPool {
	private static final int DEFAULT_PORT = 22;
	private Hashtable pool;
	public SSHSessionPool() {
		pool = new Hashtable();
	}
	public Session getSession(ICVSRepositoryLocation location, String password, ConnectionProgressMonitor monitor) throws CVSAuthenticationException, IOException {
		if (!pool.containsKey(location)) connectSession(location, password, monitor);
		return (Session) pool.get(location);
	}
	public void removeLocation(ICVSRepositoryLocation location) {
		if (!pool.containsKey(location)) return;
		Session s = (Session) pool.get(location);
		s.disconnect();
		pool.remove(location);
	}
	private void connectSession(ICVSRepositoryLocation location, String password, ConnectionProgressMonitor monitor) throws IOException, CVSAuthenticationException {
		String hostname = location.getHost();
		IUserInfo userInfo = new PasswordAuthInfo(location.getUsername(), password);
		JSch jsch = new JSch();
		int port = location.getPort();
		if (port == ICVSRepositoryLocation.USE_DEFAULT_PORT) port = DEFAULT_PORT;
		Session session = jsch.getSession(location.getHost(), port);
		session.setUserInfo(userInfo);
		try {
			session.connect(monitor);
		} catch (UnknownHostException ex) {
				throw ex;
		} catch (AuthenticationException ex) {
			throw new CVSAuthenticationException("Authentication failed");
		} catch (Exception ex) {
			throw new IOException(ex.getMessage());
		}
		pool.put(location, session);
	}
}
