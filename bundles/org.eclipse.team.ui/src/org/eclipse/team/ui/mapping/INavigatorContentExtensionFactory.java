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
package org.eclipse.team.ui.mapping;

import org.eclipse.jface.viewers.ILabelProvider;

/**
 * Factory for creating a NavigatorContentProvider for
 * a given team context.
 * 
 * WARNING: This class is part of a provision API and is subject to change
 * until the release is final.
 * 
 * @since 3.2
 */
public interface INavigatorContentExtensionFactory {

	public NavigatorContentExtension createProvider(ITeamViewerContext context);
	
	/**
	 * TODO: Should not need this but I added it to make it work
	 */
	public ILabelProvider getLabelProvider();
}
