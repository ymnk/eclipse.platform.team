/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.*;

public class CompareAction extends TeamAction {

	
	protected void execute(IAction action) throws InvocationTargetException,
			InterruptedException {
		
		IResource[] selectedResources = getSelectedResources();
		
		if (selectedResources.length == 2) {

			ITypedElement left = null;
			if (selectedResources[0] != null) {
				left = getElementFor(selectedResources[0]);
			}

			ITypedElement right = null;
			if (selectedResources[1] != null) {
				right = getElementFor(selectedResources[1]);
			}

			openInCompare(left, right);
		}
	}
	
	//XXX: from CompareRevisionAction*
	private void openInCompare(ITypedElement left, ITypedElement right) {
		IWorkbenchPage workBenchPage = getTargetPage();
		CompareEditorInput input = createCompareEditorInput(left, right, workBenchPage);
		IEditorPart editor = CompareRevisionAction.findReusableCompareEditor(workBenchPage);
		if (editor != null) {
			IEditorInput otherInput = editor.getEditorInput();
			if (otherInput.equals(input)) {
				// simply provide focus to editor
				workBenchPage.activate(editor);
			} else {
				// if editor is currently not open on that input either re-use
				// existing
				CompareUI.reuseCompareEditor(input, (IReusableEditor) editor);
				workBenchPage.activate(editor);
			}
		} else {
			CompareUI.openCompareEditor(input);
		}
	}
	
	protected AbstractSaveableCompareEditorInput createCompareEditorInput(
			ITypedElement left, ITypedElement right, IWorkbenchPage page) {
		return new TwoSidesSaveableCompareEditorInput(left,
				right, page);
	}
	
	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);
	}
	
	//XXX: from CompareRevisionAction
	private ITypedElement getElementFor(IResource resource) {
		return SaveableCompareEditorInput.createFileElement((IFile)resource);
	}

}
