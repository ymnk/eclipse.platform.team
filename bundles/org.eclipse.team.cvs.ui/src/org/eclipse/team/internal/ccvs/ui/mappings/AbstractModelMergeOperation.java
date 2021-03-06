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
package org.eclipse.team.internal.ccvs.ui.mappings;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.ui.synchronize.ModelParticipantMergeOperation;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;
import org.eclipse.ui.IWorkbenchPart;

public abstract class AbstractModelMergeOperation extends ModelParticipantMergeOperation {
	
	private boolean ownsManager = false;
	
	public AbstractModelMergeOperation(IWorkbenchPart part, ISynchronizationScopeManager manager, boolean ownsManager) {
		super(part, manager);
		this.ownsManager = ownsManager;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.TeamOperation#canRunAsJob()
	 */
	protected boolean canRunAsJob() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.operations.ResourceMappingMergeOperation#isPreviewInDialog()
	 */
	protected boolean isPreviewInDialog() {
		return CVSUIPlugin.getPlugin().getPreferenceStore().getString(ICVSUIConstants.PREF_UPDATE_PREVIEW).equals(ICVSUIConstants.PREF_UPDATE_PREVIEW_IN_DIALOG);
	}
	
	protected void endOperation(IProgressMonitor monitor) throws InvocationTargetException {
		if (ownsManager) {
			ISynchronizationScopeManager manager = getScopeManager();
			manager.dispose();
		}
		super.endOperation(monitor);
	}
	
	protected ModelSynchronizeParticipant createParticipant() {
		ModelSynchronizeParticipant participant = super.createParticipant();
		// Transfer ownership of the manager to the participant
		setOwnsManager(false);
		return participant;
	}

	public boolean isOwnsManager() {
		return ownsManager;
	}

	public void setOwnsManager(boolean ownsManager) {
		this.ownsManager = ownsManager;
	}

}
