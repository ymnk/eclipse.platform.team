package org.eclipse.team.internal.ui.jobs;

import java.util.*;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.core.synchronize.*;

class RefreshChangeListener implements ISubscriberChangeListener {
	private List changes = new ArrayList();
	private SubscriberSyncInfoCollector collector;

	RefreshChangeListener(SubscriberSyncInfoCollector collector) {
		this.collector = collector;
	}
	public void subscriberResourceChanged(ISubscriberChangeEvent[] deltas) {
		for (int i = 0; i < deltas.length; i++) {
			ISubscriberChangeEvent delta = deltas[i];
			if (delta.getFlags() == ISubscriberChangeEvent.SYNC_CHANGED) {
				changes.add(delta);
			}
		}
	}
	public SyncInfo[] getChanges() {
		collector.waitForCollector(new NullProgressMonitor());
		List changedSyncInfos = new ArrayList();
		SyncInfoSet set = collector.getSyncInfoTree();
		for (Iterator it = changes.iterator(); it.hasNext();) {
			ISubscriberChangeEvent delta = (ISubscriberChangeEvent) it.next();
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
