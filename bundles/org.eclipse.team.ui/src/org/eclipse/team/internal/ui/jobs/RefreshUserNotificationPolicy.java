package org.eclipse.team.internal.ui.jobs;

import java.util.*;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.internal.ui.IPreferenceIds;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.synchronize.RefreshCompleteDialog;
import org.eclipse.team.internal.ui.synchronize.sets.SubscriberInput;
import org.eclipse.team.ui.synchronize.ITeamSubscriberSyncInfoSets;
import org.eclipse.team.ui.synchronize.TeamSubscriberParticipant;

public class RefreshUserNotificationPolicy implements IRefreshSubscriberListener {
	
	protected class ChangeListener implements ITeamResourceChangeListener {
		private List changes = new ArrayList();
		private ITeamSubscriberSyncInfoSets input;
		ChangeListener(ITeamSubscriberSyncInfoSets input) {
			this.input = input;
		}
		public void teamResourceChanged(TeamDelta[] deltas) {
			for (int i = 0; i < deltas.length; i++) {
				TeamDelta delta = deltas[i];
				if(delta.getFlags() == TeamDelta.SYNC_CHANGED) {
					changes.add(delta);
				}
			}
		}
		public SyncInfo[] getChanges() {
			try {
				// wait for inputs to stop processing changes
				if(input instanceof SubscriberInput) {
					((SubscriberInput)input).getEventHandler().getEventHandlerJob().join();
				}
			} catch (InterruptedException e) {
				// continue
			}
			List changedSyncInfos = new ArrayList();
			for (Iterator it = changes.iterator(); it.hasNext(); ) {
				TeamDelta delta = (TeamDelta) it.next();
				SyncInfo info = input.getSubscriberSyncSet().getSyncInfo(delta.getResource());
				if(info != null) {
					int direction = info.getKind() & SyncInfo.DIRECTION_MASK;
					if(direction == SyncInfo.INCOMING || direction == SyncInfo.CONFLICTING) {
						changedSyncInfos.add(info);
					}
				}
			}
			return (SyncInfo[]) changedSyncInfos.toArray(new SyncInfo[changedSyncInfos.size()]);
		}
		
		public ITeamSubscriberSyncInfoSets getInput() {
			return input;
		}
		
		public void clear() {
			changes.clear();
		}
	}
	
	private ChangeListener changeListener;
	
	public RefreshUserNotificationPolicy() {		
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.jobs.IRefreshSubscriberListener#refreshStarted(org.eclipse.team.internal.ui.jobs.IRefreshEvent)
	 */
	public void refreshStarted(IRefreshEvent event) {
		TeamSubscriberParticipant participant = event.getParticipant();
		changeListener = new ChangeListener(participant.getInput());		
		participant.getInput().getSubscriber().addListener(changeListener);		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.jobs.IRefreshSubscriberListener#refreshDone(org.eclipse.team.internal.ui.jobs.IRefreshEvent)
	 */
	public void refreshDone(IRefreshEvent event) {
		int type = event.getRefreshType();
		
		boolean promptWithChanges = TeamUIPlugin.getPlugin().getPreferenceStore().getBoolean(IPreferenceIds.SYNCVIEW_VIEW_PROMPT_WITH_CHANGES);
		boolean promptWhenNoChanges = TeamUIPlugin.getPlugin().getPreferenceStore().getBoolean(IPreferenceIds.SYNCVIEW_VIEW_PROMPT_WHEN_NO_CHANGES);
		boolean promptWithChangesBkg = TeamUIPlugin.getPlugin().getPreferenceStore().getBoolean(IPreferenceIds.SYNCVIEW_VIEW_BKG_PROMPT_WITH_CHANGES);
		boolean promptWhenNoChangesBkg = TeamUIPlugin.getPlugin().getPreferenceStore().getBoolean(IPreferenceIds.SYNCVIEW_VIEW_BKG_PROMPT_WHEN_NO_CHANGES);
		
		boolean shouldPrompt = false;	
		SyncInfo[] infos  = changeListener.getChanges();
		
		if(type == IRefreshEvent.USER_REFRESH) {
			if(promptWhenNoChanges && infos.length == 0) {
				shouldPrompt = true;
			} else if(promptWithChanges && infos.length > 0) {
				shouldPrompt = true;
			}
		} else {
			if(promptWhenNoChangesBkg && infos.length == 0) {
				shouldPrompt = true;
			} else if(promptWithChangesBkg && infos.length > 0) {
				shouldPrompt = true;
			}
		}
		
		if(shouldPrompt) {
			notifyIfNeeded(changeListener, infos, event.getRefreshType());
		}
		changeListener.clear();
	}
	
	private void notifyIfNeeded(ChangeListener listener, final SyncInfo[] infos, final int type) {
		TeamUIPlugin.getStandardDisplay().asyncExec(new Runnable() {
			public void run() {
				RefreshCompleteDialog d = new RefreshCompleteDialog(
						new Shell(TeamUIPlugin.getStandardDisplay()), type, infos, new  ITeamSubscriberSyncInfoSets[] {changeListener.getInput()});
				d.setBlockOnOpen(false);
				d.open();
			}
		});
	}
}