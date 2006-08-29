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

import java.util.*;

import org.eclipse.compare.*;
import org.eclipse.compare.structuremergeviewer.IDocumentManager;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.DocumentProviderRegistry;
import org.eclipse.ui.texteditor.IDocumentProvider;

/**
 * No API yet.
 */
public class DocumentManager implements IDocumentManager {
	
	private static final boolean DEBUG= false;
	
	private static ArrayList fgKeys= new ArrayList();
	private static ArrayList fgValues= new ArrayList();
	
	private Map fDocuments = new HashMap();
	
	public static IDocument get(Object o) {
		
		for (int i= 0; i < fgKeys.size(); i++) {
			if (fgKeys.get(i) == o)
				return (IDocument) fgValues.get(i);
		}
		return null;
	}
	
	public static void put(Object o, IDocument document) {
		if (DEBUG) System.out.println("DocumentManager.put: " + document);	//$NON-NLS-1$
		for (int i= 0; i < fgKeys.size(); i++) {
			if (fgKeys.get(i) == o) {
				fgValues.set(i, document);
				return;
			}
		}
		fgKeys.add(o);
		fgValues.add(document);	
	}
	
	public static void remove(IDocument document) {
		if (document != null) {
			if (DEBUG) System.out.println("DocumentManager.remove: " + document);	//$NON-NLS-1$
			for (int i= 0; i < fgValues.size(); i++) {
				if (fgValues.get(i) == document) {
					fgKeys.remove(i);
					fgValues.remove(i);
					return;
				}
			}
			if (DEBUG) System.out.println("DocumentManager.remove: not found");	//$NON-NLS-1$
		}
	}
	
	public static void dump() {
		if (DEBUG) System.out.println("DocumentManager: managed docs:" + fgValues.size());	//$NON-NLS-1$
	}
	
	public void createDocument(Object element, String encoding)
			throws CoreException {
		CoreException exception = null;
		IDocument document = (IDocument)fDocuments.get(element);
		if (document == null) {
			IEditorInput input = getDocumentKey(element);
			if (input != null) {
				document = (IDocument)fDocuments.get(input);
				if (document == null) {
					IDocumentProvider provider = getDocumentProvider(input);
					if (provider != null) {
						try {
							provider.connect(input);
							document = provider.getDocument(input);
							if (document != null) {
								setDocument(input, document);
							}
						} catch (CoreException e) {
							// Log and fall through
							exception = e;
						}
					}
				}
			}
		}
		if (document == null && element instanceof IStreamContentAccessor) {
			IStreamContentAccessor sca= (IStreamContentAccessor) element;			
			String s= null;
			if (encoding == null)
				encoding= getEncoding(element);

			try {
				s= Utilities.readString(sca.getContents(), encoding);
			} catch (CoreException e) {
				exception = e;
			}

			document= new Document(s != null ? s : ""); //$NON-NLS-1$
			setDocument(element, document);
		}
		
		if (document == null) {
			document= new Document(""); //$NON-NLS-1$
			setDocument(element, document);
			
		}
		if (exception != null)
			throw exception;
	}
	
	public IDocument getDocument(Object element) {
		IDocument document = (IDocument)fDocuments.get(element);
		if (document == null) {
			IEditorInput input = getDocumentKey(element);
			if (input != null) {
				document = (IDocument)fDocuments.get(input);
			}
		}
		return document;
	}

	public void setDocument(Object element, IDocument document) {
		fDocuments.put(element, document);
	}
	
	public void dispose() {
		for (Iterator iterator = fDocuments.keySet().iterator(); iterator.hasNext();) {
			Object next = iterator.next();
			if (next instanceof IEditorInput) {
				IEditorInput input = (IEditorInput)next;
				IDocumentProvider provider = getDocumentProvider(input);
				if (provider != null) {
					provider.disconnect(input);
				}
			}
		}
		fDocuments.clear();
	}
	
	private String getEncoding(Object element) {
		if (element instanceof IEncodedStreamContentAccessor) {
			IEncodedStreamContentAccessor sca= (IEncodedStreamContentAccessor) element;
			try {
				String encoding = sca.getCharset();
				if (encoding != null)
					return encoding;
			} catch (CoreException e) {
				CompareUIPlugin.log(e);
			}
		}
		return ResourcesPlugin.getEncoding();
	}
	
	private IEditorInput getDocumentKey(Object element) {
		IEditorInput input = (IEditorInput)Utilities.getAdapter(element, IEditorInput.class);
		if (input != null)
			return input;
		ISharedDocumentAdapter sda = (ISharedDocumentAdapter)Utilities.getAdapter(element, ISharedDocumentAdapter.class, true);
		if (sda != null) {
			return sda.getDocumentKey(element);
		}
		return null;
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.contentmergeviewer.ITextMergeViewerContentProvider#doSave(java.lang.Object, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void doSave(Object element, IProgressMonitor monitor) throws CoreException {
		IEditorInput input = getDocumentKey(element);
		IDocumentProvider provider = getDocumentProvider(input);
		IDocument document = provider.getDocument(input);
		if (document != null) {
			try {
				provider.aboutToChange(input);
				provider.saveDocument(monitor, input, document, false);
			} finally {
				provider.changed(input);
			}
		}
	}
	
	private IDocumentProvider getDocumentProvider(IEditorInput input) {
		return DocumentProviderRegistry.getDefault().getDocumentProvider(input);
	}
	
}
