package org.eclipse.team.internal.ccvs.core.client;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.client.Command.GlobalOption;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.client.listeners.ICommandOutputListener;

/**
 * @author Administrator
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class Edit extends AbstractNotificationCommand {

	/**
	 * @see org.eclipse.team.internal.ccvs.core.client.Request#getRequestId()
	 */
	protected String getRequestId() {
		// There is no actual edit command but a noop will get a response 
		// from the server for any notifies that were sent up.
		return "noop"; //$NON-NLS$
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.core.client.Command#doExecute(Session, GlobalOption[], LocalOption[], String[], ICommandOutputListener, IProgressMonitor)
	 */
	protected IStatus doExecute(
		Session session,
		GlobalOption[] globalOptions,
		LocalOption[] localOptions,
		String[] arguments,
		ICommandOutputListener listener,
		IProgressMonitor monitor)
		throws CVSException {
			
		// Populate the CVS/Notify files before issuing the command.
		
		
		// Issue the command
		return super.doExecute(session, globalOptions, localOptions,
			arguments, listener, monitor);
	}

}
