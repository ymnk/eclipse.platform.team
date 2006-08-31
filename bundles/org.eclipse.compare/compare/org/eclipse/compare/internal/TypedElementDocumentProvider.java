/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import java.io.*;

import org.eclipse.compare.*;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.texteditor.AbstractDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;

/**
 * An {@link IDocumentProvider} that can be used to share the
 * document associated with an {@link ITypedElement}
 */
public class TypedElementDocumentProvider extends AbstractDocumentProvider {

	/**
	 * An {@link IEditorInput} that is used as the document key.
	 */
	public static class TypedElementEditorInput extends PlatformObject implements IEditorInput {

		private final ICompareInput compareInput;
		private final boolean left;
		
		public TypedElementEditorInput(ICompareInput compareInput, boolean left) {
			this.compareInput = compareInput;
			this.left = left;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.ui.IEditorInput#exists()
		 */
		public boolean exists() {
			return true;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
		 */
		public ImageDescriptor getImageDescriptor() {
			Image image = null;
			if (getTypedElement() != null)
				image = getTypedElement().getImage();
			if (image == null)
				image = compareInput.getImage();
			return ImageDescriptor.createFromImage(image);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.IEditorInput#getName()
		 */
		public String getName() {
			if (getTypedElement() != null)
				return getTypedElement().getName();
			return compareInput.getName();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.IEditorInput#getPersistable()
		 */
		public IPersistableElement getPersistable() {
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.IEditorInput#getToolTipText()
		 */
		public String getToolTipText() {
			return getName();
		}
		
		public ITypedElement getTypedElement() {
			if (left) {
				return compareInput.getLeft();
			}
			return compareInput.getRight();
		}
		
		public ITypedElement getOtherElement() {
			if (left) {
				return compareInput.getRight();
			}
			return compareInput.getLeft();
		}

		public ICompareInput getCompareInput() {
			return compareInput;
		}

		public boolean isLeft() {
			return left;
		}
		
		public String getEncoding() {
			String encoding = getStreamEncoding(getTypedElement());
			if (encoding != null)
				return encoding;
			encoding = getStreamEncoding(getOtherElement());
			if (encoding != null)
				return encoding;
			return null;
		}
		
		public InputStream getContents() throws CoreException {
			ITypedElement element = getTypedElement();
			if (element instanceof IStreamContentAccessor) {
				IStreamContentAccessor accessor = (IStreamContentAccessor) element;
				return accessor.getContents();
			}
			return null;
		}
		
		private static String getStreamEncoding(Object o) {
			if (o instanceof IEncodedStreamContentAccessor) {
				try {
					return ((IEncodedStreamContentAccessor)o).getCharset();
				} catch (CoreException e) {
					// silently ignored
				}
			}
			return null;
		}
		
	}
	
	/**
	 * Default file size.
	 */
	private static final int DEFAULT_FILE_SIZE= 15 * 1024;
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractDocumentProvider#createAnnotationModel(java.lang.Object)
	 */
	protected IAnnotationModel createAnnotationModel(Object element)
			throws CoreException {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractDocumentProvider#createDocument(java.lang.Object)
	 */
	protected IDocument createDocument(Object element) throws CoreException {
		if (element instanceof TypedElementEditorInput) {
			IDocument document= createEmptyDocument();
			if (setDocumentContent(document, (TypedElementEditorInput) element)) {
				setupDocument(element, document);
				return document;
			}
		}

		return null;
	}

	protected void doSaveDocument(IProgressMonitor monitor, Object element,
			IDocument document, boolean overwrite) throws CoreException {
		// TODO: Code is copied from ContentMergeViewer but will only be usable by TextMergeViewers.
		// We should generalize the code and put it in a place that is accessible from both code paths
		if (element instanceof TypedElementEditorInput) {
			TypedElementEditorInput editorInput = (TypedElementEditorInput) element;
			ICompareInput compareInput= editorInput.getCompareInput();
			byte[] bytes;
			try {
				bytes = document.get().getBytes(getEncoding(element));
			} catch (UnsupportedEncodingException e) {
				CompareUIPlugin.log(e);
				bytes = document.get().getBytes();
			}
			ITypedElement typedElement= editorInput.getTypedElement();
			if (typedElement == null) {
				// If there is no element on target side, copy the element from the other side
				compareInput.copy(!editorInput.isLeft());
				typedElement= editorInput.getTypedElement();
			}
			if (typedElement instanceof IEditableContent)
				((IEditableContent)typedElement).setContent(bytes);
			// TODO: I would like to removed the following hack if possible
			if (compareInput instanceof ResourceCompareInput.MyDiffNode)
				((ResourceCompareInput.MyDiffNode)compareInput).fireChange();		
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractDocumentProvider#getOperationRunner(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IRunnableContext getOperationRunner(IProgressMonitor monitor) {
		return null;
	}
	
	private String getEncoding(Object element) {
		if (element instanceof TypedElementEditorInput) {
			TypedElementEditorInput editorInput = (TypedElementEditorInput) element;
			String encoding = editorInput.getEncoding();
			if (encoding != null)
				return encoding;
		}
		return getDefaultEncoding();
	}
	
	private void setupDocument(Object element, IDocument document) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Factory method for creating empty documents.
	 * @return the newly created document
	 */
	private IDocument createEmptyDocument() {
		return new Document();
	}
	
	/**
	 * Initializes the given document from the given editor input using the given character encoding.
	 *
	 * @param document the document to be initialized
	 * @param editorInput the input from which to derive the content of the document
	 * @return <code>true</code> if the document content could be set, <code>false</code> otherwise
	 * @throws CoreException if the given editor input cannot be accessed
	 */
	private boolean setDocumentContent(IDocument document, TypedElementEditorInput editorInput) throws CoreException {
		InputStream stream= editorInput.getContents();
		if (stream == null)
			return false;
		try {
			setDocumentContent(document, stream, getEncoding(editorInput));
		} finally {
			try {
				stream.close();
			} catch (IOException x) {
				// Ignore exception on close
			}
		}
		return true;
		
	}
	
	/**
	 * Initializes the given document with the given stream using the given encoding.
	 *
	 * @param document the document to be initialized
	 * @param contentStream the stream which delivers the document content
	 * @param encoding the character encoding for reading the given stream
	 * @throws CoreException if the given stream can not be read
	 */
	private void setDocumentContent(IDocument document, InputStream contentStream, String encoding) throws CoreException {

		Reader in= null;

		try {

			if (encoding == null)
				encoding= getDefaultEncoding();

			in= new BufferedReader(new InputStreamReader(contentStream, encoding), DEFAULT_FILE_SIZE);
			StringBuffer buffer= new StringBuffer(DEFAULT_FILE_SIZE);
			char[] readBuffer= new char[2048];
			int n= in.read(readBuffer);
			while (n > 0) {
				buffer.append(readBuffer, 0, n);
				n= in.read(readBuffer);
			}

			document.set(buffer.toString());

		} catch (IOException x) {
			String message= (x.getMessage() != null ? x.getMessage() : ""); //$NON-NLS-1$
			IStatus s= new Status(IStatus.ERROR, CompareUIPlugin.PLUGIN_ID, IStatus.OK, message, x);
			throw new CoreException(s);
		} finally {
			try {
				if (in != null)
					in.close();
				else
					contentStream.close();
			} catch (IOException x) {
				// Ignore exceptions on close
			}
		}
	}
	
	private String getDefaultEncoding() {
		return ResourcesPlugin.getEncoding();
	}

}
