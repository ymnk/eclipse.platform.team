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
package org.eclipse.team.internal.ui.synchronize.compare;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.team.ui.synchronize.actions.SyncInfoFilter;

public class SyncInfoSetCompareInput extends CompareEditorInput {
	// TODO: when are these objects disposed, and how to hook in to the dispose to free listeners?
	private IResource[] resources;
	private ISyncInfoSet set;
	private ITeamSubscriberSyncInfoSets sets;

	public SyncInfoSetCompareInput(CompareConfiguration configuration, IResource[] resources, SyncInfoFilter filter, ITeamSubscriberSyncInfoSets sets) {
		super(configuration);
		this.resources = resources;
		this.sets = sets;
		if(resources != null && resources.length > 0) {
			this.set = sets.createNewFilteredSyncSet(resources, filter);
		} else {
			this.set = sets.getSubscriberSyncSet();
		}
	}

	public Viewer createDiffViewer(Composite parent) {
		SyncInfoDiffTreeViewer v = new SyncInfoDiffTreeViewer(parent, sets.getParticipant(), set);
		v.updateCompareEditorInput(this);
		v.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				ISelection s = event.getSelection();
				SyncInfoDiffNode node = getElement(s);
				if(node != null && node.getResource().getType() == IResource.FILE) {
					Utils.updateLabels(node.getSyncInfo(), getCompareConfiguration());
				}
			}
		});
		v.setAcceptParticipantMenuContributions(allowParticipantMenuContributions());
		return v;
	}

	protected Object prepareInput(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		return new SyncInfoDiffNode(set, ResourcesPlugin.getWorkspace().getRoot());
	}
	
	private static SyncInfoDiffNode getElement(ISelection selection) {
		if (selection != null && selection instanceof IStructuredSelection) {
			IStructuredSelection ss= (IStructuredSelection) selection;
			if (ss.size() == 1) {
				Object o = ss.getFirstElement();
				if(o instanceof SyncInfoDiffNode) {
					return (SyncInfoDiffNode)o;
				}
			}
		}
		return null;
	}
	
	protected boolean allowParticipantMenuContributions() {
		return false;
	}
}
