package org.eclipse.team.ui.sync;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.core.subscribers.TeamSubscriber;
import org.eclipse.team.internal.ui.sync.pages.*;
import org.eclipse.ui.part.IPageBookViewPage;

public class SubscriberPage extends AbstractSynchronizeViewPage {

	private TeamSubscriber subscriber;
	
	public SubscriberPage(TeamSubscriber subscriber, String name, ImageDescriptor imageDescriptor) {
		super(name, imageDescriptor);
		this.subscriber = subscriber;
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.ISynchronizeViewPage#createPage(org.eclipse.team.ui.sync.ISynchronizeView)
	 */
	public IPageBookViewPage createPage(INewSynchronizeView view) {
		// TODO Auto-generated method stub
		return new SubscriberSynchronizeViewPage(subscriber, view);
	}
}
