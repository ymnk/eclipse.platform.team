/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.subscriber;

import org.eclipse.compare.structuremergeviewer.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.ActionDelegateWrapper;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.IActionBars;

/**
 * Actions for the CVS workspace participant toolbar menu
 */
public class WorkspaceParticipantActionContributions implements IActionContribution {

	private ActionDelegateWrapper commitToolbar;
	private ActionDelegateWrapper updateToolbar;
	private IPropertyChangeListener listener = new IPropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent event) {
			if (event.getProperty().equals(ISynchronizePageConfiguration.P_MODEL)) {
				Object o = event.getNewValue();
				inputChanged(o);
			}
		}
	};
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IContextMenuContribution#initialize(org.eclipse.jface.viewers.StructuredViewer)
	 */
	public void initialize(ISynchronizePageConfiguration configuration) {
		commitToolbar = new ActionDelegateWrapper(new SubscriberCommitAction(), configuration.getSite().getPart());
		WorkspaceUpdateAction action = new WorkspaceUpdateAction();
		action.setPromptBeforeUpdate(true);
		updateToolbar = new ActionDelegateWrapper(action, configuration.getSite().getPart());

		Utils.initAction(commitToolbar, "action.SynchronizeViewCommit.", Policy.getBundle()); //$NON-NLS-1$
		Utils.initAction(updateToolbar, "action.SynchronizeViewUpdate.", Policy.getBundle()); //$NON-NLS-1$

		configuration.addPropertyChangeListener(listener);		
		Object o = configuration.getProperty(ISynchronizePageConfiguration.P_MODEL);
		if (o != null) {
			inputChanged(o);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IContextMenuContribution#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	public void fillContextMenu(IMenuManager manager) {
		// Nothing to add to context menu
	}
	
	public void setActionBars(IActionBars actionBars) {
		IToolBarManager toolbar = actionBars.getToolBarManager();
		if (toolbar != null) {
			toolbar.add(new Separator());
			toolbar.add(updateToolbar);
			toolbar.add(commitToolbar);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IContextMenuContribution#dispose()
	 */
	public void dispose() {
		// Nothing needs to be disposed
	}

	/**
	 * Invoked when the model shown by the page's view changes
	 */
	protected void inputChanged(Object o) {
		if (o instanceof ISynchronizeModelElement) {
			final ISynchronizeModelElement input = (ISynchronizeModelElement)o;
			input.addCompareInputChangeListener(new ICompareInputChangeListener() {
				public void compareInputChanged(ICompareInput source) {
					modelChanged(input);
				}
			});
			modelChanged(input);
		}
	}
	
	/**
	 * @param element
	 */
	public void modelChanged(ISynchronizeModelElement element) {
		commitToolbar.setSelection(element);
		updateToolbar.setSelection(element);
	}
}
