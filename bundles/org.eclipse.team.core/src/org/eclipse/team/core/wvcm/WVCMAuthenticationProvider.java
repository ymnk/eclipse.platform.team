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
package org.eclipse.team.core.wvcm;

import javax.wvcm.ProviderFactory;

public class WVCMAuthenticationProvider {
	ProviderFactory.Callback callback = new ProviderFactory.Callback() {
		/**
		 * This is just a dummy callback. There wasn't much in the javadoc
		 * for this. However, if I was implementing
		 * one I would make the following assumptions.
		 * 1) the retryCount on the first attempt is 0.
		 * 2) On the first attemp, the Callback will use cached user info. 
		 * If none is cached, the user is prompted.
		 * 3) On subsequent attempts (retryCount > 0), the user is prompted and
		 * the new values are cached.
		 * 4) If the user cancels the prompt, null is returned to indicate the cancel
		 */
		public Authentication getAuthentication(String realm, Integer retryCount) {
			return new ProviderFactory.Callback.Authentication() {
				public String loginName() {
					return "someone";
				}
				public String password() {
					return "someone's password";
				}
			};
		}
	};
	
	public ProviderFactory.Callback getDefaultAuthentication() {
		return callback;
	}
}
