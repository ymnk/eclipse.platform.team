/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.team.internal.ui.widgets;

import org.eclipse.compare.*;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.ui.synchronize.SyncInfoDiffNode;

public class TextChangePreviewViewer implements IChangePreviewViewer {

	private ComparePreviewer fViewer;
	

	private static class ComparePreviewer extends CompareViewerSwitchingPane {
		private CompareConfiguration fCompareConfiguration;
		public ComparePreviewer(Composite parent) {
			super(parent, SWT.BORDER | SWT.FLAT, true);
			fCompareConfiguration= new CompareConfiguration();
			fCompareConfiguration.setLeftEditable(false);
			fCompareConfiguration.setLeftLabel("Local File"); //$NON-NLS-1$
			fCompareConfiguration.setRightEditable(false);
			fCompareConfiguration.setRightLabel("Remote File"); //$NON-NLS-1$
		}
		protected Viewer getViewer(Viewer oldViewer, Object input) {
			String localText = "Local File:";
			String remoteText = "Remote File:";
			if(input instanceof SyncInfoDiffNode) {
			}
			return CompareUI.findContentViewer(oldViewer, (ICompareInput)input, this, fCompareConfiguration);
		}
		public void setText(String text) {
			/*
			Object input= getInput();
			if (input instanceof CompareInput) {
				CompareInput cInput= (CompareInput)input;
				setImage(fLabelProvider.getImage(cInput.getChangeElement()));
				super.setText(fLabelProvider.getText(cInput.getChangeElement()));
			} else {
				super.setText(text);
				setImage(null);
			}
			*/
			super.setText(text);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.refactoring.IChangePreviewViewer#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		fViewer= new ComparePreviewer(parent);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.refactoring.IChangePreviewViewer#getControl()
	 */
	public Control getControl() {
		return fViewer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.refactoring.IChangePreviewViewer#setInput(org.eclipse.jdt.internal.ui.refactoring.ChangeElement)
	 */
	public void setInput(Object input) throws CoreException {
		fViewer.setInput(input);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.refactoring.IChangePreviewViewer#refresh()
	 */
	public void refresh() {
		fViewer.getViewer().refresh();
	}
}
