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
package org.eclipse.team.internal.ui.synchronize.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.ITeamStatus;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.ISharedImages;
import org.eclipse.team.ui.synchronize.subscriber.TeamSubscriberParticipant;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionGroup;

public class StatusLineContributionGroup extends ActionGroup implements ISyncInfoSetChangeListener {

	private static final String INCOMING_ID = TeamUIPlugin.ID + "org.eclipse.team.iu.statusline.incoming"; //$NON-NLS-1$
	private static final String OUTGOING_ID = TeamUIPlugin.ID + "org.eclipse.team.iu.statusline.outgoing"; //$NON-NLS-1$
	private static final String CONFLICTING_ID = TeamUIPlugin.ID + "org.eclipse.team.iu.statusline.conflicting"; //$NON-NLS-1$

	private StatusLineCLabelContribution incoming;
	private StatusLineCLabelContribution outgoing;
	private StatusLineCLabelContribution conflicting;
	
	private Image incomingImage = TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_DLG_SYNC_INCOMING).createImage();
	private Image outgoingImage = TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_DLG_SYNC_OUTGOING).createImage();
	private Image conflictingImage = TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_DLG_SYNC_CONFLICTING).createImage();
	
	private SubscriberSyncInfoCollector collector;
	private TeamSubscriberParticipant participant;

	public StatusLineContributionGroup(final Shell shell, TeamSubscriberParticipant participant) {
		super();
		this.participant = participant;
		this.collector = participant.getSubscriberSyncInfoCollector();
		this.incoming = createStatusLineContribution(INCOMING_ID, TeamSubscriberParticipant.INCOMING_MODE, "0", incomingImage); //$NON-NLS-1$
		this.outgoing = createStatusLineContribution(OUTGOING_ID, TeamSubscriberParticipant.OUTGOING_MODE, "0", outgoingImage); //$NON-NLS-1$
		this.conflicting = createStatusLineContribution(CONFLICTING_ID, TeamSubscriberParticipant.CONFLICTING_MODE, "0", conflictingImage); //$NON-NLS-1$
		collector.getSyncInfoTree().addSyncSetChangedListener(this);
	}

	private StatusLineCLabelContribution createStatusLineContribution(String id, final int mode, String label, Image image) {
		StatusLineCLabelContribution item = new StatusLineCLabelContribution(id, 15);
		item.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				participant.setMode(mode);
			}
		});
		item.setText(label); //$NON-NLS-1$
		item.setImage(image);
		return item;
	}

	public void dispose() {
		collector.getSyncInfoTree().removeSyncSetChangedListener(this);
		incomingImage.dispose();
		outgoingImage.dispose();
		conflictingImage.dispose();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.internal.ui.sync.sets.ISyncSetChangedListener#syncSetChanged(org.eclipse.team.internal.ui.sync.sets.SyncSetChangedEvent)
	 */
	public void syncInfoChanged(ISyncInfoSetChangeEvent event, IProgressMonitor monitor) {
		updateCounts();
	}

	private void updateCounts() {
		if (collector != null) {
			SyncInfoSet set = collector.getSyncInfoTree();
			final int workspaceConflicting = (int) set.countFor(SyncInfo.CONFLICTING, SyncInfo.DIRECTION_MASK);
			final int workspaceOutgoing = (int) set.countFor(SyncInfo.OUTGOING, SyncInfo.DIRECTION_MASK);
			final int workspaceIncoming = (int) set.countFor(SyncInfo.INCOMING, SyncInfo.DIRECTION_MASK);

			TeamUIPlugin.getStandardDisplay().asyncExec(new Runnable() {
				public void run() {
						conflicting.setText(new Integer(workspaceConflicting).toString()); //$NON-NLS-1$
						incoming.setText(new Integer(workspaceIncoming).toString()); //$NON-NLS-1$
						outgoing.setText(new Integer(workspaceOutgoing).toString()); //$NON-NLS-1$

						conflicting.setTooltip(Policy.bind("StatisticsPanel.numbersTooltip", Policy.bind("StatisticsPanel.conflicting"))); //$NON-NLS-1$ //$NON-NLS-2$
						outgoing.setTooltip(Policy.bind("StatisticsPanel.numbersTooltip", Policy.bind("StatisticsPanel.outgoing"))); //$NON-NLS-1$ //$NON-NLS-2$
						incoming.setTooltip(Policy.bind("StatisticsPanel.numbersTooltip", Policy.bind("StatisticsPanel.incoming"))); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.actions.ActionGroup#fillActionBars(org.eclipse.ui.IActionBars)
	 */
	public void fillActionBars(IActionBars actionBars) {
		IStatusLineManager mgr = actionBars.getStatusLineManager();
		mgr.add(incoming);
		mgr.add(outgoing);
		mgr.add(conflicting);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.ISyncInfoSetChangeListener#syncInfoSetReset(org.eclipse.team.core.subscribers.SyncInfoSet, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void syncInfoSetReset(SyncInfoSet set, IProgressMonitor monitor) {
		updateCounts();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.ISyncInfoSetChangeListener#syncInfoSetError(org.eclipse.team.core.subscribers.SyncInfoSet, org.eclipse.team.core.ITeamStatus[], org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void syncInfoSetErrors(SyncInfoSet set, ITeamStatus[] errors, IProgressMonitor monitor) {
		// Nothing to do for errors
	}
}