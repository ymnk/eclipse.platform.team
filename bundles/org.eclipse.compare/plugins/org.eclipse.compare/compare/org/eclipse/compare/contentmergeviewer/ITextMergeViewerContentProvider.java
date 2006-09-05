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
package org.eclipse.compare.contentmergeviewer;

import org.eclipse.compare.ITypedElement;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.DocumentProviderRegistry;
import org.eclipse.ui.texteditor.IDocumentProvider;

/**
 * An extension to {@link IMergeViewerContentProvider} that supports the
 * use of shared documents (i.e. file buffers) when edited file contents.
 * 
 * <p>
 * This interface may be implemented by clients.
 * TODO: Should we make an abstract class available?
 * TODO: Or should we make this internal at least to begin with?
 * 
 * @since 3.3
 */
public interface ITextMergeViewerContentProvider extends IMergeViewerContentProvider {

	/**
	 * Return the key that can be used to obtain a document provider
	 * from the {@link DocumentProviderRegistry} and a document from an
	 * {@link IDocumentProvider} for the given {@link ITypedElement}.
	 * @param element the element
	 * @return the key that can be used to obtain the document provider and document
	 * for the given element
	 */
	IEditorInput getDocumentKey(Object element);
	
	/**
	 * Return the document provider for the given element.
	 * The element will be converted to a document key by calling
	 * {@link #getDocumentKey(Object)} and then the document provider
	 * will be obtained from the {@link DocumentProviderRegistry}
	 * using this key.
	 * @param element the element
	 * @return the document provider for the given element
	 */
	IDocumentProvider getDocumentProvider(Object element);
	
}
