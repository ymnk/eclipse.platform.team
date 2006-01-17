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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.diff.*;
import org.eclipse.team.core.mapping.IMergeContext;
import org.eclipse.team.internal.ui.Utils;

public class ResourceMergeHandler extends MergeActionHandler {
	
	private final boolean overwrite;

	public ResourceMergeHandler(boolean overwrite) {
		this.overwrite = overwrite;
	}

	public Object execute(final ExecutionEvent event) throws ExecutionException {
		try {
			new ResourceModelProviderOperation(getConfiguration()) {
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					try {
						IMergeContext context = (IMergeContext)getContext();
						IDiffNode[] diffs = getFileDeltas(getStructuredSelection(event));
						IStatus status = context.merge(diffs, overwrite, monitor);
						if (!status.isOK())
							throw new CoreException(status);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}

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
			
			}.run();
		} catch (InvocationTargetException e) {
			Utils.handle(e);
		} catch (InterruptedException e) {
			// Ignore
		}
		return null;
	}

}
