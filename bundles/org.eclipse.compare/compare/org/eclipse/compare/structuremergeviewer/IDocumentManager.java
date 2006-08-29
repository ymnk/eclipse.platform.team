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
package org.eclipse.compare.structuremergeviewer;

import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.IDocumentProvider;

/**
 * Interface for an object that manages the life cycle of shared
 * documents that are used to display and edit the text of a comparison.
 * @since 3.3
 */
public interface IDocumentManager {
	
	/**
	 * Return the document associated with the given element or
	 * <code>null</code> if this manager does not have a document
	 * for the given element. Clients who wish to share a document
	 * with this manager can call {@link #createDocument(Object, String)}
	 * in order to have the manager create a document for the element.
	 * If the manager was able to create a document, a subsequent call to {@link #getDocument(Object)}
	 * will return the document.
	 * @param element the element
	 * @return the document for the element
	 */
	IDocument getDocument(Object element);
	
	/**
	 * Create a document for the given element. The manager will attempt to
	 * obtain a document from an {@link IDocumentProvider}. If unable to do
	 * so, the manager will then try to access the contents of the element
	 * via the {@link IStreamContentAccessor} interface and create a document
	 * from that. If both of these fail, an empty document will be created.
	 * @param element the element
	 * @param encoding the encoding for the contents or <code>null</code>
	 * 	to indicate that the manager should try and determine the encoding.
	 * @throws CoreException
	 */
	void createDocument(Object element, String encoding) throws CoreException;

}
