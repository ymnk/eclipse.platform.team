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
package org.eclipse.team.ui.synchronize.subscriber;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.*;
import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.ui.synchronize.viewers.*;

/**
 * Action to refresh a participant to find changes then display them in a dialog. The user can edit the changes
 * in the dialog and will be prompted to save the changes when the dialog is closed.
 * 
 * @since 3.0
 */
public class OpenCompareDialogAction {

	private SubscriberParticipant participant;
	private Shell shell;
	private IResource[] resources;
	private boolean rememberInSyncView;
	private String targetId;

	/**
	 * 
	 * @param shell shell to use to open the compare dialog
	 * @param participant the participant to use as a basis for the comparison
	 * @param resources
	 */
	public OpenCompareDialogAction(Shell shell, String targetId, SubscriberParticipant participant, IResource[] resources) {
		this.shell = shell;
		this.targetId = targetId;
		this.participant = participant;
		this.resources = resources;
	}

	public void run() {
		Subscriber s = participant.getSubscriber();
		RefreshAction.run(null, participant.getName(), s.roots(), participant.getSubscriberSyncInfoCollector(), new IRefreshSubscriberListener() {
			public void refreshStarted(IRefreshEvent event) {
			}
			public void refreshDone(final IRefreshEvent event) {
				TeamUIPlugin.getStandardDisplay().asyncExec(new Runnable() {
					public void run() {
						if (participant.getSubscriberSyncInfoCollector().getSyncInfoTree().isEmpty()) {
							MessageDialog.openInformation(getShell(), Policy.bind("OpenComparedDialog.noChangeTitle"), Policy.bind("OpenComparedDialog.noChangesMessage"));
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

	protected Shell getShell() {
		return shell;
	}
	
	protected boolean isSingleFileCompare(IResource[] resources) {
		return resources.length == 1 && resources[0].getType() == IResource.FILE;
	}

	protected void compareAndOpenEditors(IRefreshEvent event, SubscriberParticipant participant) {
		SyncInfo[] changes = event.getChanges();
		for (int i = 0; i < changes.length; i++) {
			SyncInfo info = changes[i];
			CompareUI.openCompareEditor(new SyncInfoCompareInput(event.getSubscriber().getName(), info));
		}
	}

	protected void compareAndOpenDialog(final IRefreshEvent event, final SubscriberParticipant participant) {
		TreeViewerAdvisor advisor = new TreeViewerAdvisor(targetId, null, participant.getSubscriberSyncInfoCollector().getSyncInfoTree());
		CompareConfiguration cc = new CompareConfiguration();
		SynchronizeCompareInput input = new SynchronizeCompareInput(cc, advisor) {
			public String getTitle() {
				int numChanges = participant.getSubscriberSyncInfoCollector().getSyncInfoTree().size();
				if(numChanges > 1) {
					return Policy.bind("OpenComparedDialog.diffViewTitleMany", Integer.toString(numChanges));
				} else {
					return Policy.bind("OpenComparedDialog.diffViewTitleOne", Integer.toString(numChanges));
				}
			}
		};
		try {
			// model will be built in the background since we know the compare input was 
			// created with a subscriber participant
			input.run(new NullProgressMonitor());
		} catch (InterruptedException e) {
			Utils.handle(e);
		} catch (InvocationTargetException e) {
			Utils.handle(e);
		}
		CompareDialog dialog = createCompareDialog(getShell(), participant.getName(), input);
		dialog.setSynchronizeParticipant(participant);
		dialog.setBlockOnOpen(true);
		dialog.open();
	}
	
	protected CompareDialog createCompareDialog(Shell shell, String title, CompareEditorInput input) {
		return new CompareDialog(shell, title, input);
	}
}
