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

import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;

/**
 * Superclass of CVS particpant actions that uses the classname as the key
 * to access the text from the resource bundle
 */
public abstract class CVSParticipantAction extends SynchronizeModelAction {

	protected CVSParticipantAction() {
		super(null);
		Utils.initAction(this, getBundleKeyPrefix(), Policy.getBundle());
	}

	/**
	 * Return the key to the action text in the resource bundle.
	 * The default is the class name followed by a dot (.).
	 * @return the bundle key prefix
	 */
	protected String getBundleKeyPrefix() {
		return getClass().getName()  + ".";
	}
}
