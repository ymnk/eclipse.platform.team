package org.eclipse.team.internal.ui.jobs;

import java.util.*;

import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.internal.ui.synchronize.sets.SubscriberInput;

class RefreshChangeListener implements ITeamResourceChangeListener {
	private List changes = new ArrayList();
	private SubscriberInput input;

	RefreshChangeListener(SubscriberInput input) {
		this.input = input;
	}
	public void teamResourceChanged(TeamDelta[] deltas) {
		for (int i = 0; i < deltas.length; i++) {
			TeamDelta delta = deltas[i];
			if (delta.getFlags() == TeamDelta.SYNC_CHANGED) {
				changes.add(delta);
			}
		}
	}
	public SyncInfo[] getChanges() {
		try {
			// wait for inputs to stop processing changes
			if (input instanceof SubscriberInput) {
				((SubscriberInput) input).getEventHandler().getEventHandlerJob().join();
			}
		} catch (InterruptedException e) {
			// continue
		}
		List changedSyncInfos = new ArrayList();
		for (Iterator it = changes.iterator(); it.hasNext();) {
			TeamDelta delta = (TeamDelta) it.next();
			SyncInfo info = input.getSubscriberSyncSet().getSyncInfo(delta.getResource());
			if (info != null) {
				int direction = info.getKind() & SyncInfo.DIRECTION_MASK;
				if (direction == SyncInfo.INCOMING || direction == SyncInfo.CONFLICTING) {
					changedSyncInfos.add(info);
				}
			}
		}
		return (SyncInfo[]) changedSyncInfos.toArray(new SyncInfo[changedSyncInfos.size()]);
	}

	public void clear() {
		changes.clear();
	}
}
