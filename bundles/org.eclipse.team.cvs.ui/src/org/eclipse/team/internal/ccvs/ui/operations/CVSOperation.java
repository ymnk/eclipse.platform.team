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
package org.eclipse.team.internal.ccvs.ui.operations;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.ui.PlatformUI;


/**
 * This class is the abstract superclass for CVS operations. It provides
 * error handling, prompting and other UI.
 */
public abstract class CVSOperation implements IRunnableWithProgress {
	
	private IRunnableContext runnableContext;
	private boolean interruptable = true;
	private boolean modifiesWorkspace = true;
	
	public static void run(Shell shell, CVSOperation operation) throws CVSException, InterruptedException {
		operation.setRunnableContext(new ProgressMonitorDialog(shell));
		operation.execute();
	}
	
	/**
	 * Execute the operation in the given runnable context. If null is passed, 
	 * the runnable context assigned to the operation is used.
	 * 
	 * @throws InterruptedException
	 * @throws CVSException
	 */
	public void execute(IRunnableContext runnableContext) throws InterruptedException, CVSException {
		if (runnableContext == null) {
			runnableContext = getRunnableContext();
		}
		try {
			runnableContext.run(isInterruptable(), isInterruptable(), this);
		} catch (InvocationTargetException e) {
			throw CVSException.wrapException(e);
		} catch (OperationCanceledException e) {
			throw new InterruptedException();
		}
	}
	
	/**
	 * Execute the operation in the runnable context that has been assigned to the operation.
	 * If a context has not been assigned, the workbench window is used.
	 * 
	 * @throws InterruptedException
	 * @throws CVSException
	 */
	public void execute() throws InterruptedException, CVSException {
		execute(getRunnableContext());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		try {
			if (isModifiesWorkspace()) {
				new CVSWorkspaceModifyOperation(this).execute(monitor);
			} else {
				execute(monitor);
			}
		} catch (CVSException e) {
			throw new InvocationTargetException(e);
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		}
	}
	
	/**
	 * Subclasses must override to perform the operation
	 * @param monitor
	 * @throws CVSException
	 * @throws InterruptedException
	 */
	public abstract void execute(IProgressMonitor monitor) throws CVSException, InterruptedException;

	/**
	 * @return
	 */
	private IRunnableContext getRunnableContext() {
		if (runnableContext == null) {
			return PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		}
		return runnableContext;
	}
	
	/**
	 * @return
	 */
	public boolean isInterruptable() {
		return interruptable;
	}

	/**
	 * @param context
	 */
	public void setRunnableContext(IRunnableContext context) {
		this.runnableContext = context;
	}

	/**
	 * @param b
	 */
	public void setInterruptable(boolean b) {
		interruptable = b;
	}

	/**
	 * @return
	 */
	public boolean isModifiesWorkspace() {
		return modifiesWorkspace;
	}

	/**
	 * @param b
	 */
	public void setModifiesWorkspace(boolean b) {
		modifiesWorkspace = b;
	}

}
