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
package org.eclipse.team.ui.synchronize;

import java.util.*;
import org.eclipse.compare.structuremergeviewer.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.*;
import org.eclipse.jface.util.ListenerList;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.core.Assert;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.synchronize.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;

/**
 * A <code>StructuredViewerAdvisor</code> controls various UI
 * aspects of viewers that show {@link SyncInfoSet} like the context menu, toolbar, 
 * content provider, label provider, navigation, and model provider. The 
 * advisor allows decoupling viewer behavior from the viewers presentation. This
 * allows viewers that aren't in the same class hierarchy to re-use basic
 * behavior. 
 * <p>
 * This advisor allows viewer contributions made in a plug-in manifest to
 * be scoped to a particular unique id. As a result the context menu for the
 * viewer can be configured to show object contributions for random id schemes.
 * To enable declarative action contributions for a configuration there are two
 * steps required:
 * <ul>
 * <li>Create a viewer contribution with a <code>targetID</code> that groups
 * sets of actions that are related. A common pratice for synchronize view
 * configurations is to use the participant id as the targetID.
 * 
 * <pre>
 *  &lt;viewerContribution
 *  id=&quot;org.eclipse.team.ccvs.ui.CVSCompareSubscriberContributions&quot;
 *  targetID=&quot;org.eclipse.team.cvs.ui.compare-participant&quot;&gt;
 *  ...
 * </pre>
 * 
 * <li>Create a configuration instance with a <code>menuID</code> that
 * matches the targetID in the viewer contribution.
 * </ul>
 * </p><p>
 * Clients may subclass to add behavior for concrete structured viewers.
 * </p>
 * 
 * @see TreeViewerAdvisor
 * @since 3.0
 */
public abstract class StructuredViewerAdvisor {
	
	// The physical model shown to the user in the provided viewer. The information in 
	// this set is transformed by the model provider into the actual logical model displayed
	// in the viewer.
	private SyncInfoSet set;
	private StructuredViewer viewer;
	private ISynchronizeModelProvider modelProvider;
	private List toggleModelProviderActions;
	
	// Listeners for model changes
	private ListenerList listeners;
	private ISynchronizePageConfiguration configuration;
	
	/**
	 * Action that allows changing the model providers supported by this advisor.
	 */
	private class ToggleModelProviderAction extends Action implements ISynchronizeModelChangeListener {
		private ISynchronizeModelProviderDescriptor descriptor;
		protected ToggleModelProviderAction(ISynchronizeModelProviderDescriptor descriptor) {
			super(descriptor.getName(), Action.AS_RADIO_BUTTON);
			setImageDescriptor(descriptor.getImageDescriptor());
			setToolTipText(descriptor.getName());
			this.descriptor = descriptor;
			update();
			addInputChangedListener(this);
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
		}
		
		public void modelChanged(ISynchronizeModelElement root) {
			update();
		}
		
		public void update() {
			ISynchronizeModelProvider mp = getActiveModelProvider();
			if(mp != null) {
				setChecked(mp.getDescriptor().getId().equals(descriptor.getId()));
			}
		}
	}
	
	/**
	 * Create an advisor that will allow viewer contributions with the given <code>targetID</code>. This
	 * advisor will provide a presentation model based on the given sync info set. The model is disposed
	 * when the viewer is disposed.
	 * 
	 * @param targetID the targetID defined in the viewer contributions in a plugin.xml file.
	 * @param site the workbench site with which to register the menuId. Can be <code>null</code> in which
	 * case a site will be found using the default workbench page.
	 * @param set the set of <code>SyncInfo</code> objects that are to be shown to the user.
	 */
	public StructuredViewerAdvisor(ISynchronizePageConfiguration configuration, SyncInfoSet set) {
		this.configuration = configuration;
		this.set = set;
	}
		
	/**
	 * Install a viewer to be configured with this advisor. An advisor can only be installed with
	 * one viewer at a time. When this method completes the viewer is considered initialized and
	 * can be shown to the user. 

	 * @param viewer the viewer being installed
	 */
	public final void initializeViewer(StructuredViewer viewer) {
		Assert.isTrue(this.viewer == null, "Can only be initialized once."); //$NON-NLS-1$
		Assert.isTrue(validateViewer(viewer));
		this.viewer = viewer;
	
		initializeListeners(viewer);
		initializeActions(viewer);
		viewer.setLabelProvider(getLabelProvider());
		viewer.setContentProvider(getContentProvider());
		
		// The input may of been set already. In that case, don't change it and
		// simply assign it to the view.
		if(modelProvider == null) {
			prepareInput(null);
		}
		setInput(viewer);
	}
	
	/**
	 * This is called to add a listener to the model shown in the viewer. The listener is
	 * called when the model is changed or updated.
	 * 
	 * @param listener the listener to add
	 */
	public void addInputChangedListener(ISynchronizeModelChangeListener listener) {
		if (listeners == null)
			listeners= new ListenerList();
		listeners.add(listener);
	}

	/**
	 * Remove a model listener.
	 * 
	 * @param listener the listener to remove.
	 */
	public void removeInputChangedListener(ISynchronizeModelChangeListener listener) {
		if (listeners != null) {
			listeners.remove(listener);
			if (listeners.isEmpty())
				listeners= null;
		}
	}
	
	/**
	 * Must be called when an advisor is no longer needed.
	 */
	public void dispose() {
		if(modelProvider != null) {
			modelProvider.dispose();
		}
	}

	/**
	 * Return the <code>SyncInfoSet</code> used to create the model shown by this advisor.
	 * 
	 * @return the <code>SyncInfoSet</code> used to create the model shown by this advisor.
	 */
	public SyncInfoSet getSyncInfoSet() {
		return set;
	}
	
	/**
	 * Subclasses must implement to allow navigation of their viewers.
	 * 
	 * @param next if <code>true</code> then navigate forwards, otherwise navigate
	 * backwards.
	 * @return <code>true</code> if the end is reached, and <code>false</code> otherwise.
	 */
	public abstract boolean navigate(boolean next);

	/**
	 * Sets a new selection for this viewer and optionally makes it visible. The advisor will try and
	 * convert the objects into the appropriate viewer objects. This is required because the model
	 * provider controls the actual model elements in the viewer and must be consulted in order to
	 * understand what objects can be selected in the viewer.
	 * 
	 * @param object the objects to select
	 * @param reveal <code>true</code> if the selection is to be made visible, and
	 *                  <code>false</code> otherwise
	 */
	public void setSelection(Object[] objects, boolean reveal) {
		ISelection selection = getSelection(objects);
		if (!selection.isEmpty()) {
			viewer.setSelection(selection, reveal);
		}
	}
	
	/**
	 * Gets a new selection that contains the view model objects that
	 * correspond to the given objects. The advisor will try and
	 * convert the objects into the appropriate viewer objects. 
	 * This is required because the model provider controls the actual 
	 * model elements in the viewer and must be consulted in order to
	 * understand what objects can be selected in the viewer.
	 * <p>
	 * This method does not affect the selection of the viewer itself.
	 * It's main purpose is for testing and should not be used by other
	 * clients.
	 * </p>
	 * @param object the objects to select
	 * @return a selection corresponding to the given objects
	 */
	public ISelection getSelection(Object[] objects) {
		if (modelProvider != null) {
	 		Object[] viewerObjects = new Object[objects.length];
			for (int i = 0; i < objects.length; i++) {
				viewerObjects[i] = modelProvider.getMapping(objects[i]);
			}
			return new StructuredSelection(viewerObjects);
		} else {
			return StructuredSelection.EMPTY;
		}
	}
	 
	/**
	 * Creates the model that will be shown in the viewers. This can be called before the
	 * viewer has been created.
	 * <p>
	 * The result of this method can be shown used as the input to a viewer. However, the
	 * prefered method of initializing a viewer is to call {@link #initializeViewer(StructuredViewer)}
	 * directly. This method only exists when the model must be created before the
	 * viewer.
	 * </p>
	 * @param monitor shows progress while preparing the model
	 * @return the model that can be shown in a viewer
	 */
	public Object prepareInput(IProgressMonitor monitor) {
		return internalPrepareInput(null, monitor);
	}
	
	protected Object internalPrepareInput(String id, IProgressMonitor monitor) {
		if(modelProvider != null) {
			modelProvider.dispose();
		}
		modelProvider = createModelProvider(id);		
		return modelProvider.prepareInput(monitor);
	}
	
	/**
	 * Allows the advisor to make contributions to the given action bars. Note that some of the 
	 * items in the action bar may not be accessible.
	 * 
	 * @param actionBars the toolbar manager to which to add actions.
	 */
	public void setActionBars(IActionBars actionBars) {
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
	
	/**
	 * Method invoked from <code>initializeViewer(Composite, StructuredViewer)</code>
	 * in order to initialize any actions for the viewer. It is invoked before
	 * the input is set on the viewer in order to allow actions to be
	 * initialized before there is any reaction to the input being set (e.g.
	 * selecting and opening the first element).
	 * <p>
	 * The default behavior is to add the up and down navigation nuttons to the
	 * toolbar. Subclasses can override.
	 * </p>
	 * @param viewer the viewer being initialize
	 */
	protected void initializeActions(StructuredViewer viewer) {
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
	
	/**
	 * Method invoked from <code>initializeViewer(Composite, StructuredViewer)</code>
	 * in order to initialize any listeners for the viewer.
	 *
	 * @param viewer the viewer being initialize
	 */
	protected void initializeListeners(final StructuredViewer viewer) {
		viewer.getControl().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				StructuredViewerAdvisor.this.dispose();
			}
		});
	}
	
	/**
	 * Get the model provider that will be used to create the input
	 * for the adviser's viewer.
	 * @return the model provider
	 */
	protected abstract ISynchronizeModelProvider createModelProvider(String id);
	
	protected ISynchronizeModelProvider getActiveModelProvider() {
		return modelProvider;
	}
	
	/**
	 * Return the list of supported model providers for this advisor.
	 * @param viewer
	 * @return
	 */
	protected abstract ISynchronizeModelProviderDescriptor[] getSupportedModelProviders();
	
	/**
	 * Subclasses can validate that the viewer being initialized with this advisor
	 * is of the correct type.
	 * 
	 * @param viewer the viewer to validate
	 * @return <code>true</code> if the viewer is valid, <code>false</code> otherwise.
	 */
	protected abstract boolean validateViewer(StructuredViewer viewer);

	/**
	 * Run the runnable in the UI thread.
	 * @param r the runnable to run in the UI thread.
	 */
	protected void aSyncExec(Runnable r) {
		final Control ctrl = viewer.getControl();
		if (ctrl != null && !ctrl.isDisposed()) {
			ctrl.getDisplay().asyncExec(r);
		}
	}

	private void fireChanges() {
		if (listeners != null) {
			Object[] l= listeners.getListeners();
			for (int i= 0; i < l.length; i++)
				((ISynchronizeModelChangeListener) l[i]).modelChanged(modelProvider.getModelRoot());
		}
	}

	/**
	 * Returns the content provider for the viewer.
	 * 
	 * @return the content provider for the viewer.
	 */
	protected IStructuredContentProvider getContentProvider() {
		return new BaseWorkbenchContentProvider();
	}


	/**
	 * Get the label provider that will be assigned to the viewer initialized
	 * by this configuration. Subclass may override but should either wrap the
	 * default one provided by this method or subclass <code>TeamSubscriberParticipantLabelProvider</code>.
	 * In the later case, the logical label provider should still be assigned
	 * to the subclass of <code>TeamSubscriberParticipantLabelProvider</code>.
	 * @param logicalProvider
	 *            the label provider for the selected logical view
	 * @return a label provider
	 * @see SynchronizeModelElementLabelProvider
	 */
	protected ILabelProvider getLabelProvider() {
		return new SynchronizeModelElementLabelProvider();
	}

	/**
	 * Returns the viewer configured by this advisor.
	 * 
	 * @return the viewer configured by this advisor.
	 */
	public final StructuredViewer getViewer() {
		return viewer;
	}

	/**
	 * Called to set the input to a viewer. The input to a viewer is always the model created
	 * by the model provider.
	 * 
	 * @param viewer the viewer to set the input.
	 */
	protected final void setInput(StructuredViewer viewer) {
		modelProvider.setViewer(viewer);
		viewer.setSorter(modelProvider.getViewerSorter());
		ISynchronizeModelElement input = modelProvider.getModelRoot();
		if (input instanceof DiffNode) {
			((DiffNode) input).addCompareInputChangeListener(new ICompareInputChangeListener() {
				public void compareInputChanged(ICompareInput source) {
					fireChanges();
				}
			});
		}
		viewer.setInput(modelProvider.getModelRoot());
	}
}