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
package org.eclipse.team.internal.ui.synchronize;

import java.util.*;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.*;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.ui.synchronize.IActionContribution;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.IActionBars;

/**
 * @author mvalenta
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public abstract class ModelViewController implements IActionContribution {
	
	private ISynchronizeModelProvider modelProvider;
	private List toggleModelProviderActions;
	private ISynchronizePageConfiguration configuration;
	
	/**
	 * Action that allows changing the model providers supported by this advisor.
	 */
	private class ToggleModelProviderAction extends Action implements IPropertyChangeListener {
		private ISynchronizeModelProviderDescriptor descriptor;
		protected ToggleModelProviderAction(ISynchronizeModelProviderDescriptor descriptor) {
			super(descriptor.getName(), Action.AS_RADIO_BUTTON);
			setImageDescriptor(descriptor.getImageDescriptor());
			setToolTipText(descriptor.getName());
			this.descriptor = descriptor;
			update();
			configuration.addPropertyChangeListener(this);
		}

		public void run() {
			ISynchronizeModelProvider mp = getActiveModelProvider();
			IStructuredSelection selection = null;
			if(mp != null) {
				if(mp.getDescriptor().getId().equals(descriptor.getId())) return;	
				selection = (IStructuredSelection)viewer.getSelection();	
			}
			internalPrepareInput(descriptor.getId(), null);
			setInput(getViewer());
			if(selection != null) {
				setSelection(selection.toArray(), true);
			}
			configuration.setProperty(ISynchronizePageConfiguration.P_MODEL, modelProvider.getModelRoot());
		}
		
		public void update() {
			ISynchronizeModelProvider mp = getActiveModelProvider();
			if(mp != null) {
				setChecked(mp.getDescriptor().getId().equals(descriptor.getId()));
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
		 */
		public void propertyChange(PropertyChangeEvent event) {
			if (event.getProperty().equals(ISynchronizePageConfiguration.P_MODEL)) {
				update();
			}
		}
	}
	
	/**
	 * Return the list of supported model providers for this advisor.
	 * @param viewer
	 * @return
	 */
	protected abstract ISynchronizeModelProviderDescriptor[] getSupportedModelProviders();
	
	/**
	 * Get the model provider that will be used to create the input
	 * for the adviser's viewer.
	 * @return the model provider
	 */
	protected abstract ISynchronizeModelProvider createModelProvider(String id);
	
	protected ISynchronizeModelProvider getActiveModelProvider() {
		return modelProvider;
	}
	
	protected Object internalPrepareInput(String id, IProgressMonitor monitor) {
		if(modelProvider != null) {
			modelProvider.dispose();
		}
		modelProvider = createModelProvider(id);		
		return modelProvider.prepareInput(monitor);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IActionContribution#initialize(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration)
	 */
	public void initialize(ISynchronizePageConfiguration configuration) {
		this.configuration = configuration;
		ISynchronizeModelProviderDescriptor[] providers = getSupportedModelProviders();
		// We only need switching of layouts if there is more than one model provider
		if (providers.length > 1) {
			toggleModelProviderActions = new ArrayList();
			for (int i = 0; i < providers.length; i++) {
				final ISynchronizeModelProviderDescriptor provider = providers[i];
				toggleModelProviderActions.add(new ToggleModelProviderAction(provider));
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IActionContribution#setActionBars(org.eclipse.ui.IActionBars)
	 */
	public void setActionBars(IActionBars actionBars) {
		// TODO: add to group
		IToolBarManager toolbar = actionBars.getToolBarManager();
		IMenuManager menu = actionBars.getMenuManager();
		IContributionManager contribManager = null;
		if(menu != null) {
			MenuManager layout = new MenuManager(Policy.bind("action.layout.label")); //$NON-NLS-1$
			menu.add(layout);	
			contribManager = layout;
		} else if(toolbar != null) {
			contribManager = toolbar;
		}
		
		if (toggleModelProviderActions != null && contribManager != null) {
			if (toolbar != null) {
				toolbar.add(new Separator());
				for (Iterator iter = toggleModelProviderActions.iterator(); iter.hasNext();) {
					contribManager.add((Action) iter.next());
				}
				toolbar.add(new Separator());
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IActionContribution#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	public void fillContextMenu(IMenuManager manager) {
		// No context menu entries
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.IActionContribution#dispose()
	 */
	public void dispose() {
		if(modelProvider != null) {
			modelProvider.dispose();
		}
	}
}
