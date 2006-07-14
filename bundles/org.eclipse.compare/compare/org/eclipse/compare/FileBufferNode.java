package org.eclipse.compare;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.ui.Saveable;
import org.eclipse.ui.texteditor.IDocumentProvider;

/**
 * Abstract implementation for an <code>ITypedElement</code> that represents a text file
 * that shares a buffer with other editors.
 * 
 * @since 3.3
 */
public abstract class FileBufferNode extends Saveable implements IContentChangeNotifier, IStreamContentAccessor {

	private ListenerList fListenerList;
	private IDocumentProvider fDocumentProvider;
	
	/**
	 * Create a file buffer node on the given document provider.
	 * @param provider a document provider
	 */
	public FileBufferNode(IDocumentProvider provider) {
		fDocumentProvider = provider;
	}
	
	/**
	 * Return the element that the buffer is associated with.
	 * TODO: We need to enumerate what the valid element types are.
	 * @return the element that the buffer is associated with
	 */
	protected abstract Object getElement();

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
		return new ByteArrayInputStream(getDocumentProvider().getDocument(getElement()).get().getBytes());
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

}
