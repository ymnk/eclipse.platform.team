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
package org.eclipse.compare.internal;

import org.eclipse.compare.*;
import org.eclipse.compare.contentmergeviewer.ITextMergeViewerContentProvider;
import org.eclipse.compare.structuremergeviewer.*;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.DocumentProviderRegistry;
import org.eclipse.ui.texteditor.IDocumentProvider;

/**
 * Adapts any <code>ContentMergeViewer</code> to work on an <code>ICompareInput</code>
 * e.g. a <code>DiffNode</code>.
 */
public class MergeViewerContentProvider implements ITextMergeViewerContentProvider {
	
	public static final char ANCESTOR_ELEMENT = 'A';
	public static final char RIGHT_ELEMENT = 'R';
	public static final char LEFT_ELEMENT = 'L';
	
	private CompareConfiguration fCompareConfiguration;
	private String fAncestorError;
	private String fLeftError;
	private String fRightError;
	private Viewer fViewer;
		
	public MergeViewerContentProvider(CompareConfiguration cc) {
		fCompareConfiguration= cc;
	}
	
	private boolean hasError() {
		return fAncestorError != null || fLeftError != null || fRightError != null;
	}
	
	public void dispose() {
		// empty default implementation
	}
	
	public void inputChanged(Viewer v, Object o1, Object o2) {
		this.fViewer = v;
	}
	
	//---- ancestor
			
	public void setAncestorError(String errorMessage) {
		fAncestorError= errorMessage;
	}
	
	public String getAncestorLabel(Object element) {
		if (fAncestorError != null)
			return fAncestorError;
		return fCompareConfiguration.getAncestorLabel(element);
	}
	
	public Image getAncestorImage(Object element) {
		if (fAncestorError != null)
			return null;
		return fCompareConfiguration.getAncestorImage(element);
	}
	
	public Object getAncestorContent(Object element) {
		if (element instanceof ICompareInput)
			return ((ICompareInput) element).getAncestor();
		return null;
	}
	
	public boolean showAncestor(Object element) {
		if (element instanceof ICompareInput)
			return true;	// fix for #45239: Show ancestor for incoming and outgoing changes
			//return (((ICompareInput)element).getKind() & Differencer.DIRECTION_MASK) == Differencer.CONFLICTING;
		return false;
	}

	//---- left
					
	public void setLeftError(String errorMessage) {
		fLeftError= errorMessage;
	}
	
	public String getLeftLabel(Object element) {
		if (fLeftError != null)
			return fLeftError;
		return fCompareConfiguration.getLeftLabel(element);
	}
	
	public Image getLeftImage(Object element) {
		if (fLeftError != null)
			return null;
		return fCompareConfiguration.getLeftImage(element);
	}
	
	public Object getLeftContent(Object element) {	
		if (element instanceof ICompareInput)
			return ((ICompareInput) element).getLeft();
		return null;
	}
		
	public boolean isLeftEditable(Object element) {
		if (hasError())
			return false;
		if (element instanceof ICompareInput) {
			Object left= ((ICompareInput) element).getLeft();
			if (left == null) {
				IDiffElement parent= ((IDiffElement)element).getParent();
				if (parent instanceof ICompareInput)
					left= ((ICompareInput) parent).getLeft();
			}
			if (left instanceof IEditableContent)
				return ((IEditableContent)left).isEditable();
		}
		return false;
	}

	public void saveLeftContent(Object element, byte[] bytes) {
		if (element instanceof ICompareInput) {
			ICompareInput node= (ICompareInput) element;
			if (bytes != null) {
				ITypedElement left= node.getLeft();
				// #9869: problem if left is null (because no resource exists yet) nothing is done!
				if (left == null) {
					node.copy(false);
					left= node.getLeft();
				}
				if (left instanceof IEditableContent)
					((IEditableContent)left).setContent(bytes);
				if (node instanceof ResourceCompareInput.MyDiffNode)
					((ResourceCompareInput.MyDiffNode)node).fireChange();
			} else {
				node.copy(false);
			}			
		}
	}
	
	//---- right
	
	public void setRightError(String errorMessage) {
		fRightError= errorMessage;
	}
	
	public String getRightLabel(Object element) {
		if (fRightError != null)
			return fRightError;
		return fCompareConfiguration.getRightLabel(element);
	}
	
	public Image getRightImage(Object element) {
		if (fRightError != null)
			return null;
		return fCompareConfiguration.getRightImage(element);
	}
	
	public Object getRightContent(Object element) {
		if (element instanceof ICompareInput)
			return ((ICompareInput) element).getRight();
		return null;
	}
	
	public boolean isRightEditable(Object element) {
		if (hasError())
			return false;
		if (element instanceof ICompareInput) {
			Object right= ((ICompareInput) element).getRight();
			if (right == null) {
				IDiffContainer parent= ((IDiffElement)element).getParent();
				if (parent instanceof ICompareInput)
					right= ((ICompareInput) parent).getRight();
			}
			if (right instanceof IEditableContent)
				return ((IEditableContent)right).isEditable();
		}
		return false;
	}
	
	public void saveRightContent(Object element, byte[] bytes) {
		if (element instanceof ICompareInput) {
			ICompareInput node= (ICompareInput) element;
			if (bytes != null) {
				ITypedElement right= node.getRight();
				// #9869: problem if right is null (because no resource exists yet) nothing is done!
				if (right == null) {
					node.copy(true);
					right= node.getRight();
				}
				if (right instanceof IEditableContent)
					((IEditableContent)right).setContent(bytes);
				if (node instanceof ResourceCompareInput.MyDiffNode)
					((ResourceCompareInput.MyDiffNode)node).fireChange();
			} else {
				node.copy(true);
			}		
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.contentmergeviewer.ITextMergeViewerContentProvider#getDocumentKey(java.lang.Object)
	 */
	public IEditorInput getDocumentKey(Object element) {
		if (element == null)
			return null;
		IEditorInput input = (IEditorInput)Utilities.getAdapter(element, IEditorInput.class);
		if (input != null)
			return input;
		ISharedDocumentAdapter sda = (ISharedDocumentAdapter)Utilities.getAdapter(element, ISharedDocumentAdapter.class, true);
		if (sda != null) {
			return sda.getDocumentKey(element);
		}
		Object viewerInput = fViewer.getInput();
		if (viewerInput instanceof ICompareInput) {
			ICompareInput ci = (ICompareInput) viewerInput;
			char leg = getLeg(ci, element);
			if (leg != 0)
				return new ThreeWayTypedElementEditorInput(ci, leg);
		}
		return null;
	}
	
	private char getLeg(ICompareInput ci, Object element) {
		// TODO should pass leg to content provider instead of calculation
		if (ci.getLeft() == element)
			return LEFT_ELEMENT;
		if (ci.getRight() == element)
			return RIGHT_ELEMENT;
		if (ci.getAncestor() == element)
			return ANCESTOR_ELEMENT;
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.contentmergeviewer.ITextMergeViewerContentProvider#getDocumentProvider(java.lang.Object)
	 */
	public IDocumentProvider getDocumentProvider(Object element) {
		IEditorInput input = getDocumentKey(element);
		if (input != null)
			return getDocumentProvider(input);
		return null;
	}
	
	private IDocumentProvider getDocumentProvider(IEditorInput input) {
		return DocumentProviderRegistry.getDefault().getDocumentProvider(input);
	}
}

