package org.eclipse.compare;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.Saveable;
import org.eclipse.ui.progress.IProgressService;
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
 * TODO: Thread safety
 * @since 3.3
 */
public abstract class DocumentNode extends Saveable implements IContentChangeNotifier, IStreamContentAccessor, IDocumentAccessor {

	private ListenerList fListenerList;
	private IDocumentProvider fDocumentProvider;
	private IEditorInput element;
	private boolean connected;
	
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
		IDocument document = getDocument();
		if (document != null)
			return new ByteArrayInputStream(document.get().getBytes());
		// A null document means we are not connected. 
		// Return the contents directly from the source
		return internalGetContents();
	}

	/**
	 * Return the contents directly from the source. This method will be called when 
	 * clients call {@link #getContents()} before {@link #connect()} has been called.
	 * @return the contents directly from the source
	 * @throws CoreException
	 */
	protected abstract InputStream internalGetContents() throws CoreException;

	/**
	 * Set the contents of the source directly. This method is used when {@link #setContent(byte[])}
	 * is called before {@link #connect()}.
	 * @param contents the new contents for the element
	 * @param monitor a progress monitor
	 * @throws CoreException
	 */
	protected abstract void internalSetContents(byte[] contents, IProgressMonitor monitor) throws CoreException;
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.Saveable#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void doSave(IProgressMonitor monitor) throws CoreException {
		IDocument document = getDocument();
		if (document != null) {
			try {
				fDocumentProvider.aboutToChange(getElement());
				fDocumentProvider.saveDocument(monitor, getElement(), document, false);
			} finally {
				fDocumentProvider.changed(getElement());
			}
		}
		// If the document is null, we are not connected and hence have nothing to save
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.Saveable#equals(java.lang.Object)
	 */
	public boolean equals(Object object) {
		if (object == this)
			return true;
		if (object instanceof DocumentNode) {
			DocumentNode other = (DocumentNode) object;
			return other.getElement().equals(getElement());
		}
		return false;
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
	 * @see org.eclipse.compare.IDocumentAccessor#connect()
	 */
	public void connect() throws CoreException {
		if (!isConnected()) {
			getDocumentProvider().connect(getElement());
			connected = true;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.IDocumentAccessor#isConnected()
	 */
	public boolean isConnected() {
		return connected;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.IDocumentAccessor#disconnect()
	 */
	public void disconnect() {
		if (isConnected()) {
			getDocumentProvider().disconnect(getElement());
			connected = false;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.IDocumentAccessor#getDocument()
	 */
	public IDocument getDocument() {
		return getDocumentProvider().getDocument(getElement());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.IEditableContent#setContent(byte[])
	 */
	public void setContent(final byte[] contents) {
		try {
			IRunnableWithProgress runnable = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						IDocument document = getDocument();
						if (document != null) {
							if (contents == null)
								document.set(new String(new byte[0]));
							else 
								document.set(new String(contents));
							doSave(monitor);
						} else {
							internalSetContents(contents, monitor);
						}
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			};
			IProgressService progressService= PlatformUI.getWorkbench().getProgressService();
			progressService.run(false,false, runnable);
		} catch (InvocationTargetException e) {
			Throwable t = e.getTargetException();
			if (t instanceof CoreException) {
				handleException((CoreException) t);
			} else {
				handleException(new CoreException(new Status(IStatus.ERROR, CompareUIPlugin.PLUGIN_ID, 0, "An internal error occurred while saving", e)));
			}
		} catch (InterruptedException e) {
			// Ignore
		}
		fireContentChanged();
	}

	/**
	 * Handle the core exception that occurred.
	 * @param exception the exception
	 */
	protected void handleException(CoreException exception) {
		// TODO: This error should really be shown to the user somehow
		//ErrorDialog.openError(null, null, null, exception.getStatus());
		CompareUIPlugin.log(exception);
	}
}
