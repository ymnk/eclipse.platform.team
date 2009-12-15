/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.patch;

import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.ui.mapping.SynchronizationCompareAdapter;
import org.eclipse.ui.IMemento;

public class PatchCompareAdapter extends SynchronizationCompareAdapter {

	public ICompareInput asCompareInput(ISynchronizationContext context,
			Object o) {
		if (o instanceof ICompareInput)
			return (ICompareInput) o;
		return super.asCompareInput(context, o);
	}

	public void save(ResourceMapping[] mappings, IMemento memento) {
		// TODO Auto-generated method stub

	}

	public ResourceMapping[] restore(IMemento memento) {
		// TODO Auto-generated method stub
		return null;
	}

}
