/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import java.util.Collections;

import org.eclipse.core.commands.*;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Event;

/**
 * An action that delegates to an appropriate handler when performing 
 * a merge opereration.
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is a guarantee neither that this API will
 * work nor that it will remain the same. Please do not use this API without
 * consulting with the Platform/Team team.
 * </p>
 * 
 * @since 3.2
 */
public class SynchronizationAction extends Action {
	
	private final String handlerId;
	private final CommonMenuManager manager;
	
	public SynchronizationAction(String handlerId, CommonMenuManager manager) {
		Assert.isNotNull(handlerId);
		Assert.isNotNull(manager);
		this.handlerId = handlerId;
		this.manager = manager;
	}
	
	public void runWithEvent(Event event) {
		IHandler handler = getHandler();
		if (handler != null && handler.isEnabled()) {
			try {
				handler.execute(new ExecutionEvent(null, Collections.EMPTY_MAP, event, null));
			} catch (ExecutionException e) {
				handle(e);
			}
		}
	}

	private void handle(Throwable e) {
		if (e instanceof ExecutionException) {
			ExecutionException ee = (ExecutionException) e;
			if (ee.getCause() != null) {
				handle(e.getCause());
			}
		}
		//TODO: handle the exception
	}

	private IHandler getHandler() {
		return manager.getHandler(handlerId);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#isEnabled()
	 */
	public boolean isEnabled() {
		IHandler handler = getHandler();
		return handler != null && handler.isEnabled();
	}

}
