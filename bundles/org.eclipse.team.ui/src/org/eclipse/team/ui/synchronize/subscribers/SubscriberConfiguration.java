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
package org.eclipse.team.ui.synchronize.subscribers;

import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.team.core.subscribers.WorkingSetFilteredSyncInfoCollector;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.synchronize.SyncInfoModelElement;
import org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration;
import org.eclipse.team.internal.ui.synchronize.actions.*;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.*;


/**
 * A <code>SynchronizeConfiguration</code> object controls various UI aspects of 
 * synchronization viewers.
 * <p>
 * Clients may use this class as is, or subclass to add new state and behavior.
 * </p>
 * @since 3.0
 */
public class SubscriberConfiguration extends SynchronizePageConfiguration {
	
	/**
	 * Property constant for the working set used to filter the visible
	 * elements of the model. The value can be any <code>IWorkingSet</code>
	 * or <code>null</code>;
	 */
	public static final String P_SYNCVIEWPAGE_WORKINGSET = TeamUIPlugin.ID  + ".P_SYNCVIEWPAGE_WORKINGSET";	 //$NON-NLS-1$
	
	/**
	 * Property constant for the mode used to filter the visible
	 * elements of the model. The value can be one of the mode integer
	 * constants.
	 */
	public static final String P_SYNCVIEWPAGE_MODE = TeamUIPlugin.ID  + ".P_SYNCVIEWPAGE_MODE";	 //$NON-NLS-1$
	
	/**
	 * Property constant which indicates which modes are to be available to the user.
	 * The value is to be an integer that combines one or more of the
	 * mode bit values.
	 * Either <code>null</code> or <code>0</code> can be used to indicate that
	 * mode filtering is not supported.
	 */
	public static final String P_SUPPORTED_MODES = TeamUIPlugin.ID  + ".P_SUPPORTED_MODES";	 //$NON-NLS-1$
	
	/**
	 * Modes are direction filters for the view
	 */
	public final static int INCOMING_MODE = 0x1;
	public final static int OUTGOING_MODE = 0x2;
	public final static int BOTH_MODE = 0x4;
	public final static int CONFLICTING_MODE = 0x8;
	public final static int ALL_MODES = INCOMING_MODE | OUTGOING_MODE | CONFLICTING_MODE | BOTH_MODE;

	private final static int[] INCOMING_MODE_FILTER = new int[] {SyncInfo.CONFLICTING, SyncInfo.INCOMING};
	private final static int[] OUTGOING_MODE_FILTER = new int[] {SyncInfo.CONFLICTING, SyncInfo.OUTGOING};
	private final static int[] BOTH_MODE_FILTER = new int[] {SyncInfo.CONFLICTING, SyncInfo.INCOMING, SyncInfo.OUTGOING};
	private final static int[] CONFLICTING_MODE_FILTER = new int[] {SyncInfo.CONFLICTING};

	/**
	 * Filters out-of-sync resources by working set and mode
	 */
	private WorkingSetFilteredSyncInfoCollector collector;
	
	// Actions
	private OpenWithActionGroup openWithActions;
	private RefactorActionGroup refactorActions;
	private Action refreshSelectionAction;
	private ExpandAllAction expandAllAction;
	
	public SubscriberConfiguration(SubscriberParticipant participant) {
		super(participant);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeConfiguration#initialize(org.eclipse.team.ui.synchronize.StructuredViewerAdvisor)
	 */
	public void initialize(StructuredViewerAdvisor advisor) {
		initializeCollector();
		initializeActions(advisor);
		addContextMenuListener(new IActionContribution() {
			public void fillContextMenu(IMenuManager manager) {
				if (openWithActions != null) {
					openWithActions.fillContextMenu(manager, FILE_MENU);
				}
				if (refactorActions != null) {
					refactorActions.fillContextMenu(manager, EDIT_MENU);
				}
				if (refreshSelectionAction != null) {
					manager.insertAfter(EDIT_MENU, refreshSelectionAction);
				}
				if (expandAllAction != null) {
					manager.insertAfter(NAVIGATE_MENU, expandAllAction);
				}
			}
		});
		initializeListeners(advisor.getViewer());
	}
	
	private void initializeCollector() {
		collector = new WorkingSetFilteredSyncInfoCollector(((SubscriberParticipant)getParticipant()).getSubscriberSyncInfoCollector(), participant.getSubscriber().roots());
		collector.reset();
	}

	public void dispose() {
		collector.dispose();
	}
	
	private IWorkingSet getWorkingSet() {
		Object o = getProperty(P_SYNCVIEWPAGE_WORKINGSET);
		if (o instanceof IWorkingSet) {
			return (IWorkingSet)o;
		}
		return null;
	}
	
	private int getMode() {
		Object o = getProperty(P_SYNCVIEWPAGE_WORKINGSET);
		if (o instanceof Integer) {
			return ((Integer)o).intValue();
		}
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration#setProperty(java.lang.String, java.lang.Object)
	 */
	public void setProperty(String key, Object newValue) {
		if (key.equals(P_SYNCVIEWPAGE_MODE)) {
			if (setMode(((Integer)newValue).intValue())) {
				super.setProperty(key, newValue);
			} else {
				return;
			}
		}
		if (key.equals(P_SYNCVIEWPAGE_WORKINGSET)) {
			if (setWorkingSet((IWorkingSet)newValue)) {
				super.setProperty(key, newValue);
			} else {
				return;
			}
		}
		super.setProperty(key, newValue);
	}
	
	private boolean setMode(int mode) {
		int oldMode = getMode();
		if(oldMode == mode) return false;
		updateMode(mode);
		return true;
	}
	
	private boolean setWorkingSet(IWorkingSet workingSet) {
		IWorkingSet oldSet = getWorkingSet();
		if (workingSet == null || !workingSet.equals(oldSet)) {
			updateWorkingSet(workingSet);
			return true;
		}
		return false;
	}
	
	private void updateWorkingSet(IWorkingSet workingSet) {
		if(collector != null) {
			IResource[] resources = workingSet != null ? Utils.getResources(workingSet.getElements()) : new IResource[0];
			collector.setWorkingSet(resources);
		}
	}

	private int getSupportedModes() {
		Object o = getProperty(P_SUPPORTED_MODES);
		if (o instanceof Integer) {
			return ((Integer)o).intValue();
		}
		return 0;
	}
	
	private void initializeActions(StructuredViewerAdvisor advisor) {
		final StructuredViewer viewer = advisor.getViewer();
		if (viewer instanceof AbstractTreeViewer) {
			expandAllAction = new ExpandAllAction((AbstractTreeViewer) viewer);
			Utils.initAction(expandAllAction, "action.expandAll."); //$NON-NLS-1$
		}
		if (site.getType() == ISynchronizePageSite.VIEW) {
			openWithActions = new OpenWithActionGroup(site, participant.getName());
			refactorActions = new RefactorActionGroup(site);
		}
		refreshSelectionAction = new Action() {
			public void run() {
				if(viewer != null && ! viewer.getControl().isDisposed()) {
					IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
					IResource[] resources = Utils.getResources(selection.toArray());
					participant.refresh(resources, participant.getRefreshListeners().createSynchronizeViewListener(participant), Policy.bind("Participant.synchronizing"), site.getWorkbenchSite()); //$NON-NLS-1$
				}
			}
		};
		Utils.initAction(refreshSelectionAction, "action.refreshWithRemote."); //$NON-NLS-1$

	}

	private void initializeListeners(final StructuredViewer viewer) {
		if (site.getType() == ISynchronizePageSite.VIEW) {
			viewer.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					updateStatusLine((IStructuredSelection) event.getSelection());
				}
			});
			viewer.addOpenListener(new IOpenListener() {
				public void open(OpenEvent event) {
					handleOpen();
				}
			});
			viewer.addDoubleClickListener(new IDoubleClickListener() {
				public void doubleClick(DoubleClickEvent event) {
					handleDoubleClick(viewer, event);
				}
			});
		}
	}
	
	private void handleDoubleClick(StructuredViewer viewer, DoubleClickEvent event) {
		IStructuredSelection selection = (IStructuredSelection) event.getSelection();
		DiffNode node = (DiffNode) selection.getFirstElement();
		if (node != null && node instanceof SyncInfoModelElement) {
			SyncInfoModelElement syncNode = (SyncInfoModelElement) node;
			IResource resource = syncNode.getResource();
			if (syncNode != null && resource != null && resource.getType() == IResource.FILE) {
				openWithActions.openInCompareEditor();
				return;
			}
		}
	}
	
	private void handleOpen() {
		openWithActions.openInCompareEditor();
	}
	
	private void updateStatusLine(IStructuredSelection selection) {
		IWorkbenchSite ws = getSite().getWorkbenchSite();
		if (ws != null && ws instanceof IViewSite) {
			String msg = getStatusLineMessage(selection);
			((IViewSite)ws).getActionBars().getStatusLineManager().setMessage(msg);
		}
	}
	
	private String getStatusLineMessage(IStructuredSelection selection) {
		if (selection.size() == 1) {
			Object first = selection.getFirstElement();
			if (first instanceof SyncInfoModelElement) {
				SyncInfoModelElement node = (SyncInfoModelElement) first;
				IResource resource = node.getResource();
				if (resource == null) {
					return node.getName();
				} else {
					return resource.getFullPath().makeRelative().toString();
				}
			}
		}
		if (selection.size() > 1) {
			return selection.size() + Policy.bind("SynchronizeView.13"); //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
	}
	
	/**
	 * This method is invoked from <code>setMode</code> when the mode has changed.
	 * It sets the filter on the collector to show the <code>SyncInfo</code>
	 * appropriate for the mode.
	 * @param mode the new mode (one of <code>INCOMING_MODE_FILTER</code>,
	 * <code>OUTGOING_MODE_FILTER</code>, <code>CONFLICTING_MODE_FILTER</code>
	 * or <code>BOTH_MODE_FILTER</code>)
	 */
	private void updateMode(int mode) {
		if(collector != null) {	
		
			int[] modeFilter = BOTH_MODE_FILTER;
			switch(mode) {
			case INCOMING_MODE:
				modeFilter = INCOMING_MODE_FILTER; break;
			case OUTGOING_MODE:
				modeFilter = OUTGOING_MODE_FILTER; break;
			case BOTH_MODE:
				modeFilter = BOTH_MODE_FILTER; break;
			case CONFLICTING_MODE:
				modeFilter = CONFLICTING_MODE_FILTER; break;
			}

			collector.setFilter(
					new FastSyncInfoFilter.AndSyncInfoFilter(
							new FastSyncInfoFilter[] {
									new FastSyncInfoFilter.SyncInfoDirectionFilter(modeFilter)
							}));
		}
	}
}
