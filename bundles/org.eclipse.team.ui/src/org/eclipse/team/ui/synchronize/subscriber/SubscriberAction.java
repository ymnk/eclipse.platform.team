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
package org.eclipse.team.ui.synchronize.subscriber;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.*;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.actions.TeamAction;
import org.eclipse.team.ui.synchronize.viewers.IBusyWorkbenchAdapter;
import org.eclipse.team.ui.synchronize.viewers.SyncInfoDiffNode;
import org.eclipse.ui.*;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

/**
 * This action provides utilities for performing operations on diff elements
 * contained in a selection.
 * 1. provides scheduling action via workbench part (provide feedback via view)
 * 2. provides selection filtering
 * 3. provides support for running action in background or foreground
 * 4. provides support for locking workspace resources
 * 5. 
 * <p>
 * It is optional for SubscriberParticipant actions to subclass.
 * </p>
 * @since 3.0
 */
public abstract class SubscriberAction extends TeamAction implements IViewActionDelegate, IEditorActionDelegate {
		
	private IDiffElement[] filteredDiffElements = null;
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public final void run(IAction action) {
		//		TODO: Saving can change the sync state! How should this be handled?
		//		 boolean result = saveIfNecessary();
		//		 if (!result) return null;
		SyncInfoSet syncSet = new SyncInfoSet(getFilteredSyncInfos());
		if (syncSet == null || syncSet.isEmpty()) return;
		try {
			getRunnableContext().run(getJobName(syncSet), getSchedulingRule(syncSet), true, getRunnable(syncSet));
		} catch (InvocationTargetException e) {
			handle(e);
		} catch (InterruptedException e) {
			// nothing to do;
		}
	}
	
	/**
	 * Return the job name to be used if the action can run as a job.
	 * 
	 * @param syncSet
	 * @return
	 */
	protected abstract String getJobName(SyncInfoSet syncSet);
	
	/**
	 * Subsclasses must override to provide behavior for the action.
	 * @param syncSet the set of filtered sync info objects on which to perform the action.
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 * 		  reporting and cancellation are not desired 
	 * @throws TeamException if something went wrong running the action
	 */
	protected abstract void run(SyncInfoSet syncSet, IProgressMonitor monitor) throws TeamException;
	
	protected void handle(Exception e) {
		Utils.handle(e);
	}
	
	/**
	 * This method returns all instances of SyncInfo that are in the current
	 * selection. For a tree view, this is any descendants of the selected resource that are
	 * contained in the view.
	 * 
	 * @return the selected resources
	 */
	protected IDiffElement[] getDiffElements() {
		return Utils.getDiffNodes(((IStructuredSelection)selection).toArray());
	}

	/**
	 * The default enablement behavior for subscriber actions is to enable
	 * the action if there is at least one SyncInfo in the selection
	 * for which the action is enabled (determined by invoking 
	 * <code>isEnabled(SyncInfo)</code>).
	 * @see org.eclipse.team.internal.ui.actions.TeamAction#isEnabled()
	 */
	protected boolean isEnabled() throws TeamException {
		return (getFilteredDiffElements().length > 0);
	}

	/**
	 * Default filter includes all out-of-sync elements in the current
	 * selection.
	 * @return a sync info filter which selects all out-of-sync resources.
	 */
	protected FastSyncInfoFilter getSyncInfoFilter() {
		return new FastSyncInfoFilter();
	}

	/**
	 * Return the selected diff element for which this action is enabled.
	 * @return the list of selected diff elements for which this action is enabled.
	 */
	protected IDiffElement[] getFilteredDiffElements() {
		if (filteredDiffElements == null) {
			IDiffElement[] elements = getDiffElements();
			List filtered = new ArrayList();
			for (int i = 0; i < elements.length; i++) {
				IDiffElement e = elements[i];
				if (e instanceof SyncInfoDiffNode) {
					SyncInfo info = ((SyncInfoDiffNode) e).getSyncInfo();
					if (info != null && getSyncInfoFilter().select(info)) {
						filtered.add(e);
					}
				}
			}
			filteredDiffElements = (IDiffElement[]) filtered.toArray(new IDiffElement[filtered.size()]);
		}
		return filteredDiffElements;
	}
	
	/**
	 * Return the selected SyncInfo for which this action is enabled.
	 * @return the selected SyncInfo for which this action is enabled.
	 */
	protected SyncInfo[] getFilteredSyncInfos() {
		IDiffElement[] elements = getFilteredDiffElements();
		List filtered = new ArrayList();
		for (int i = 0; i < elements.length; i++) {
			IDiffElement e = elements[i];
			if (e instanceof SyncInfoDiffNode) {
				filtered.add(((SyncInfoDiffNode)e).getSyncInfo());
			}
		}
		return (SyncInfo[]) filtered.toArray(new SyncInfo[filtered.size()]);
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorActionDelegate#setActiveEditor(org.eclipse.jface.action.IAction, org.eclipse.ui.IEditorPart)
	 */
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		setActivePart(action, targetEditor);
	}
	
	public void markBusy(IDiffElement[] elements, boolean isBusy) {
		for (int i = 0; i < elements.length; i++) {
			IDiffElement e = elements[i];
			if(e instanceof IAdaptable) {
				IBusyWorkbenchAdapter busyAdapter = (IBusyWorkbenchAdapter) ((IAdaptable)e).getAdapter(IBusyWorkbenchAdapter.class);
				if(busyAdapter != null) {
					busyAdapter.setBusy(e, isBusy);		
				}
			}
		}
	}
	
	public static void schedule(Job job, IWorkbenchSite site) {
		if (site != null) {
			IWorkbenchSiteProgressService siteProgress = (IWorkbenchSiteProgressService) site.getAdapter(IWorkbenchSiteProgressService.class);
			if (siteProgress != null) {
				siteProgress.schedule(job);
				return;
			}
		}
		job.schedule();
	}

	/*
	 * Return the ITeamRunnableContext which will be used to run the operation.
	 */
	private ITeamRunnableContext getRunnableContext() {
		if (canRunAsJob()) {
			// mark resources that will be affected by job
			markBusy(getFilteredDiffElements(), true);
			// register to unmark when job is finished
			IJobChangeListener listener = new JobChangeAdapter() {
				public void done(IJobChangeEvent event) {
					markBusy(getFilteredDiffElements(), false);
				}
			};					
			return new JobRunnableContext(listener) {
				// schedule via view
				protected void schedule(Job job) {
					IWorkbenchSite site = null;
					IWorkbenchPart part = getTargetPart();
					if(part != null) {
						site = part.getSite();
					}
					SubscriberAction.schedule(job, site);
				}
			};
		} else {
			return new ProgressDialogRunnableContext(shell);
		}
	}

	protected boolean canRunAsJob() {
		return false;
	}

	/**
	 * Return a scheduling rule that includes all resources that will be operated 
	 * on by the subscriber action. The default behavior is to include all projects
	 * effected by the operation. Subclasses may override.
	 * 
	 * @param syncSet
	 * @return
	 */
	protected ISchedulingRule getSchedulingRule(SyncInfoSet syncSet) {
		IResource[] resources = syncSet.getResources();
		Set set = new HashSet();
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			set.add(resource.getProject());
		}
		IProject[] projects = (IProject[]) set.toArray(new IProject[set.size()]);
		if (projects.length == 1) {
			return projects[0];
		} else {
			return new MultiRule(projects);
		}
	}

	public IRunnableWithProgress getRunnable(final SyncInfoSet syncSet) {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					SubscriberAction.this.run(syncSet, monitor);
				} catch (TeamException e) {
					throw new InvocationTargetException(e);
				}
			}
		};
	}
}
