/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.mapping.IMergeContext;
import org.eclipse.team.ui.operations.ModelProviderOperation;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Action that performs an optimistic merge
 */
public class OptimisticUpdateAction extends Action {

	private final ISynchronizePageConfiguration configuration;

	public OptimisticUpdateAction(ISynchronizePageConfiguration configuration) {
		this.configuration = configuration;
		Utils.initAction(this, "action.merge."); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		final IMergeContext context = (IMergeContext)((ModelSynchronizeParticipant)configuration.getParticipant()).getContext();
		try {
			new ModelProviderOperation(configuration) {
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					try {
						performMerge(context, monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			
			}.run();
		} catch (InvocationTargetException e) {
			Utils.handle(e);
		} catch (InterruptedException e) {
			// Ignore
		}
	}
	

}
