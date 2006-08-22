package org.eclipse.compare;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.Saveable;
import org.eclipse.ui.texteditor.DocumentProviderRegistry;
import org.eclipse.ui.texteditor.IDocumentProvider;

/**
 * Abstract implementation for an <code>IDocumentAccessor</code> that makes use of the 
 * {@link DocumentProviderRegistry} to obtain a shared document for an object.
 * 
 * TODO: Do we need this to be a stream content accessor? Probably since client viewers may not know about the document
 * TODO: Do we need to be a content change notifier? If so, should we just forward all changes from the document? If not,
 * we need to make sure that clients do not rely on this notification to work properly.
 * TODO: We need to make sure the document is created lazily.
 * TODO: We also need to make sure we disconnect when we are done.
 * @since 3.3
 */
public abstract class DocumentNode extends Saveable implements IContentChangeNotifier, IStreamContentAccessor, IDocumentAccessor {

	private ListenerList fListenerList;
	private IDocumentProvider fDocumentProvider;
	private IEditorInput element;
	
	/**
	 * Create a file buffer node on the given editor input. An editor input is provided because
	 * the {@link DocumentProviderRegistry} is keyed by this.
	 * @param input an editor input
	 */
	public DocumentNode(IEditorInput input) {
		element = input;
		fDocumentProvider = DocumentProviderRegistry.getDefault().getDocumentProvider(element);
	}
	
	/**
	 * Return the element that the buffer is associated with.
	 * TODO: We need to enumerate what the valid element types are.
	 * @return the element that the buffer is associated with
	 */
	protected Object getElement() {
		return element;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.IContentChangeNotifier#addContentChangeListener(org.eclipse.compare.IContentChangeListener)
	 */
	public void addContentChangeListener(IContentChangeListener listener) {
		if (fListenerList == null)
			fListenerList= new ListenerList();
		fListenerList.add(listener);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.IContentChangeNotifier#removeContentChangeListener(org.eclipse.compare.IContentChangeListener)
	 */
	public void removeContentChangeListener(IContentChangeListener listener) {
		if (fListenerList != null) {
			fListenerList.remove(listener);
			if (fListenerList.isEmpty())
				fListenerList= null;
		}
	}
	
	/**
	 * Notifies all registered <code>IContentChangeListener</code>s of a content change.
	 */
	protected void fireContentChanged() {
		if (fListenerList != null) {
			Object[] listeners= fListenerList.getListeners();
			for (int i= 0; i < listeners.length; i++)
				((IContentChangeListener)listeners[i]).contentChanged(this);
		}
	}
	
	/**
	 * Return the document provider for this node.
	 * @return the document provider for this node
	 */
	public IDocumentProvider getDocumentProvider() {
		return fDocumentProvider;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.IStreamContentAccessor#getContents()
	 */
	public InputStream getContents() throws CoreException {
		IDocument document = getDocumentProvider().getDocument(getElement());
		if (document == null) {
			// We have not been initialized yet
			getDocumentProvider().connect(getElement());
			document = getDocumentProvider().getDocument(getElement());
		}
		return new ByteArrayInputStream(document.get().getBytes());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.Saveable#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void doSave(IProgressMonitor monitor) throws CoreException {
		fDocumentProvider.saveDocument(monitor, getElement(), fDocumentProvider.getDocument(getElement()), false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.Saveable#equals(java.lang.Object)
	 */
	public boolean equals(Object object) {
		return object == this;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.Saveable#hashCode()
	 */
	public int hashCode() {
		return getElement().hashCode();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.Saveable#isDirty()
	 */
	public boolean isDirty() {
		return fDocumentProvider.canSaveDocument(getElement());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.IDocumentAccessor#getDocument()
	 */
	public IDocument getDocument() {
		return getDocumentProvider().getDocument(getElement());
	}

}
