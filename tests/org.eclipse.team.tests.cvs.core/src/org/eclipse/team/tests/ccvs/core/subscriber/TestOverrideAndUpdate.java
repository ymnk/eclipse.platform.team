package org.eclipse.team.tests.ccvs.core.subscriber;

import org.eclipse.team.core.subscribers.MutableSyncInfoSet;
import org.eclipse.team.internal.ccvs.ui.subscriber.OverrideAndUpdateAction;

public class TestOverrideAndUpdate extends OverrideAndUpdateAction {	
	
	private boolean prompted = false; 
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.subscriber.SafeUpdateAction#promptForOverwrite(org.eclipse.team.ui.sync.SyncInfoSet)
	 */
	protected boolean promptForOverwrite(MutableSyncInfoSet syncSet) {
		this.prompted = true;
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.subscriber.SafeUpdateAction#warnAboutFailedResources(org.eclipse.team.ui.sync.SyncInfoSet)
	 */
	protected void warnAboutFailedResources(MutableSyncInfoSet syncSet) {
		return;
	}

	public boolean isPrompted() {
		return this.prompted;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.subscriber.CVSSubscriberAction#canRunAsJob()
	 */
	protected boolean canRunAsJob() {
		return false;
	}
}
