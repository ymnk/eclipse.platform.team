/*******************************************************************************
 * Copyright (c) 2000, 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 * IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.team.internal.ccvs.core.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.syncinfo.NotifyInfo;

/**
 * This class acts as the abstract superclass of the commands related to watch/edit
 */
public abstract class AbstractNotificationCommand extends Command {

	public static final LocalOption EDIT_OPTION = new LocalOption("-a", "edit"); //$NON-NLS-1$ //$NON-NLS-2$
	public static final LocalOption UNEDIT_OPTION = new LocalOption("-a", "unedit"); //$NON-NLS-1$ //$NON-NLS-2$
	public static final LocalOption COMMIT_OPTION = new LocalOption("-a", "commit"); //$NON-NLS-1$ //$NON-NLS-2$
	public static final LocalOption ALL_OPTION = new LocalOption("-a", "all"); //$NON-NLS-1$ //$NON-NLS-2$
	public static final LocalOption NONE_OPTION = new LocalOption("-a", "none"); //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * @see org.eclipse.team.internal.ccvs.core.client.Command#sendLocalResourceState(Session, GlobalOption[], LocalOption[], ICVSResource[], IProgressMonitor)
	 */
	protected void sendLocalResourceState(
		Session session,
		GlobalOption[] globalOptions,
		LocalOption[] localOptions,
		ICVSResource[] resources,
		IProgressMonitor monitor)
		throws CVSException {
			
		// Send all folders that are managed to the server
		new NOOPVisitor(session, monitor).visit(session, resources);
	}
	
	/**
	 * Translate the -a local options to the notification characters used by edit/unedit
	 */
	protected char[] convertLocalOptionsToNotifyCharacters(LocalOption[] options) {
		List list = Arrays.asList(options);
		if (list.contains(ALL_OPTION)) {
			return NotifyInfo.ALL;
		} else if (list.contains(NONE_OPTION)) {
			return null;
		} else {
			List result = new ArrayList();
			if (list.contains(EDIT_OPTION)) {
				result.add(new Character(NotifyInfo.EDIT));
			}
			if (list.contains(UNEDIT_OPTION)) {
				result.add(new Character(NotifyInfo.UNEDIT));
			}
			if (list.contains(COMMIT_OPTION)) {
				result.add(new Character(NotifyInfo.COMMIT));
			}
			// if none are specified, the default is all
			if (result.isEmpty()) {
				return NotifyInfo.ALL;
			}
			char[] chars = new char[result.size()];
			for (int i = 0; i < chars.length; i++) {
				chars[i] = ((Character)result.get(i)).charValue();
			}
			return chars;
		}
	}

}
