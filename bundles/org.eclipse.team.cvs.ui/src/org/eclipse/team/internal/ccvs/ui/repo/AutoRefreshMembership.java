/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 * IBM - Initial implementation
 ******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.repo;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.actions.CVSAction;
import org.eclipse.team.internal.ccvs.ui.model.CVSTagElement;

/**
 * For each child of the selected tag, refresh the known tags using the auto-refresh files
 */
public class AutoRefreshMembership extends CVSAction {

	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#execute(org.eclipse.jface.action.IAction)
	 */
	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		// XXX this method only removes. It never adds
		run(new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				CVSTagElement[] tags = getSelectedTags();
				RepositoryManager manager = CVSUIPlugin.getPlugin().getRepositoryManager();
				try {
					ICVSRemoteResource[] resources = manager.getFoldersForTag(tags[0].getRoot(), tags[0].getTag(), Policy.monitorFor(null));
					monitor.beginTask(null, 100 * resources.length);
					for (int i = 0; i < resources.length; i++) {
						ICVSRemoteResource resource = resources[i];
						if (resource instanceof ICVSFolder) {
							manager.refreshDefinedTags((ICVSFolder)resource, true /* replace */, true, Policy.subMonitorFor(monitor, 100));
						}
					}
				} catch (TeamException e) {
					throw new InvocationTargetException(e);
				}
			}
		}, true, PROGRESS_DIALOG);
	}

	/**
	 * @see org.eclipse.team.internal.ui.actions.TeamAction#isEnabled()
	 */
	protected boolean isEnabled() throws TeamException {
		CVSTagElement[] tags = getSelectedTags();
		if (tags.length != 1) return false;
		return (tags[0].getTag().getType() == CVSTag.VERSION || tags[0].getTag().getType() == CVSTag.BRANCH);
	}
	
	/**
	 * Returns the selected CVS tags
	 */
	protected CVSTagElement[] getSelectedTags() {
		ArrayList tags = new ArrayList();
		if (!selection.isEmpty()) {
			Iterator elements = ((IStructuredSelection) selection).iterator();
			while (elements.hasNext()) {
				Object adapter = getAdapter(elements.next(), CVSTagElement.class);
				if (adapter instanceof CVSTagElement) {
					tags.add(adapter);
				}
			}
		}
		return (CVSTagElement[])tags.toArray(new CVSTagElement[tags.size()]);
	}
}
