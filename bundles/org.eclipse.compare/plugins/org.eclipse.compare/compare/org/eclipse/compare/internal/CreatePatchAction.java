/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import java.lang.reflect.Field;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.ContentMergeViewer;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.compare.internal.merge.DocumentMerger;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;

public class CreatePatchAction extends Action {

	private TextMergeViewer viewer;
	private boolean rightToLeft;

	public CreatePatchAction(TextMergeViewer viewer, boolean rightToLeft) {
		super(CompareMessages.CreatePatchActionTitle);
		this.viewer = viewer;
		this.rightToLeft = rightToLeft;
	}

	public void run() {
		SaveDiffFileWizard.run(getMerger(), getDocument(true),
				getDocument(false), getLabel(true), getLabel(false),
				getPath(true), getPath(false), viewer.getControl().getShell(),
				rightToLeft);
	}

	private String getPath(boolean left) {
		try {
			Field ciField = null;
			if (left) {
				ciField = TextMergeViewer.class
						.getDeclaredField("fLeftContributor"); //$NON-NLS-1$
			} else {
				ciField = TextMergeViewer.class
						.getDeclaredField("fRightContributor"); //$NON-NLS-1$
			}
			ciField.setAccessible(true);
			Object ciObj = ciField.get(viewer);
			Class clazz = ciObj.getClass();
			Field field = clazz.getDeclaredField("fDocumentKey"); //$NON-NLS-1$
			field.setAccessible(true);
			IEditorInput editorInput = (IEditorInput) field.get(ciObj);
			if (editorInput instanceof FileEditorInput) {
				FileEditorInput fei = (FileEditorInput) editorInput;
				String path = fei.getFile().getProjectRelativePath().toString();
				return path;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ""; //$NON-NLS-1$
	}

	private String getLabel(boolean left) {
		try {
			Field field = ContentMergeViewer.class
					.getDeclaredField("fCompareConfiguration"); //$NON-NLS-1$
			field.setAccessible(true);
			CompareConfiguration cc = (CompareConfiguration) field.get(viewer);
			if (left) {
				field = CompareConfiguration.class
						.getDeclaredField("fLeftLabel"); //$NON-NLS-1$
			} else {
				field = CompareConfiguration.class
						.getDeclaredField("fRightLabel"); //$NON-NLS-1$
			}
			field.setAccessible(true);
			return (String) field.get(cc);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public DocumentMerger getMerger() {
		try {
			Field field = TextMergeViewer.class.getDeclaredField("fMerger"); //$NON-NLS-1$
			field.setAccessible(true);
			return (DocumentMerger) field.get(viewer);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private IDocument getDocument(boolean left) {
		try {
			Field field = null;
			if (left) {
				field = TextMergeViewer.class.getDeclaredField("fLeft"); //$NON-NLS-1$
			} else {
				field = TextMergeViewer.class.getDeclaredField("fRight"); //$NON-NLS-1$
			}
			field.setAccessible(true);
			MergeSourceViewer msv = (MergeSourceViewer) field.get(viewer);
			field = MergeSourceViewer.class
					.getDeclaredField("fRememberedDocument"); //$NON-NLS-1$
			field.setAccessible(true);
			return (IDocument) field.get(msv);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
