package org.eclipse.team.internal.ui.jobs;

import java.util.*;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.core.subscribers.*;

class RefreshChangeListener implements ITeamResourceChangeListener {
	private List changes = new ArrayList();
	private TeamSubscriberSyncInfoCollector collector;

	RefreshChangeListener(TeamSubscriberSyncInfoCollector collector) {
		this.collector = collector;
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
		collector.waitForCollector(new NullProgressMonitor());
		List changedSyncInfos = new ArrayList();
		SyncInfoSet set = collector.getSyncInfoSet();
		for (Iterator it = changes.iterator(); it.hasNext();) {
			TeamDelta delta = (TeamDelta) it.next();
			SyncInfo info = set.getSyncInfo(delta.getResource());
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
