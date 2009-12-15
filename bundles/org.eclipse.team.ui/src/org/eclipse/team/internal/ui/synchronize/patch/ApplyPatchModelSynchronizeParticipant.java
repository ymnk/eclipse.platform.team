/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.patch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.mapping.SynchronizationActionProvider;
import org.eclipse.team.ui.synchronize.*;

public class ApplyPatchModelSynchronizeParticipant extends
		ModelSynchronizeParticipant {

	public static final String ID = "org.eclipse.team.ui.applyPatchModelParticipant"; //$NON-NLS-1$

	public ApplyPatchModelSynchronizeParticipant(SynchronizationContext context) {
		super(context);
		init();
	}

	private void init() {
		try {
			ISynchronizeParticipantDescriptor descriptor = TeamUI
					.getSynchronizeManager().getParticipantDescriptor(ID);
			setInitializationData(descriptor);
			setSecondaryId(Long.toString(System.currentTimeMillis()));
		} catch (CoreException e) {
			// ignore
		}
	}

	protected void initializeConfiguration(
			final ISynchronizePageConfiguration configuration) {
		super.initializeConfiguration(configuration);
		configuration
				.setSupportedModes(ISynchronizePageConfiguration.INCOMING_MODE
						| ISynchronizePageConfiguration.CONFLICTING_MODE);
		configuration.setMode(ISynchronizePageConfiguration.INCOMING_MODE);
	}

	protected ModelSynchronizeParticipantActionGroup createMergeActionGroup() {
		return new ApplyPatchModelSynchronizeParticipantActionGroup();
	}

	public class ApplyPatchModelSynchronizeParticipantActionGroup extends
			ModelSynchronizeParticipantActionGroup {
		protected void configureMergeAction(String mergeActionId, Action action) {
			if (mergeActionId == SynchronizationActionProvider.MERGE_ACTION_ID) {
				// Custom label for merge
				action.setText("Apply (merge)"); //$NON-NLS-1$
			} else if (mergeActionId == SynchronizationActionProvider.OVERWRITE_ACTION_ID) {
				// Custom label for overwrite
				action.setText("Apply (overwrite)"); //$NON-NLS-1$
			} else if (mergeActionId == SynchronizationActionProvider.MARK_AS_MERGE_ACTION_ID) {
				// Custom label for mark-as-merged
				action.setText("Exclude"); //$NON-NLS-1$
			} else {
				super.configureMergeAction(mergeActionId, action);
			}
		}
	}
}
