package org.eclipse.team.ui.sync;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.core.subscribers.TeamSubscriber;
import org.eclipse.team.internal.ui.IPreferenceIds;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.sync.pages.SubscriberSynchronizeViewPage;
import org.eclipse.team.internal.ui.sync.sets.SubscriberInput;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.part.IPageBookViewPage;

public class SubscriberPage extends AbstractSynchronizeViewPage {
	
	protected TeamSubscriber subscriber;
	protected SubscriberSynchronizeViewPage page;
	protected int currentMode;
	
	/**
	 * Property constant indicating the mode of a page has changed. 
	 */
	public static final String P_SYNCVIEWPAGE_WORKINGSET = TeamUIPlugin.ID  + ".P_SYNCVIEWPAGE_WORKINGSET";	 //$NON-NLS-1$
	
	/**
	 * Property constant indicating the mode of a page has changed. 
	 */
	public static final String P_SYNCVIEWPAGE_MODE = TeamUIPlugin.ID  + ".P_SYNCVIEWPAGE_MODE";	 //$NON-NLS-1$
	
	/**
	 * Modes are direction filters for the view
	 */
	public final static int INCOMING_MODE = 1;
	public final static int OUTGOING_MODE = 2;
	public final static int BOTH_MODE = 3;
	public final static int CONFLICTING_MODE = 4;
	
	/**
	 * Property constant indicating the mode of a page has changed. 
	 */
	public static final String P_SYNCVIEWPAGE_LAYOUT = TeamUIPlugin.ID  + ".P_SYNCVIEWPAGE_LAYOUT";	 //$NON-NLS-1$
	
	/**
	 * View type constant (value 0) indicating that the synchronize view will be shown
	 * as a tree.
	 */
	public static final int TREE_LAYOUT = 0;
	
	/**
	 * View type constant (value 1) indicating that the synchronize view will be shown
	 * as a table.
	 */
	public static final int TABLE_LAYOUT = 1;
	
	public SubscriberPage(TeamSubscriber subscriber, String name, ImageDescriptor imageDescriptor) {
		super(name, imageDescriptor);
		this.subscriber = subscriber;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.ISynchronizeViewPage#createPage(org.eclipse.team.ui.sync.ISynchronizeView)
	 */
	public IPageBookViewPage createPage(INewSynchronizeView view) {
		this.page = new SubscriberSynchronizeViewPage(this, view);
		return page;
	}
	
	public SubscriberInput getInput() {
		return page.getInput();
	}
	
	public void setMode(int mode) {
		int oldMode = getMode();
		currentMode = mode;
		TeamUIPlugin.getPlugin().getPreferenceStore().setValue(IPreferenceIds.SYNCVIEW_SELECTED_MODE, mode);
		firePropertyChange(this, P_SYNCVIEWPAGE_MODE, new Integer(oldMode), new Integer(mode));
	}
	
	public int getMode() {
		return currentMode;
	}
	
	public void setLayout(int layout) {
		firePropertyChange(this, P_SYNCVIEWPAGE_LAYOUT, new Integer(page.getLayout()), new Integer(layout));
	}
	
	public int getLayout() {
		return page.getLayout();		
	}
	
	public void setWorkingSet(IWorkingSet set) {
		firePropertyChange(this, P_SYNCVIEWPAGE_WORKINGSET, null, set);
	}
	
	public TeamSubscriber getSubscriber() {
		return subscriber;
	}
	
	public void setActionsBars(IActionBars actionBars) {		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.AbstractSynchronizeViewPage#dispose()
	 */
	protected void dispose() {
		super.dispose();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.AbstractSynchronizeViewPage#init()
	 */
	protected void init() {
		super.init();
	}
}
