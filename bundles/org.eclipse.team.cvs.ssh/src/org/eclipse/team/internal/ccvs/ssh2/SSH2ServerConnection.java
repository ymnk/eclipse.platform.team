package org.eclipse.team.internal.ccvs.ssh2;

/*
 * (c) Copyright IBM Corp. 2000, 2003.
 * All Rights Reserved.
 */

import java.io.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.IServerConnection;
import org.eclipse.team.internal.ccvs.core.connection.CVSAuthenticationException;
import org.eclipse.team.internal.ccvs.ssh.SSHPlugin;

import com.jcraft.jsch.*;

public class SSH2ServerConnection implements IServerConnection {

	// command to start remote cvs in server mode
	private static final String INVOKE_SVR_CMD = "cvs server"; //$NON-NLS-1$
	
	// cvs format for the repository (e.g. :extssh:user@host:/home/cvs/repo)
	private ICVSRepositoryLocation location;
	// password for user specified in repository location string
	private String password;
	
	private ChannelExec channel;
	private InputStream inputStream;
	private OutputStream outputStream;

	protected SSH2ServerConnection(ICVSRepositoryLocation location, String password) {
		this.location = location;
		this.password = password;
	}

	public void open(IProgressMonitor monitor) throws IOException, CVSAuthenticationException {
		ConnectionProgressMonitor innerMonitor = new ConnectionProgressMonitor(monitor);
		Session session = SSHPlugin.getPlugin().getPool().getSession(location, password, innerMonitor);
		channel=(ChannelExec) session.openChannel("exec");
		((ChannelExec)channel).setCommand(INVOKE_SVR_CMD);
		channel.connect();
		inputStream = channel.getInputStream();
		outputStream = channel.getOutputStream();
	}



	/**
	 * @see org.eclipse.team.internal.ccvs.core.IServerConnection#close()
	 */
	public void close() throws IOException {
		channel.close();
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.core.IServerConnection#getInputStream()
	 */
	public InputStream getInputStream() {
		return inputStream;
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.core.IServerConnection#getOutputStream()
	 */
	public OutputStream getOutputStream() {
		return outputStream;
	}

}
