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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.team.core.subscribers.ITeamResourceChangeListener;
import org.eclipse.team.core.subscribers.TeamDelta;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.IStorageDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IElementStateListener;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.quickdiff.IQuickDiffProviderImplementation;

/**
 * The CVS quick diff provider that returns the latest remote revision for a file
 * that is managed by the CVS provider.
 * 
 * 
 * 
 * @since 3.0
 */
public class RemoteRevisionQuickDiffProvider implements IQuickDiffProviderImplementation {

	private boolean fDocumentRead = false;
	private ITextEditor fEditor = null;
	private IDocument fReference = null;
	private IDocumentProvider documentProvider = null;
	private IEditorInput input = null;
	private String fId;
	private ICVSFile cvsFile;

	protected Job updateJob;
	
	/**
	 * Updates the document if a sync changes occurs to the associated CVS file.
	 */
	private ITeamResourceChangeListener teamChangeListener = new ITeamResourceChangeListener() {
		public void teamResourceChanged(TeamDelta[] deltas) {
			if(cvsFile != null) {
				for (int i = 0; i < deltas.length; i++) {
					TeamDelta delta = deltas[i];
					try {
						if(delta.getResource().equals(cvsFile.getIResource())) {
							if(delta.getFlags() == TeamDelta.SYNC_CHANGED) {
								fetchContentsInJob();
							}
						}
					} catch (CVSException e) {
						e.printStackTrace();
					} 
				}
			}
		}
	};

	/**
	 * Updates the document if the document is changed (e.g. replace with)
	 */
	private IElementStateListener documentListener = new IElementStateListener() {
		public void elementDirtyStateChanged(Object element, boolean isDirty) {
		}

		public void elementContentAboutToBeReplaced(Object element) {
		}

		public void elementContentReplaced(Object element) {
			if(element == input) {
				fetchContentsInJob();
			}
		}

		public void elementDeleted(Object element) {
		}

		public void elementMoved(Object originalElement, Object movedElement) {
		}
	};
	
	/*
	 * @see org.eclipse.test.quickdiff.DocumentLineDiffer.IQuickDiffReferenceProvider#getReference()
	 */
	public IDocument getReference(IProgressMonitor monitor) {
		try {
			if (!fDocumentRead)
				readDocument(monitor);
			if (fDocumentRead)
				return fReference;
			else
				return null;
		} catch(CoreException e) {
			CVSUIPlugin.log(e);
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.quickdiff.IQuickDiffProviderImplementation#setActiveEditor(org.eclipse.ui.texteditor.ITextEditor)
	 */
	public void setActiveEditor(ITextEditor targetEditor) {
		if (targetEditor != fEditor) {
			dispose();
			fEditor= targetEditor;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.quickdiff.IQuickDiffProviderImplementation#isEnabled()
	 */
	public boolean isEnabled() {
		if (!initialized())
			return false;
		return getCVSFile() != null;
	}

	/*
	 * @see org.eclipse.jface.text.source.diff.DocumentLineDiffer.IQuickDiffReferenceProvider#dispose()
	 */
	public void dispose() {
		fReference= null;
		cvsFile = null;
		fDocumentRead= false;
		if(documentProvider != null) {
			documentProvider.removeElementStateListener(documentListener);
		}
		CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().removeListener(teamChangeListener);
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
	
	private boolean initialized() {
		return fEditor != null;
	}
	
	private void readDocument(IProgressMonitor monitor) throws CoreException {
		if (!initialized())
			return;

		fDocumentRead= false;
	
		if (fReference == null) {
			fReference= new Document();
		}
	
		cvsFile = getCVSFile();
		if(cvsFile != null) {
			ICVSRemoteResource remote = CVSWorkspaceRoot.getRemoteTree(cvsFile.getIResource(), cvsFile.getSyncInfo().getTag(), monitor);
			IDocumentProvider docProvider= fEditor.getDocumentProvider();
			if (docProvider instanceof IStorageDocumentProvider) {
				documentProvider = docProvider;
				IStorageDocumentProvider provider= (IStorageDocumentProvider) documentProvider;			
				String encoding= provider.getEncoding(fEditor.getEditorInput());
				if (encoding == null) {
					encoding= provider.getDefaultEncoding();
				}
				InputStream stream= remote.getContents(monitor);
				if (stream == null) {
					return;
				}
				setDocumentContent(fReference, stream, encoding);
				
				CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().addListener(teamChangeListener);
				((IDocumentProvider)provider).addElementStateListener(documentListener);
			}
		}
		fDocumentRead= true;
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
			throw new CVSException(Policy.bind("RemoteRevisionQuickDiffProvider.readingFile"), x);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException x) {
					throw new CVSException(Policy.bind("RemoteRevisionQuickDiffProvider.closingFile"), x);
				}
			}
		}
	}
	
	private ICVSFile getCVSFile() {
		IEditorInput input= fEditor.getEditorInput();
		if (input instanceof IFileEditorInput) {
			IFile file= ((IFileEditorInput)input).getFile();
			try {
				if(CVSWorkspaceRoot.isSharedWithCVS(file)) {
					return CVSWorkspaceRoot.getCVSFileFor(file);
				}
			} catch (CVSException e) {
				CVSUIPlugin.log(e);
			}
		}
		return null;
	}

	private void fetchContentsInJob() {
		if(updateJob != null && updateJob.getState() != Job.NONE) {
			updateJob.cancel();
			try {
				updateJob.join();
			} catch (InterruptedException e) {				
			}
		}
		Job updateJob = new Job(Policy.bind("RemoteRevisionQuickDiffProvider.fetchingFile")) {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					readDocument(monitor);
				} catch (CoreException e) {
					return e.getStatus();
				}
				return Status.OK_STATUS;
			}
		};
		updateJob.schedule();
	}
}
