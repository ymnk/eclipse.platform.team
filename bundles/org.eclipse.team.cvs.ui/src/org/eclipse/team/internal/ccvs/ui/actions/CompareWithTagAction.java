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
package org.eclipse.team.internal.ccvs.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.TagSelectionDialog;
import org.eclipse.team.internal.ccvs.ui.subscriber.CVSLocalCompareConfiguration;
import org.eclipse.team.ui.synchronize.viewers.SyncInfoSetCompareInput;

public class CompareWithTagAction extends WorkspaceAction {

	public void execute(IAction action) throws InvocationTargetException, InterruptedException {
		IResource[] resources = getSelectedResources();
		CVSTag tag = promptForTag(resources);
		if (tag == null) return;
		final SyncInfoSetCompareInput input = createCompareInput(resources, tag);
		
		// Show the compare viewer
		run(new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				CompareUI.openCompareEditorOnPage(input, getTargetPage());
			}
		}, false /* cancelable */, PROGRESS_BUSYCURSOR);
	}
	
	private SyncInfoSetCompareInput createCompareInput(IResource[] resources, CVSTag tag) {
		final CVSLocalCompareConfiguration viewerConfig = CVSLocalCompareConfiguration.create(resources, tag);
		final SyncInfoSetCompareInput input = new SyncInfoSetCompareInput(new CompareConfiguration(), viewerConfig) {
				protected Object prepareInput(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						return viewerConfig.prepareInput(monitor);
					} catch (TeamException e) {
						throw new InvocationTargetException(e);
					}
				}
				public void contributeToToolBar(ToolBarManager tbm) {
					viewerConfig.contributeToToolBar(tbm);
					super.contributeToToolBar(tbm);
				}
		};
		return input;
	}

	private CVSTag promptForTag(IResource[] resources) {
		IProject[] projects = new IProject[resources.length];
		for (int i = 0; i < resources.length; i++) {
			projects[i] = resources[i].getProject();
		}
		CVSTag tag = TagSelectionDialog.getTagToCompareWith(getShell(), projects);
		return tag;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForNonExistantResources()
	 */
	protected boolean isEnabledForNonExistantResources() {
		return true;
	}
	
	
}
