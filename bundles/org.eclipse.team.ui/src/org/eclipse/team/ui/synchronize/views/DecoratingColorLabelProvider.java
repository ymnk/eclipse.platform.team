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
package org.eclipse.team.ui.synchronize.views;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Color;

/**
 * Should PR UI to fix their decorating provider!!!
 */
public class DecoratingColorLabelProvider extends DecoratingLabelProvider implements IColorProvider {

	public DecoratingColorLabelProvider(ILabelProvider provider, ILabelDecorator decorator) {
		super(provider, decorator);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
	 */
	public Color getForeground(Object element) {
		ILabelProvider p = getLabelProvider();
		if(p instanceof IColorProvider) {
			return ((IColorProvider)p).getForeground(element);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.Object)
	 */
	public Color getBackground(Object element) {
		ILabelProvider p = getLabelProvider();
		if(p instanceof IColorProvider) {
			return ((IColorProvider)p).getBackground(element);
		}
		return null;
	}
}
