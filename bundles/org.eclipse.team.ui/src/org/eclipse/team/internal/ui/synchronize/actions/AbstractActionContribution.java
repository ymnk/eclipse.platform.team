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
package org.eclipse.team.internal.ui.synchronize.actions;

import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;

/**
 * Actions common to CVS, FTP and WebDAV particpants
 */
public abstract class AbstractActionContribution implements IActionContribution, IPropertyChangeListener {

	private ISynchronizePageConfiguration configuration;
	private RemoveSynchronizeParticipantAction removeAction;
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IActionContribution#initialize(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration)
	 */
	public void initialize(ISynchronizePageConfiguration configuration) {
		this.configuration = configuration;
		// Listen for model changes
		configuration.addPropertyChangeListener(this);
		Object o = configuration.getProperty(ISynchronizePageConfiguration.P_MODEL);
		if (o != null) {
			inputChanged(o);
		}
	}
	
	protected void createRemoveAction(ISynchronizePageConfiguration configuration) {
		if (configuration.getSite().getPart() instanceof IViewPart) {
			ISynchronizeParticipant p = configuration.getParticipant();
			removeAction = new RemoveSynchronizeParticipantAction(p);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IActionContribution#dispose()
	 */
	public void dispose() {
		configuration.removePropertyChangeListener(this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IActionContribution#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	public void fillContextMenu(IMenuManager manager) {
		// Do nothing
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IActionContribution#setActionBars(org.eclipse.ui.IActionBars)
	 */
	public void fillActionBars(IActionBars actionBars) {
		if(actionBars != null) {
			IToolBarManager toolbar = actionBars.getToolBarManager();
			if(toolbar != null) {
				if (removeAction != null) {
					// Add the remove to the end of the toolbar
					toolbar.add(removeAction);
				}
			}
		}	
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		String property = event.getProperty();
		if (property.equals(ISynchronizePageConfiguration.P_MODEL)) {
			Object o = event.getNewValue();
			inputChanged(o);
		}
	}
	
	private void inputChanged(Object o) {
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
	 * Subclasses can implement this method to recieve notification
	 * whenever the model changes (i.e. if the view input changes or the
	 * nodes being displayed change).
	 * @param input the view input
	 */
	protected abstract void modelChanged(ISynchronizeModelElement input);
}
