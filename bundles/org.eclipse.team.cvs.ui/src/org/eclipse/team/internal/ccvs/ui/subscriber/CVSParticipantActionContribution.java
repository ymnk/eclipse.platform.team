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

import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.internal.ccvs.ui.CVSLightweightDecorator;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ui.synchronize.actions.RemoveSynchronizeParticipantAction;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.team.ui.synchronize.subscribers.SubscriberParticipant;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;

/**
 * Actions common to all CVS particpants
 */
public abstract class CVSParticipantActionContribution implements IActionContribution, IPropertyChangeListener {

	private ISynchronizePageConfiguration configuration;
	private RemoveSynchronizeParticipantAction removeAction;
	
	private static class CVSLabelDecorator extends LabelProvider implements ILabelDecorator  {
		public String decorateText(String input, Object element) {
			String text = input;
			if (element instanceof ISynchronizeModelElement) {
				IResource resource =  ((ISynchronizeModelElement)element).getResource();
				if(resource != null && resource.getType() != IResource.ROOT) {
					CVSLightweightDecorator.Decoration decoration = new CVSLightweightDecorator.Decoration();
					CVSLightweightDecorator.decorateTextLabel(resource, decoration, false, true);
					StringBuffer output = new StringBuffer(25);
					if(decoration.prefix != null) {
						output.append(decoration.prefix);
					}
					output.append(text);
					if(decoration.suffix != null) {
						output.append(decoration.suffix);
					}
					return output.toString();
				}
			}
			return text;
		}
		public Image decorateImage(Image base, Object element) {
			return base;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IActionContribution#initialize(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration)
	 */
	public void initialize(ISynchronizePageConfiguration configuration) {
		this.configuration = configuration;
		// Listen for model changes
		configuration.addPropertyChangeListener(this);
		// Listen for decorator changed to refresh the viewer's labels.
		CVSUIPlugin.addPropertyChangeListener(this);
		Object o = configuration.getProperty(ISynchronizePageConfiguration.P_MODEL);
		if (o != null) {
			inputChanged(o);
		}
		ILabelProvider provider = (ILabelProvider)configuration.getProperty(ISynchronizePageConfiguration.P_LABEL_PROVIDER);
		configuration.setProperty(
				ISynchronizePageConfiguration.P_LABEL_PROVIDER,
				new DecoratingColorLabelProvider(provider, new CVSLabelDecorator()));
	}
	
	protected void createRemoveAction(ISynchronizePageConfiguration configuration) {
		if (configuration.getSite().getPart() instanceof IViewPart) {
			SubscriberParticipant p = (SubscriberParticipant)configuration.getParticipant();
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
	public void setActionBars(IActionBars actionBars) {
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
		//TODO:
//		if(property.equals(CVSUIPlugin.P_DECORATORS_CHANGED) && getViewer() != null && getSyncInfoSet() != null) {
//			getViewer().refresh(true /* update labels */);
//		}
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
	 * @param input
	 */
	protected abstract void modelChanged(ISynchronizeModelElement input);
}
