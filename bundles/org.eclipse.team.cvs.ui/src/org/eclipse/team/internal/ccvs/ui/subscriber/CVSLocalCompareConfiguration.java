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
package org.eclipse.team.internal.ccvs.ui.subscriber;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.team.ui.synchronize.DiffTreeViewerConfiguration;
import org.eclipse.team.ui.synchronize.SyncInfoDiffNode;
import org.eclipse.team.ui.synchronize.actions.RefreshAction;

/**
 * Provides compare specific support
 */
public class CVSLocalCompareConfiguration extends DiffTreeViewerConfiguration {

	private CVSCompareSubscriber subscriber;
	private TeamSubscriberSyncInfoCollector collector;
	private RefreshAction refreshAction;
	private RefreshAction refreshAllAction;
	private FilteredSyncInfoCollector filteredSyncSet;

	/**
	 * Return a <code>SyncInfoSetCompareConfiguration</code> that can be used in a
	 * <code>SyncInfoSetCompareInput</code> to show the comparsion between the local
	 * workspace resources and their tagged counterparts on the server.
	 * @param resources the resources to be compared
	 * @param tag the tag to be compared with
	 * @return a configuration for a <code>SyncInfoSetCompareInput</code>
	 */
	public static CVSLocalCompareConfiguration create(IResource[] resources, CVSTag tag) {
		CVSCompareSubscriber subscriber = new CVSCompareSubscriber(resources, tag);
		TeamSubscriberSyncInfoCollector collector = new TeamSubscriberSyncInfoCollector(subscriber);
		return new CVSLocalCompareConfiguration(subscriber, collector);
	}
	
	public CVSLocalCompareConfiguration(CVSCompareSubscriber subscriber, TeamSubscriberSyncInfoCollector collector) {
		super("org.eclipse.team.cvs.ui.compare-participant", collector.getSyncInfoSet()); //$NON-NLS-1$
		this.subscriber = subscriber;
		this.collector = collector;
		this.filteredSyncSet = new FilteredSyncInfoCollector(collector.getSyncInfoSet(), null, new SyncInfoFilter() {
			private SyncInfoFilter contentCompare = new SyncInfoFilter.ContentComparisonSyncInfoFilter();
			public boolean select(SyncInfo info, IProgressMonitor monitor) {
				// Want to select infos whose contents do not match
				return !contentCompare.select(info, monitor);
			}
		});
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncInfoSetCompareConfiguration#dispose()
	 */
	public void dispose() {
		filteredSyncSet.dispose();
		collector.dispose();
		subscriber.dispose();
		super.dispose();
	}
	
	public SyncInfoDiffNode prepareInput(IProgressMonitor monitor) throws TeamException {
		subscriber.refresh(subscriber.roots(), IResource.DEPTH_INFINITE, monitor);
		collector.waitForCollector(monitor);
		return getInput();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncInfoSetCompareConfiguration#fillContextMenu(org.eclipse.jface.viewers.StructuredViewer, org.eclipse.jface.action.IMenuManager)
	 */
	protected void fillContextMenu(StructuredViewer viewer, IMenuManager manager) {
		manager.add(refreshAction);
		manager.add(new Separator());
		super.fillContextMenu(viewer, manager);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.DiffTreeViewerConfiguration#getInput()
	 */
	protected SyncInfoDiffNodeRoot getInput() {
		return new ChangeLogDiffNodeRoot(getSyncSet());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.DiffTreeViewerConfiguration#contributeToToolBar(org.eclipse.jface.action.IToolBarManager)
	 */
	public void contributeToToolBar(IToolBarManager tbm) {
		tbm.add(refreshAllAction);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncInfoSetCompareConfiguration#initializeViewer(org.eclipse.swt.widgets.Composite, org.eclipse.jface.viewers.StructuredViewer)
	 */
	public void initializeViewer(Composite parent, StructuredViewer viewer) {
		super.initializeViewer(parent, viewer);
	}

	protected void initializeActions(StructuredViewer viewer) {
		super.initializeActions(viewer);
		refreshAction = new RefreshAction(viewer, ((CVSSyncTreeSubscriber)collector.getTeamSubscriber()).getName(), collector, null /* no listener */, false);
		refreshAllAction = new RefreshAction(viewer, ((CVSSyncTreeSubscriber)collector.getTeamSubscriber()).getName(), collector, null /* no listener */, true);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncInfoSetCompareConfiguration#getSyncSet()
	 */
	public SyncInfoSet getSyncSet() {
		return filteredSyncSet.getSyncInfoSet();
	}
}
