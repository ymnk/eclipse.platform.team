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

import org.eclipse.core.internal.resources.mapping.ResourceMapping;
import org.eclipse.core.internal.resources.mapping.ResourceTraversal;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ccvs.core.CVSSyncInfo;
import org.eclipse.team.internal.ccvs.ui.subscriber.WorkspaceSynchronizeParticipant;
import org.eclipse.team.ui.mapping.IMergeContext;
import org.eclipse.team.ui.mapping.MergeContext;
import org.eclipse.team.ui.synchronize.ISynchronizeScope;
import org.eclipse.team.ui.synchronize.ResourceMappingScope;

public class CVSMergeContext extends MergeContext {
	
	private WorkspaceSynchronizeParticipant participant;

	public static IMergeContext createContext(ResourceMapping[] mappings, ResourceMappingScope scope, IProgressMonitor monitor) {
		WorkspaceSynchronizeParticipant participant = new WorkspaceSynchronizeParticipant(scope);
		participant.refreshNow(participant.getResources(), NLS.bind("Preparing to merge {0}", new String[] { "TODO: mapping description for CVS merge context initialization" }), monitor);
		return new CVSMergeContext(THREE_WAY, participant, mappings);
	}
	
	protected CVSMergeContext(String type, WorkspaceSynchronizeParticipant participant, ResourceMapping[] mappings) {
		super(type, participant.getSyncInfoSet(), mappings);
		this.participant = participant;
	}

	public IStatus markAsMerged(IFile file, IProgressMonitor monitor) {
		try {
			SyncInfo info = getSyncInfoTree().getSyncInfo(file);
			if (info instanceof CVSSyncInfo) {
				CVSSyncInfo cvsInfo = (CVSSyncInfo) info;
				cvsInfo.makeOutgoing(monitor);
			}
			return Status.OK_STATUS;
		} catch (TeamException e) {
			return e.getStatus();
		}
	}
	
	public void dispose() {
		participant.dispose();
		super.dispose();
	}

	public SyncInfo getSyncInfo(IResource resource) throws CoreException {
		return participant.getSubscriber().getSyncInfo(resource);
	}

	public void refresh(ResourceTraversal[] traversals, int flags, IProgressMonitor monitor) throws CoreException {
		IResource[] resources = new ResourceMappingScope("", getResourceMappings(ALL_MAPPINGS), traversals).getRoots();
		participant.refreshNow(resources, "TODO: CVS Merge Context Refresh", monitor);
	}

	public ISynchronizeScope getScope() {
		return (ResourceMappingScope)participant.getScope();
	}

}
