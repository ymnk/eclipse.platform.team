/*******************************************************************************
 * Copyright (c) 2008 Aleksandra Wozniak and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Aleksandra Wozniak (aleksandra.k.wozniak@gmail.com) - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

/**
 * The "Compare with other resource" action
 * @since 3.4
 */
public class CompareWithOtherResourceAction implements IObjectActionDelegate {

	Shell shell;
	CompareWithOtherResourceDialog dialog;
	private ISelection fselection;

	public CompareWithOtherResourceAction() {
		shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	public void run(IAction action) {
		dialog = new CompareWithOtherResourceDialog(shell, fselection);
		dialog.open();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		fselection = selection;
	}

}
