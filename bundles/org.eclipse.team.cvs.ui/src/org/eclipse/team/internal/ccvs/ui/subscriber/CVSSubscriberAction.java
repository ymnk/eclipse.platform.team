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
package org.eclipse.team.internal.ccvs.ui.subscriber;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.sync.SyncInfo;
import org.eclipse.team.internal.ccvs.core.CVSSyncInfo;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ui.sync.views.SyncResource;
import org.eclipse.team.ui.sync.SubscriberAction;
import org.eclipse.team.ui.sync.SyncResourceSet;
import org.eclipse.ui.PlatformUI;

/**
 * @author Administrator
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public abstract class CVSSubscriberAction extends SubscriberAction {
	
	protected boolean isOutOfSync(SyncResource resource) {
		if (resource == null) return false;
		return (!(resource.getKind() == 0) || ! resource.getLocalResource().exists());
	}
	
	/**
	 * @param element
	 */
	protected void makeInSync(SyncResource element) throws TeamException {
		if (isOutOfSync(element)) {
			SyncResource parent = element.getParent();
			if (parent != null) {
				makeInSync(parent);
			}
			SyncInfo info = element.getSyncInfo();
			if (info == null) return;
			if (info instanceof CVSSyncInfo) {
				CVSSyncInfo cvsInfo= (CVSSyncInfo) info;
				cvsInfo.makeInSync();
			}
		}
	}
	
	/**
	 * Handle the exception by showing an error dialog to the user.
	 * Sync actions seem to need to be sync-execed to work
	 * @param t
	 */
	protected void handle(Throwable t) {
		CVSUIPlugin.openError(getShell(), getErrorTitle(), null, t, CVSUIPlugin.PERFORM_SYNC_EXEC | CVSUIPlugin.LOG_NONTEAM_EXCEPTIONS);
	}

	/**
	 * Return the error title that will appear in any error dialogs shown to the user
	 * @return
	 */
	protected String getErrorTitle() {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
//		TODO: Saving can change the sync state! How should this be handled?
//			 boolean result = saveIfNecessary();
//			 if (!result) return null;

		SyncResourceSet syncSet = getFilteredSyncResourceSet(getFilteredSyncResources());
		if (syncSet == null || syncSet.isEmpty()) return;
		try {
			getRunnableContext().run(true /* fork */, true /* cancelable */, getRunnable(syncSet));
		} catch (InvocationTargetException e) {
			handle(e);
		} catch (InterruptedException e) {
			// nothing to do;
		}
	}

	/**
	 * Return an IRunnableWithProgress that will operate on the given sync set.
	 * This method is invoked by <code>run(IAction)</code> when the action is
	 * executed from a menu. The default implementation invokes the method
	 * <code>run(SyncResourceSet, IProgressMonitor)</code>.
	 * @param syncSet
	 * @return
	 */
	protected IRunnableWithProgress getRunnable(final SyncResourceSet syncSet) {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				CVSSubscriberAction.this.run(syncSet, monitor);
			}
		};
	}

	/**
	 * @param syncSet
	 * @param monitor
	 */
	protected abstract void run(SyncResourceSet syncSet, IProgressMonitor monitor) throws InvocationTargetException, InterruptedException;

	/**
	 * Returns the runnableContext.
	 * @return IRunnableContext
	 */
	protected IRunnableContext getRunnableContext() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	}
	
	/**
	 * Filter the sync resource set using action specific criteria or input from the user.
	 * @param selectedResources
	 * @return
	 */
	protected SyncResourceSet getFilteredSyncResourceSet(SyncResource[] selectedResources) {
		// If there are conflicts or outgoing changes in the syncSet, we need to warn the user.
		return new SyncResourceSet(selectedResources);
	}
}
