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
package org.eclipse.team.internal.ccvs.ui;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.team.core.sync.IRemoteResource;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.quickdiff.IQuickDiffProviderImplementation;
import org.eclipse.ui.editors.text.StorageDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Quick and dirty cvs reference provider. No background, no hourglass cursor, no nothing.
 * @since 3.0
 */
public class RemoteRevisionQuickDiffProvider implements IQuickDiffProviderImplementation {

	private boolean fDocumentRead= false;
	private ITextEditor fEditor= null;
	private IDocument fReference= null;
	private String fId;

	/*
	 * @see org.eclipse.test.quickdiff.DocumentLineDiffer.IQuickDiffReferenceProvider#getReference()
	 */
	public IDocument getReference() {
		if (!fDocumentRead)
			readDocument();
		if (fDocumentRead)
			return fReference;
		else
			return null;
	}

	/**
	 * Intitializes the given document with the given stream using the given encoding.
	 *
	 * @param document the document to be initialized
	 * @param contentStream the stream which delivers the document content
	 * @param encoding the character encoding for reading the given stream
	 * @exception CoreException if the given stream can not be read
	 */
	private static void setDocumentContent(IDocument document, InputStream contentStream, String encoding) throws CoreException {
		Reader in= null;
		try {
			final int DEFAULT_FILE_SIZE= 15 * 1024;

			in= new BufferedReader(new InputStreamReader(contentStream, encoding), DEFAULT_FILE_SIZE);
			CharArrayWriter caw= new CharArrayWriter(DEFAULT_FILE_SIZE);
			char[] readBuffer= new char[2048];
			int n= in.read(readBuffer);
			while (n > 0) {
				caw.write(readBuffer, 0, n);
				n= in.read(readBuffer);
			}
			document.set(caw.toString());
		} catch (IOException x) {
			String msg= x.getMessage() == null ? "" : x.getMessage(); //$NON-NLS-1$
			IStatus s= new Status(IStatus.ERROR, PlatformUI.PLUGIN_ID, IStatus.OK, msg, x);
			throw new CoreException(s);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException x) {
				}
			}
		}
	}

	private void readDocument() {
		// TODO this is quick&dirty
		if (!initialized())
			return;
		fDocumentRead= false;
		IEditorInput input= fEditor.getEditorInput();
		if (fReference == null)
			fReference= new Document();
		if (input instanceof IFileEditorInput) {
			IFile file= ((IFileEditorInput)input).getFile();
			IRemoteResource remote= null;
			try {
				remote= CVSWorkspaceRoot.getRemoteResourceFor(file);
			} catch (CVSException e) {
				return;
			}
			if (remote == null)
				return;

			IDocumentProvider provider= fEditor.getDocumentProvider();
			if (provider instanceof StorageDocumentProvider) {
				StorageDocumentProvider sProvider= (StorageDocumentProvider)provider;
				String encoding= sProvider.getEncoding(input);
				if (encoding == null)
					encoding= sProvider.getDefaultEncoding();
				try {
					InputStream stream= remote.getContents(new NullProgressMonitor());
					if (stream == null)
						return;
					setDocumentContent(fReference, stream, encoding);
				} catch (CoreException e1) {
					// TODO Auto-generated catch block
					MessageDialog.openError(fEditor.getSite().getShell(), "CoreException", "Error when retrieving remote version");
					return;
				}
			}
		}
		fDocumentRead= true;
	}

	private boolean initialized() {
		return fEditor != null;
	}

	public void setActiveEditor(ITextEditor targetEditor) {
		if (targetEditor != fEditor) {
			dispose();
			fEditor= targetEditor;
		}
	}

	/*
	 * @see org.eclipse.quickdiff.QuickDiffTestPlugin.IQuickDiffProviderImplementation#isEnabled()
	 */
	public boolean isEnabled() {
		if (!initialized())
			return false;
		IEditorInput input= fEditor.getEditorInput();
		if (input instanceof IFileEditorInput) {
			IFile file= ((IFileEditorInput)input).getFile();
			try {
				return CVSWorkspaceRoot.isSharedWithCVS(file);
			} catch (CVSException e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}
		return false;
	}

	/*
	 * @see org.eclipse.jface.text.source.diff.DocumentLineDiffer.IQuickDiffReferenceProvider#dispose()
	 */
	public void dispose() {
		fReference= null;
		fDocumentRead= false;
	}

	/*
	 * @see org.eclipse.quickdiff.QuickDiffTestPlugin.IQuickDiffProviderImplementation#setId(java.lang.String)
	 */
	public void setId(String id) {
		fId= id;
	}

	/*
	 * @see org.eclipse.jface.text.source.diff.DocumentLineDiffer.IQuickDiffReferenceProvider#getId()
	 */
	public String getId() {
		return fId;
	}
}
