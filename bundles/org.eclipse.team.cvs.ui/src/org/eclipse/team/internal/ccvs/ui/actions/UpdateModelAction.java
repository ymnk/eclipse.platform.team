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
package org.eclipse.team.internal.ccvs.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.internal.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.internal.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.core.subscribers.SubscriberResourceMappingContext;
import org.eclipse.team.ui.mapping.IMergeContext;
import org.eclipse.team.ui.mapping.IResourceMappingManualMerger;
import org.eclipse.team.ui.mapping.ResourceMappingMergeOperation;
import org.eclipse.team.ui.synchronize.ResourceMappingScope;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Action that runs an update without prompting the user for a tag.
 * 
 * @since 3.1
 */
public class UpdateModelAction extends WorkspaceTraversalAction {
    
	private class CVSMergeOperation extends ResourceMappingMergeOperation {

		protected CVSMergeOperation(IWorkbenchPart part, ResourceMapping[] mappings) {
			super(part, mappings);
		}

		protected IMergeContext buildMergeContext(ModelProvider provider, ResourceMapping[] mappings, ResourceMappingScope scope, IProgressMonitor monitor) {
			return CVSMergeContext.createContext(provider, mappings, scope, monitor);
		}

		protected IResourceMappingManualMerger getDefaultManualMerger() {
			// TODO Still need to define how manual merges happen
			return null;
		}

		protected RemoteResourceMappingContext getAncestorContext() {
			return SubscriberResourceMappingContext.getCheckInContext(CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber());
		}

		protected RemoteResourceMappingContext getRemoteContext() {
			return SubscriberResourceMappingContext.getUpdateContext(CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber());
		}
		
	}
	
    /*
     * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForAddedResources()
     */
    protected boolean isEnabledForAddedResources() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForNonExistantResources()
     */
    protected boolean isEnabledForNonExistantResources() {
        return true;
    }
    
	public void execute(IAction action) throws InterruptedException, InvocationTargetException {
		new CVSMergeOperation(getTargetPart(), getCVSResourceMappings()).run();
	}
	
	public String getId() {
		return "org.eclipse.team.cvs.ui.modelupdate";
	}
}
