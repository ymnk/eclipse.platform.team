package org.eclipse.team.internal.ui.jobs;

import java.util.*;

import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.ui.synchronize.SyncInfoSet;
import org.eclipse.team.ui.synchronize.SyncInfoCollector;

class RefreshChangeListener implements ITeamResourceChangeListener {
	private List changes = new ArrayList();
	private SyncInfoCollector collector;

	RefreshChangeListener(SyncInfoCollector collector) {
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
		collector.waitForCollector();
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
