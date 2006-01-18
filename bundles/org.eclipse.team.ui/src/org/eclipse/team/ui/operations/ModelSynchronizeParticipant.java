/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.operations;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.mapping.IMergeContext;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.mapping.*;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.mapping.ICompareAdapter;
import org.eclipse.team.ui.mapping.SynchronizationActionProvider;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * Synchronize participant that obtains it's synchronization state from
 * a {@link ISynchronizationContext}.
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is a guarantee neither that this API will
 * work nor that it will remain the same. Please do not use this API without
 * consulting with the Platform/Team team.
 * </p>
 * 
 * @since 3.2
 **/
public class ModelSynchronizeParticipant extends
		AbstractSynchronizeParticipant {

	/**
	 * The id of the merge action group that determines where the merge
	 * actions (e.g. merge and overwrite) appear in the context menu or toolbar.
	 */
	public static final String MERGE_ACTION_GROUP = "group.merge"; //$NON-NLS-1$

	/**
	 * The id of the action group that determines where the other
	 * actions (e.g. mark-as-mered) appear in the context menu.
	 */
	public static final String OTHER_ACTION_GROUP = "group.other"; //$NON-NLS-1$
	
	private ISynchronizationContext context;
	
	private boolean mergingEnabled = true;

	/**
	 * Actions for a model participant
	 */
	private class ModelActionContribution extends SynchronizePageActionGroup {
		private MergeIncomingChangesAction updateToolbarAction;
		
		public void initialize(ISynchronizePageConfiguration configuration) {
			super.initialize(configuration);
			
			ISynchronizationContext context = ((ModelSynchronizeParticipant)configuration.getParticipant()).getContext();
			if (context instanceof IMergeContext) {
				updateToolbarAction = new MergeIncomingChangesAction(configuration);
				appendToGroup(
						ISynchronizePageConfiguration.P_TOOLBAR_MENU,
						MERGE_ACTION_GROUP,
						updateToolbarAction);
				// TODO: Should add a merge all to the context menu as well?
			}
		}
	}
	
	/**
	 * Create a participant for the given context
	 * @param context the synchronization context
	 */
	public ModelSynchronizeParticipant(ISynchronizationContext context, String name) {
		initializeContext(context);
		try {
			setInitializationData(TeamUI.getSynchronizeManager().getParticipantDescriptor("org.eclipse.team.ui.synchronization_context_synchronize_participant")); //$NON-NLS-1$
		} catch (CoreException e) {
			TeamUIPlugin.log(e);
		}
		setSecondaryId(Long.toString(System.currentTimeMillis()));
		setName(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.AbstractSynchronizeParticipant#initializeConfiguration(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration)
	 */
	protected void initializeConfiguration(
			ISynchronizePageConfiguration configuration) {
		if (isMergingEnabled()) {
			configuration.addMenuGroup(ISynchronizePageConfiguration.P_TOOLBAR_MENU, MERGE_ACTION_GROUP);
			configuration.addMenuGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU, MERGE_ACTION_GROUP);
			configuration.addMenuGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU, OTHER_ACTION_GROUP);
			configuration.addActionContribution(new ModelActionContribution());
		}
		configuration.setSupportedModes(ISynchronizePageConfiguration.ALL_MODES);
		configuration.setMode(ISynchronizePageConfiguration.BOTH_MODE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#createPage(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration)
	 */
	public IPageBookViewPage createPage(
			ISynchronizePageConfiguration configuration) {
		return new ModelSynchronizePage(configuration);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#run(org.eclipse.ui.IWorkbenchPart)
	 */
	public void run(IWorkbenchPart part) {
		// TODO Auto-generated method stub
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#dispose()
	 */
	public void dispose() {
		context.dispose();
	}
	
	/**
	 * Set the context of this participant. This method must be invoked
	 * before a page is obtained from the participant.
	 * @param context the context for this participant
	 */
	protected void initializeContext(ISynchronizationContext context) {
		this.context = context;
	}

	/**
	 * Return the synchronization context for this participant.
	 * @return the synchronization context for this participant
	 */
	public ISynchronizationContext getContext() {
		return context;
	}
	
	/**
	 * Return a compare input for the given model object or <code>null</code>
	 * if the object is not eligible for comparison.
	 * @param o the model object
	 * @return a compare input for the model object or <code>null</code>
	 */
	public ICompareInput asCompareInput(Object o) {
		if (o instanceof ICompareInput) {
			return (ICompareInput) o;
		}
		// Get a compare input from the model provider's compare adapter
		ICompareAdapter adapter = Utils.getCompareAdapter(o);
		if (adapter != null)
			return adapter.asCompareInput(getContext(), o);
		return null;
	}
	
	/**
	 * Return a structure viewer for viewing the structure of the given compare input
	 * @param parent the parent composite of the viewer
	 * @param oldViewer the current viewer which can be returned if it is appropriate for use with the given input
	 * @param input the compare input to be viewed
	 * @param configuration the compare configuration information
	 * @return a viewer for viewing the structure of the given compare input
	 */ 
	public Viewer findStructureViewer(Composite parent, Viewer oldViewer, ICompareInput input, CompareConfiguration configuration) {
		// Get a structure viewer from the model provider's compare adapter
		ICompareAdapter adapter = Utils.getCompareAdapter(input);
		if (adapter != null)
			return adapter.findStructureViewer(parent, oldViewer, input, configuration);
		return null;
	}

	/**
	 * Return a viewer for comparing the content of the given compare input.
	 * @param parent the parent composite of the viewer
	 * @param oldViewer the current viewer which can be returned if it is appropriate for use with the given input
	 * @param input the compare input to be viewed
	 * @param configuration the compare configuration information
	 * @return a viewer for comparing the content of the given compare input
	 */ 
	public Viewer findContentViewer(Composite parent, Viewer oldViewer, ICompareInput input, CompareConfiguration configuration) {
		// Get a content viewer from the model provider's compare adapter
		ICompareAdapter adapter = Utils.getCompareAdapter(input);
		if (adapter != null)
			return adapter.findContentViewer(parent, oldViewer, input, configuration);
		return null;
	}

	/**
	 * Prepare the compare inout for display using the compare configuration. 
	 * @param input the compare input to be displayed
	 * @param configuration the compare configuration for the editor that will display the input
	 * @param monitor a progress monitor
	 */
	public void prepareInput(ICompareInput input, CompareConfiguration configuration, IProgressMonitor monitor) throws CoreException {
		// Get a content viewer from the model provider's compare adapter
		ICompareAdapter adapter = Utils.getCompareAdapter(input);
		if (adapter != null)
			adapter.prepareInput(input, configuration, monitor);
	}

	/**
	 * Return whether their is a compare input associated with the given object.
	 * In otherwords, return <code>true</code> if {@link #asCompareInput(Object) }
	 * would return a value and <code>false</code> if it would return <code>null</code>.
	 * @param object the object.
	 * @return whether their is a compare input associated with the given object
	 */
	public boolean hasCompareInputFor(Object object) {
		// Get a content viewer from the model provider's compare adapter
		ICompareAdapter adapter = Utils.getCompareAdapter(object);
		if (adapter != null)
			return adapter.hasCompareInput(getContext(), object);
		return false;
	}

	/**
	 * Return whether merge capabilities are enabled for this participant.
	 * If merging is enabled, merge actions can be shown. If merging is disabled, no 
	 * merge actions should be surfaced.
	 * @return whether merge capabilities should be enabled for this participant
	 */
	public boolean isMergingEnabled() {
		return mergingEnabled;
	}

	/**
	 * Set whether merge capabilities should be enabled for this participant.
	 * @param mergingEnabled whether merge capabilities should be enabled for this participant
	 */
	public void setMergingEnabled(boolean mergingEnabled) {
		this.mergingEnabled = mergingEnabled;
	}
	
	/**
	 * Method to add the merge actions to the contetx menu. This method
	 * is called by the internal synchronization framework and should not
	 * to be invoked by other clients. Subsclasses can configure the
	 * merge actions by overriding {@link #configureMergeAction(String, Action)}
	 * and can control where in the context menu the action appears by 
	 * overriding {@link #addToContextMenu(String, Action, IMenuManager)}.
	 * @param cmm the menu manager
	 */
	public final void addMergeActions(CommonMenuManager cmm) {
		if (isMergingEnabled()) {
			SynchronizationAction merge = new SynchronizationAction(SynchronizationActionProvider.MERGE_ACTION_ID, cmm);
			configureMergeAction(SynchronizationActionProvider.MERGE_ACTION_ID, merge);
			addToContextMenu(SynchronizationActionProvider.MERGE_ACTION_ID, merge, cmm);
			SynchronizationAction overwrite = new SynchronizationAction(SynchronizationActionProvider.OVERWRITE_ACTION_ID, cmm);
			configureMergeAction(SynchronizationActionProvider.OVERWRITE_ACTION_ID, overwrite);
			addToContextMenu(SynchronizationActionProvider.OVERWRITE_ACTION_ID, overwrite, cmm);
			SynchronizationAction markAsMerged = new SynchronizationAction(SynchronizationActionProvider.MARK_AS_MERGE_ACTION_ID, cmm);
			configureMergeAction(SynchronizationActionProvider.MARK_AS_MERGE_ACTION_ID, markAsMerged);
			addToContextMenu(SynchronizationActionProvider.MARK_AS_MERGE_ACTION_ID, markAsMerged, cmm);
		}
	}
	
	/**
	 * Configure the merge action to have appropriate label, image, etc.
	 * Subclasses may override but should invoke the overridden
	 * method for unrecognized ids in order to support future additions.
	 * @param mergeActionId the id of the merge action (one of 
	 * {@link SynchronizationActionProvider#MERGE_ACTION_ID},
	 * {@link SynchronizationActionProvider#OVERWRITE_ACTION_ID} or
	 * {@link SynchronizationActionProvider#MARK_AS_MERGE_ACTION_ID})
	 * @param action the action for the given id
	 */
	protected void configureMergeAction(String mergeActionId, Action action) {
		if (mergeActionId == SynchronizationActionProvider.MERGE_ACTION_ID) {
			Utils.initAction(action, "action.merge."); //$NON-NLS-1$
		} else if (mergeActionId == SynchronizationActionProvider.OVERWRITE_ACTION_ID) {
			Utils.initAction(action, "action.overwrite."); //$NON-NLS-1$
		} else if (mergeActionId == SynchronizationActionProvider.MARK_AS_MERGE_ACTION_ID) {
			Utils.initAction(action, "action.markAsMerged."); //$NON-NLS-1$
		}
	}
	
	/**
	 * Add the merge action to the context menu manager. 
	 * Subclasses may override but should invoke the overridden
	 * method for unrecognized ids in order to support future additions.
	 * @param id the id of the merge action (one of 
	 * {@link SynchronizationActionProvider#MERGE_ACTION_ID},
	 * {@link SynchronizationActionProvider#OVERWRITE_ACTION_ID} or
	 * {@link SynchronizationActionProvider#MARK_AS_MERGE_ACTION_ID})
	 * @param action the action for the given id
	 * @param manager the context menu manager
	 */
	protected void addToContextMenu(String mergeActionId, Action action, IMenuManager manager) {
		IContributionItem group = null;;
		if (mergeActionId == SynchronizationActionProvider.MERGE_ACTION_ID) {
			group = manager.find(MERGE_ACTION_GROUP);
		} else if (mergeActionId == SynchronizationActionProvider.OVERWRITE_ACTION_ID) {
			group = manager.find(MERGE_ACTION_GROUP);
		} else if (mergeActionId == SynchronizationActionProvider.MARK_AS_MERGE_ACTION_ID) {
			group = manager.find(OTHER_ACTION_GROUP);
		}
		if (group != null) {
			manager.appendToGroup(group.getId(), action);
		} else {
			manager.add(action);
		}
	}
}
