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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ccvs.core.CVSCompareSubscriber;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.TagSelectionDialog;
import org.eclipse.team.internal.ccvs.ui.subscriber.CompareParticipant;
import org.eclipse.team.ui.synchronize.subscriber.*;
import org.eclipse.team.ui.synchronize.viewers.*;

public class CompareWithTagAction extends WorkspaceAction {

	public void execute(IAction action) throws InvocationTargetException, InterruptedException {
		final IResource[] resources = getSelectedResources();
		CVSTag tag = promptForTag(resources);
		if (tag == null)
			return;
		
		// Run the comparison
		CVSCompareSubscriber s = new CVSCompareSubscriber(resources, tag);
		final CompareParticipant participant = new CompareParticipant(s);
		RefreshAction.run(null, participant.getName(), s.roots(), participant.getSubscriberSyncInfoCollector(), new IRefreshSubscriberListener() {
			public void refreshStarted(IRefreshEvent event) {
			}
			public void refreshDone(final IRefreshEvent event) {
				CVSUIPlugin.getStandardDisplay().asyncExec(new Runnable() {
					public void run() {
						if (event.getChanges().length == 0) {
							MessageDialog.openInformation(getShell(), "Compare Complete", "No changes found comparing resources");
							return;
						}
						if (isSingleFileCompare(resources)) {
							compareAndOpenEditors(event, participant);
						} else {
							compareAndOpenDialog(event, participant);
						}
					}
				});
			}
		});
	}

	/**
	 * Return <code>true</code> if at least one element in the selection
	 * is a folder.
	 * 
	 * @param resources resources to check
	 * @return <code>true</code> if at least one element in the selection
	 * is a folder and <code>false</code> otherwise.
	 */
	protected boolean isSingleFileCompare(IResource[] resources) {
		return resources.length == 1 && resources[0].getType() == IResource.FILE;
	}
	
	protected void compareAndOpenEditors(IRefreshEvent event, CompareParticipant participant) {
		SyncInfo[] changes= event.getChanges();
		for (int i = 0; i < changes.length; i++) {
			SyncInfo info = changes[i];
			CompareUI.openCompareEditor(new SyncInfoCompareInput(event.getSubscriber().getName(), info));
		}
	}
	
	protected void compareAndOpenDialog(IRefreshEvent event, CompareParticipant participant) {
		TreeViewerAdvisor advisor = new TreeViewerAdvisor(participant.getId(), participant.getSubscriberSyncInfoCollector().getSyncInfoTree());
		CompareConfiguration cc = new CompareConfiguration();
		SynchronizeCompareInput input = new SynchronizeCompareInput(cc, advisor);	
		try {
			input.run(new NullProgressMonitor());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		CompareDialog dialog = new CompareDialog(getShell(), input);
		dialog.setSynchronizeParticipant(participant);
		dialog.setBlockOnOpen(true);
		dialog.open();
	}
	
	protected CVSTag promptForTag(IResource[] resources) {
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