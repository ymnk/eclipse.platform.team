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
package org.eclipse.team.internal.ccvs.ui.operations;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoTree;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.*;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.resources.*;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Operation that ensures that the contents for base
 * of each local resource is cached.
 */
public class CacheBaseContentsOperation extends SingleCommandOperation {

	private final SyncInfoTree tree;
	private final boolean includeOutgoing;

	public CacheBaseContentsOperation(IWorkbenchPart part, ResourceMapping[] mappers, LocalOption[] options, SyncInfoTree tree, boolean includeOutgoing) {
		super(part, mappers, options);
		this.tree = tree;
		this.includeOutgoing = includeOutgoing;
	}

	protected void execute(CVSTeamProvider provider, IResource[] resources, boolean recurse, IProgressMonitor monitor) throws CVSException, InterruptedException {
		IResource[] files = getFilesWithUncachedContents(resources, recurse);
		if (files.length > 0)
			super.execute(provider, files, recurse, monitor);
	}
	
	private IResource[] getFilesWithUncachedContents(IResource[] resources, boolean recurse) {
		ArrayList result = new ArrayList();
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			SyncInfo[] infos = tree.getSyncInfos(resource, recurse ? IResource.DEPTH_INFINITE: IResource.DEPTH_ONE);
			for (int j = 0; j < infos.length; j++) {
				SyncInfo info = infos[j];
				IResource local = info.getLocal();
				IResourceVariant base = info.getBase();
				if (base != null && local.getType() == IResource.FILE) {
					int direction = SyncInfo.getDirection(info.getKind());
					if (isEnabledForDirection(direction)) {
			            if (base instanceof RemoteFile) {
			                RemoteFile remote = (RemoteFile) base;
			                if (!remote.isContentsCached()) {
			                	result.add(local);
			                }
			            }
					}
				}
			}
		}
		return (IResource[]) result.toArray(new IResource[result.size()]);
	}

	/**
	 * By default, this operation will only fetch the base for
	 * files that are conflicting.
	 * @param direction the change direction
	 * @return whether the operation is enabled for the given change direction
	 */
	protected boolean isEnabledForDirection(int direction) {
		return direction == SyncInfo.CONFLICTING || 
		(includeOutgoing && direction == SyncInfo.OUTGOING);
	}

	/* (non-Javadoc)
	 * 
	 * Use a local root that is really the base tree so we can cache
	 * the base contents without affecting the local contents.
	 * 
	 * @see org.eclipse.team.internal.ccvs.ui.operations.RepositoryProviderOperation#getLocalRoot(org.eclipse.team.internal.ccvs.core.CVSTeamProvider)
	 */
	protected ICVSFolder getLocalRoot(CVSTeamProvider provider)
			throws CVSException {
		RemoteFolderTree tree = RemoteFolderTreeBuilder.buildBaseTree(
				(CVSRepositoryLocation) getRemoteLocation(provider),
				CVSWorkspaceRoot.getCVSFolderFor(provider.getProject()), null,
				new NullProgressMonitor());
		return tree;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.RepositoryProviderOperation#getCVSArguments(org.eclipse.core.resources.IResource[])
	 */
	protected ICVSResource[] getCVSArguments(Session session, IResource[] resources) {
        List result = new ArrayList();
        for (int i = 0; i < resources.length; i++) {
            IResource resource = resources[i];
            try {
				ICVSResource file = session.getLocalRoot().getChild(resource.getProjectRelativePath().toString());
				result.add(file);
			} catch (CVSException e) {
				// Log and continue
				CVSUIPlugin.log(e);
			}
        }

        return (ICVSResource[]) result.toArray(new ICVSResource[result.size()]);
	}
	
	protected IStatus executeCommand(Session session, CVSTeamProvider provider, ICVSResource[] resources, boolean recurse, IProgressMonitor monitor) throws CVSException, InterruptedException {
		return Command.UPDATE.execute(
                session,
                Command.NO_GLOBAL_OPTIONS,
                getLocalOptions(true),
                resources,
                null,
                monitor);
	}

	protected LocalOption[] getLocalOptions(boolean recurse) {
		return Update.IGNORE_LOCAL_CHANGES.addTo(super.getLocalOptions(recurse));
	}
	protected String getTaskName(CVSTeamProvider provider) {
		return NLS.bind("Fetching contents for changed files in {0}", new String[] {provider.getProject().getName()});
	}

	protected String getTaskName() {
		return "Fetching contents for changed files";
	}

}
