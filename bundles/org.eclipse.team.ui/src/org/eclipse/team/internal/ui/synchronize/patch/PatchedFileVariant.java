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

import java.io.InputStream;

import org.eclipse.compare.internal.core.patch.FileDiffResult;
import org.eclipse.compare.internal.core.patch.FilePatch2;
import org.eclipse.compare.internal.patch.WorkspacePatcher;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;

public class PatchedFileVariant implements IResourceVariant {

	private FilePatch2 diff;
	private WorkspacePatcher patcher;

	public PatchedFileVariant(WorkspacePatcher patcher, FilePatch2 diff) {
		this.diff = diff;
		this.patcher = patcher;
	}

	public byte[] asBytes() {
		// We don't persist the variant between sessions.
		return null;
	}

	public String getContentIdentifier() {
		// TODO: use patch file name (?)
		// currently it's displayed as 'Remote File (After Patch)' for the right side :/
		return "(After Patch)"; //$NON-NLS-1$
	}

	public String getName() {
		// TODO: is it ever used?
		return diff.getPath(patcher.isReversed()).lastSegment()
				+ "(patched) <IResourceVariant>"; //$NON-NLS-1$
	}

	public IStorage getStorage(IProgressMonitor monitor) throws TeamException {
		return new PatchedFileStorage(diff, patcher);
	}

	public boolean isContainer() {
		return false;
	}

	public static class PatchedFileStorage implements IStorage {
		private FilePatch2 diff;
		private WorkspacePatcher patcher;

		public PatchedFileStorage(FilePatch2 diff, WorkspacePatcher patcher) {
			this.diff = diff;
			this.patcher = patcher;
		}

		public Object getAdapter(Class adapter) {
			return null;
		}

		public boolean isReadOnly() {
			return true;
		}

		public String getName() {
			// TODO: what is it used for?
			return diff.getPath(patcher.isReversed()).lastSegment()
					+ "(patched) <IStorage>"; //$NON-NLS-1$
		}

		public IPath getFullPath() {
			return null;
		}

		public InputStream getContents() throws CoreException {
			FileDiffResult diffResult = patcher.getDiffResult(diff);
			return diffResult.getPatchedContents();
		}
	}
}
