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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.eclipse.team.internal.ui.TeamUIPlugin;

public class PatchedFileVariantComparator implements IResourceVariantComparator {

	public boolean compare(IResource local, IResourceVariant remote) {
		/*
		 * Comparing local with base returns true, see
		 * org.eclipse.team.core.synchronize.SyncInfo.calculateKind()
		 */
		IStorage s = null;
		try {
			s = remote.getStorage(null);
		} catch (TeamException e) {
			TeamUIPlugin.log(e);
		}
		return local.equals(s);
		// return false;
	}

	public boolean compare(IResourceVariant base, IResourceVariant remote) {
		return false;
	}

	public boolean isThreeWay() {
		return true;
	}
}
