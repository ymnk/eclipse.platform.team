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
package org.eclipse.team.internal.core.subscribers;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.subscribers.ISyncInfoSetChangeListener;

/**
 * Interface used by <code>SubscriberSyncInfoSet</code> to report errors that
 * occurred while populating the set to interested parties
 */
public interface ISyncInfoSetChangeListener2 extends ISyncInfoSetChangeListener {
	/**
	 * Handle an error that ocurres
	 * @param event the error event
	 * @param monitor a progress monitor
	 */
	public void handleError(SubscriberErrorEvent event, IProgressMonitor monitor);
}
