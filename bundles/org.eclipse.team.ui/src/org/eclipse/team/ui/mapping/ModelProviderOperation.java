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
package org.eclipse.team.ui.mapping;

import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.ui.operations.AbstractResourceMappingOperation;
import org.eclipse.team.ui.operations.ModelSynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizePageSite;
import org.eclipse.ui.IWorkbenchPart;

public abstract class ModelProviderOperation extends AbstractResourceMappingOperation {

	private final ISynchronizePageConfiguration configuration;

	/*
	 * Helper method for extracting the part safely from a configuration
	 */
	private static IWorkbenchPart getPart(ISynchronizePageConfiguration configuration) {
		if (configuration != null) {
			ISynchronizePageSite site = configuration.getSite();
			if (site != null) {
				return site.getPart();
			}
		}
		return null;
	}
	
	protected ModelProviderOperation(ISynchronizePageConfiguration configuration) {
		super(getPart(configuration));
		this.configuration = configuration;
	}

	public ISynchronizePageConfiguration getConfiguration() {
		return configuration;
	}

	/**
	 * Return the synchronization context associated with this action.
	 * @return the synchronization context associated with this action
	 */
	protected ISynchronizationContext getContext() {
		return ((ModelSynchronizeParticipant)getConfiguration().getParticipant()).getContext();
	}


}
