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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.sync.SyncInfo;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.repo.RepositoryManager;
import org.eclipse.team.internal.ui.sync.views.SyncResource;
import org.eclipse.team.ui.sync.SyncResourceSet;

/**
 * This action performs the update for the CVS workspace subscriber
 */
public class SubscriberUpdateAction extends CVSSubscriberAction {

	// used to indicate how conflicts are to be updated
	private boolean onlyUpdateAutomergeable;

	public static class ConfirmDialog extends MessageDialog {

		private boolean autoMerge = true;
		private Button radio1;
		private Button radio2;
	
		public ConfirmDialog(Shell parentShell) {
			super(
				parentShell, 
				Policy.bind("UpdateSyncAction.Conflicting_changes_found_1"),  //$NON-NLS-1$
				null,	// accept the default window icon
				Policy.bind("UpdateSyncAction.You_have_local_changes_you_are_about_to_overwrite_2"), //$NON-NLS-1$
				MessageDialog.QUESTION, 
				new String[] {IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL},
				0); 	// yes is the default
		}
	
		protected Control createCustomArea(Composite parent) {
			Composite composite = new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout());
			radio1 = new Button(composite, SWT.RADIO);
			radio1.addSelectionListener(selectionListener);
		
			radio1.setText(Policy.bind("UpdateSyncAction.Only_update_resources_that_can_be_automatically_merged_3")); //$NON-NLS-1$

			radio2 = new Button(composite, SWT.RADIO);
			radio2.addSelectionListener(selectionListener);

			radio2.setText(Policy.bind("UpdateSyncAction.Update_all_resources,_overwriting_local_changes_with_remote_contents_4")); //$NON-NLS-1$
		
			// set initial state
			radio1.setSelection(autoMerge);
			radio2.setSelection(!autoMerge);
		
			return composite;
		}
	
		private SelectionListener selectionListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Button button = (Button)e.widget;
				if (button.getSelection()) {
					autoMerge = (button == radio1);
				}
			}
		};
	
		public boolean getAutomerge() {
			return autoMerge;
		}
	}

	protected boolean performPrompting(SyncResourceSet syncSet) {
		// If there are conflicts or outgoing changes in the syncSet, we need to warn the user.
		onlyUpdateAutomergeable = false;
		if (syncSet.hasConflicts() || syncSet.hasOutgoingChanges()) {
			if (syncSet.hasAutoMergeableConflicts()) {
				switch (promptForMergeableConflicts()) {
					case 0: // cancel
						return false;
					case 1: // only update auto-mergeable conflicts
						onlyUpdateAutomergeable = true;
						syncSet.removeNonMergeableNodes();
						break;
					case 2: // update all conflicts
						onlyUpdateAutomergeable = false;
						break;
				}				
			} else {
				if (! promptForConflicts()) return false;				
			}
		}
		return true;
	}
	
	protected void run(SyncResourceSet syncSet, IProgressMonitor monitor) {
// TODO: Saving can change the sync state! How should this be handled?
//		boolean result = saveIfNecessary();
//		if (!result) return null;
	
		if (!performPrompting(syncSet)) return;
	
		SyncResource[] changed = syncSet.getSyncResources();
		if (changed.length == 0) return;
		
		// The list of sync resources to be updated using "cvs update"
		List updateShallow = new ArrayList();
		// A list of sync resource folders which need to be created locally 
		// (incoming addition or previously pruned)
		Set parentCreationElements = new HashSet();
		// A list of sync resources that are incoming deletions.
		// We do these first to avoid case conflicts
		List updateDeletions = new ArrayList();
		// A list of sync resources that need to be unmanaged and locally deleted
		// Note: This list is also used to unmanaged outgoing deletions
		// and to remove conflict local changes when override conflicts is chosen
		List deletions = new ArrayList();
	
		for (int i = 0; i < changed.length; i++) {
			SyncResource changedNode = changed[i];
			SyncResource parent = changedNode.getParent();
			
			// Make sure that parent folders exist
			if (parent != null && !parent.getResource().exists()) {
				// We need to ensure that parents that are either incoming folder additions
				// or previously pruned folders are recreated.
				parentCreationElements.add(parent);
			}
			
			IResource resource = changedNode.getResource();
			int kind = changedNode.getKind();
			
			if (resource.getType() == IResource.FILE) {
				// Not all change types will require a "cvs update"
				// Identify cases that do not require an "cvs update"
				// Also, for outgoing and conflicting changes indicate whether
				// the existing local resource should be deleted before the 
				// update occurs
				updateShallow.add(changedNode);
				switch (kind & SyncInfo.DIRECTION_MASK) {
					case SyncInfo.INCOMING:
						switch (kind & SyncInfo.CHANGE_MASK) {
							case SyncInfo.DELETION:
								updateDeletions.add(changedNode);
								updateShallow.remove(changedNode);
								break;
						}
						break;
					case SyncInfo.OUTGOING:
						deletions.add(changedNode);
						switch (kind & SyncInfo.CHANGE_MASK) {
							case SyncInfo.ADDITION:
								updateShallow.remove(changedNode);
								break;
						}
						break;
					case SyncInfo.CONFLICTING:
						deletions.add(changedNode);	
						switch (kind & SyncInfo.CHANGE_MASK) {
							case SyncInfo.DELETION:
								updateShallow.remove(changedNode);
								break;
							case SyncInfo.CHANGE:
								if (supportsShallowUpdateFor(changedNode)) {
									// Don't delete the local resource since the
									// action can accomodate the shallow update
									deletions.remove(changedNode);
								}
								break;
						}
						break;
				}
			} else {
				// Special handling for folders to support shallow operations on files
				// (i.e. folder operations are performed using the sync info already
				// contained in the sync info.
				if (!resource.exists()) {
					parentCreationElements.add(parent);
				} else if (((kind & SyncInfo.DIRECTION_MASK) == SyncInfo.OUTGOING)
						&& ((kind & SyncInfo.CHANGE_MASK) == SyncInfo.ADDITION)) {
					// The folder is an outgoing addition which is being overridden
					// Add it to the list of resources to be deleted
					
					// TODO: Do we need to do anything special here or will the
					// operation do the pruning? If we do need to do soemthing,
					// it probably needs to be different then the below line
					// since this will not keep the history for the folder's
					// childen.
					//deletions.add(changedNode);
				}
			}

		}
		try {
			// Calculate the total amount of work needed
			int work = (deletions.size() + updateDeletions.size() + updateShallow.size()) * 100;
			monitor.beginTask(null, work);

			RepositoryManager manager = CVSUIPlugin.getPlugin().getRepositoryManager();

			if (parentCreationElements.size() > 0) {
				handleFolderCreations((SyncResource[]) parentCreationElements.toArray(new SyncResource[parentCreationElements.size()]));				
			}
			if (deletions.size() > 0) {
				runLocalDeletions((SyncResource[])deletions.toArray(new SyncResource[deletions.size()]), manager, Policy.subMonitorFor(monitor, deletions.size() * 100));
			}
			if (updateDeletions.size() > 0) {
				runUpdateDeletions((SyncResource[])updateDeletions.toArray(new SyncResource[updateDeletions.size()]), manager, Policy.subMonitorFor(monitor, updateDeletions.size() * 100));
			}			
			if (updateShallow.size() > 0) {
				runUpdateShallow((SyncResource[])updateShallow.toArray(new SyncResource[updateShallow.size()]), manager, Policy.subMonitorFor(monitor, updateShallow.size() * 100));
			}
		} catch (final TeamException e) {
			handle(e);
			return;
		} finally {
			monitor.done();
		}
		return;
	}

	/**
	 * Method which indicates whether a shallow update will work for the given
	 * node which is an outgoing or conflicting change. The default is to return true 
	 * for conflicting changes that are automergable if the user has chosen the 
	 * appropriate operation.
	 * 
	 * @param changedNode
	 * @return
	 */
	protected boolean supportsShallowUpdateFor(SyncResource changedNode) {
		return (changedNode.getChangeDirection() == SyncInfo.CONFLICTING
			&& ((changedNode.getKind() & SyncInfo.CHANGE_MASK) == SyncInfo.CHANGE)
		 	&& onlyUpdateAutomergeable 
		 	&& (changedNode.getKind() & SyncInfo.AUTOMERGE_CONFLICT) != 0);
	}

	protected void handleFolderCreations(SyncResource[] folders) throws TeamException {
		// If a node has a parent that is an incoming folder creation, we have to 
		// create that folder locally and set its sync info before we can get the
		// node itself. We must do this for all incoming folder creations (recursively)
		// in the case where there are multiple levels of incoming folder creations.
		for (int i = 0; i < folders.length; i++) {
			SyncResource resource = folders[i];
			makeInSync(resource);
		}
	}

	/**
	 * @param element
	 */
	protected void unmanage(SyncResource element, IProgressMonitor monitor) throws CVSException {
		CVSWorkspaceRoot.getCVSResourceFor(element.getResource()).unmanage(monitor);
		
	}

	/**
	 * Method deleteAndKeepHistory.
	 * @param iResource
	 * @param iProgressMonitor
	 */
	protected void deleteAndKeepHistory(IResource resource, IProgressMonitor monitor) throws CVSException {
		try {
			if (!resource.exists()) return;
			if (resource.getType() == IResource.FILE)
				((IFile)resource).delete(false /* force */, true /* keep history */, monitor);
			else
				resource.delete(false /* force */, monitor);
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}
	
	protected void runLocalDeletions(SyncResource[] nodes, RepositoryManager manager, IProgressMonitor monitor) throws TeamException {
		monitor.beginTask(null, nodes.length * 100);
		for (int i = 0; i < nodes.length; i++) {
			SyncResource node = nodes[i];
			unmanage(node, Policy.subMonitorFor(monitor, 50));
			deleteAndKeepHistory(node.getResource(), Policy.subMonitorFor(monitor, 50));
		}
		monitor.done();
	}

	protected void runUpdateDeletions(SyncResource[] nodes, RepositoryManager manager, IProgressMonitor monitor) throws TeamException {
		// As an optimization, just perform the deletions locally
		runLocalDeletions(nodes, manager, monitor);
	}

	protected void runUpdateShallow(SyncResource[] nodes, RepositoryManager manager, IProgressMonitor monitor) throws TeamException {
		manager.update(getIResourcesFrom(nodes), new Command.LocalOption[] { Command.DO_NOT_RECURSE }, false, monitor);
	}
	
	protected IResource[] getIResourcesFrom(SyncResource[] nodes) {
		List resources = new ArrayList(nodes.length);
		for (int i = 0; i < nodes.length; i++) {
			resources.add(nodes[i].getResource());
		}
		return (IResource[]) resources.toArray(new IResource[resources.size()]);
	}

	/**
	 * Prompt for mergeable conflicts.
	 * Note: This method is designed to be overridden by test cases.
	 * @return 0 to cancel, 1 to only update mergeable conflicts, 2 to overwrite if unmergeable
	 */
	protected int promptForMergeableConflicts() {
		final boolean doAutomerge[] = new boolean[] {false};
		final int[] result = new int[] {Dialog.CANCEL};
		final Shell shell = getShell();
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				ConfirmDialog dialog = new ConfirmDialog(shell);
				result[0] = dialog.open();
				doAutomerge[0] = dialog.getAutomerge();
			}
		});
		if (result[0] == Dialog.CANCEL) return 0;
		return doAutomerge[0] ? 1 : 2;
	}

	/**
	 * Prompt for non-automergeable conflicts.
	 * Note: This method is designed to be overridden by test cases.
	 * @return false to cancel, true to overwrite local changes
	 */
	protected boolean promptForConflicts() {
		final boolean[] result = new boolean[] { false };
		final Shell shell = getShell();
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				result[0] = MessageDialog.openQuestion(shell, Policy.bind("UpdateSyncAction.Overwrite_local_changes__5"), Policy.bind("UpdateSyncAction.You_have_local_changes_you_are_about_to_overwrite._Do_you_wish_to_continue__6")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		return result[0];
	}

}
