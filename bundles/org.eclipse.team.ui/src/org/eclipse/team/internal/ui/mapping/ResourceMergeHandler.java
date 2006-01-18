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

import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.diff.*;
import org.eclipse.team.core.mapping.IMergeContext;
import org.eclipse.team.ui.mapping.ModelProviderOperation;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.navigator.IExtensionStateModel;

public class ResourceMergeHandler extends SynchronizationActionHandler {
	
	private final boolean overwrite;

	public ResourceMergeHandler(IExtensionStateModel model, boolean overwrite) {
		super(model);
		this.overwrite = overwrite;
	}

	protected ModelProviderOperation createOperation(ISynchronizePageConfiguration configuration, IStructuredSelection structuredSelection) {
		return new ResourceModelProviderOperation(configuration, structuredSelection.toArray()) {
			/* (non-Javadoc)
			 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
			 */
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				try {
					IMergeContext context = (IMergeContext)getContext();
					IDiffNode[] diffs = getFileDeltas(getElements());
					IStatus status = context.merge(diffs, overwrite, monitor);
					if (!status.isOK())
						throw new CoreException(status);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			}
			/* (non-Javadoc)
			 * @see org.eclipse.team.internal.ui.mapping.ResourceModelProviderOperation#getDiffFilter()
			 */
			protected FastDiffNodeFilter getDiffFilter() {
				return new FastDiffNodeFilter() {
					public boolean select(IDiffNode node) {
						if (node instanceof IThreeWayDiff) {
							IThreeWayDiff twd = (IThreeWayDiff) node;
							if ((twd.getDirection() == IThreeWayDiff.OUTGOING && overwrite) || twd.getDirection() == IThreeWayDiff.CONFLICTING || twd.getDirection() == IThreeWayDiff.INCOMING) {
								return true;
							}
						}
						return false;
					}
				};
			}
		};
	}

}
