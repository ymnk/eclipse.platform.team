package org.eclipse.team.ui.sync;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.core.subscribers.TeamSubscriber;
import org.eclipse.team.internal.ui.sync.pages.*;
import org.eclipse.team.internal.ui.sync.sets.SubscriberInput;
import org.eclipse.ui.part.IPageBookViewPage;

public class SubscriberPage extends AbstractSynchronizeViewPage {

	private TeamSubscriber subscriber;
	private SubscriberSynchronizeViewPage page;
	
	public SubscriberPage(TeamSubscriber subscriber, String name, ImageDescriptor imageDescriptor) {
		super(name, imageDescriptor);
		this.subscriber = subscriber;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.ISynchronizeViewPage#createPage(org.eclipse.team.ui.sync.ISynchronizeView)
	 */
	public IPageBookViewPage createPage(INewSynchronizeView view) {
		this.page = new SubscriberSynchronizeViewPage(subscriber, view);
		return page;
	}
	
	public SubscriberInput getInput() {
		return page.getInput();
	}
}
