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
package org.eclipse.jdt.ui.team;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.ui.synchronize.content.SyncInfoLabelProvider;

/**
 * This delegates to the Java element label provider.
 * TODO: What listeners need to be set up?
 */
public class JavaSyncInfoLabelProvider extends SyncInfoLabelProvider {
	
	JavaElementLabelProvider javaProvider = new JavaElementLabelProvider();

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	public void dispose() {
		super.dispose();
		javaProvider.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		if (element instanceof JavaSyncInfoDiffNode) {
			IJavaElement javaElement = ((JavaSyncInfoDiffNode)element).getElement();
			if (javaElement != null) {
				return javaProvider.getImage(javaElement);
			}
		}
		return super.getImage(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		if (element instanceof JavaSyncInfoDiffNode) {
			IJavaElement javaElement = ((JavaSyncInfoDiffNode)element).getElement();
			if (javaElement != null) {
				return javaProvider.getText(javaElement);
			}
		}
		return super.getText(element);
	}

}
