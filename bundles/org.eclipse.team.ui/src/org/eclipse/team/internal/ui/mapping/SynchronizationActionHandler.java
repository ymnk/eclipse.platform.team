/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.mapping.ModelProviderOperation;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.navigator.IExtensionStateModel;

/**
 * An abstract superclass that enables models to create handlers
 * for the basic merge operations (merge, overwrite and mark-as-merged).
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is a guarantee neither that this API will
 * work nor that it will remain the same. Please do not use this API without
 * consulting with the Platform/Team team.
 * </p>
 * 
 * @since 3.2
 */
public abstract class SynchronizationActionHandler extends AbstractHandler {

	private final IExtensionStateModel model;
	private boolean enabled = false;
	
	private ISelectionChangedListener listener = new ISelectionChangedListener() {
		public void selectionChanged(SelectionChangedEvent event) {
			updatedEnablement(event);
		}
	};

	/**
	 * Create the handler.
	 * @param model the extension state model that contains the state
	 * provided by the synchronize page display the model.
	 */
	public SynchronizationActionHandler(IExtensionStateModel model) {
		this.model = model;
		getSelectionProvider(null).addSelectionChangedListener(listener);
		updateEnablement(getSelectionProvider(null).getSelection(), getConfiguration(null));
	}

	/* private */ void updatedEnablement(SelectionChangedEvent event) {
		updateEnablement(event.getSelection(), getConfiguration(null));
	}

	private void updateEnablement(ISelection selection, ISynchronizePageConfiguration configuration) {
		boolean isEnabled = isEnabled((IStructuredSelection)selection, configuration);
		setEnabled(isEnabled);
	}

	private boolean isEnabled(IStructuredSelection selection, ISynchronizePageConfiguration configuration) {
		ModelProviderOperation op = createOperation(configuration, selection);
		return op.shouldRun();
	}

	private final ISynchronizePageConfiguration getConfiguration(ExecutionEvent event) {
		return (ISynchronizePageConfiguration)model.getProperty(TeamUI.SYNCHRONIZATION_PAGE_CONFIGURATION);
	}

	private final IStructuredSelection getStructuredSelection(ExecutionEvent event) {
		return (IStructuredSelection)getSelectionProvider(event).getSelection();
	}

	private ISelectionProvider getSelectionProvider(ExecutionEvent event) {
		return getConfiguration(event).getSite().getSelectionProvider();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.AbstractHandler#isEnabled()
	 */
	public boolean isEnabled() {
		return enabled;
	}
	
	/**
	 * Set the enablement of this handler.
	 * @param isEnabled whether the handelr is enabled
	 */
	protected void setEnabled(boolean isEnabled) {
		if (enabled != isEnabled) {
			enabled = isEnabled;
			fireHandlerChanged(new HandlerEvent(this, true, false));
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		try {
			createOperation(event).run();
		} catch (InvocationTargetException e) {
			Utils.handle(e);
		} catch (InterruptedException e) {
			// Ignore
		}
		return null;
	}

	private ModelProviderOperation createOperation(ExecutionEvent event) {
		ISynchronizePageConfiguration configuration = getConfiguration(event);
		IStructuredSelection structuredSelection = getStructuredSelection(event);
		return createOperation(configuration, structuredSelection);
	}

	/**
	 * Create and return a model provider operation that can perform
	 * the merge operaton.
	 * @param configuration the configuration of the page showing the model
	 * @param structuredSelection the selected elements
	 * @return a model provider operation that can perform
	 * the desired merge operaton
	 */
	protected abstract ModelProviderOperation createOperation(
			ISynchronizePageConfiguration configuration, 
			IStructuredSelection structuredSelection);
}
