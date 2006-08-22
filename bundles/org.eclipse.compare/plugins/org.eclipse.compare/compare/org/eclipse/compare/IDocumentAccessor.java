package org.eclipse.compare;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;

/**
 * An <code>IDocumentAccessor</code> is an object that provides access to a document
 * through which all edits to the bytes of the object are to be performed.
 * 
 * @since 3.3
 */
public interface IDocumentAccessor {
	
	/**
	 * Indicate to this accessor that you would like to access its document.
	 * This method must be called before {@link #getDocument()} is invoked.
	 * @throws CoreException
	 */
	void connect() throws CoreException;
	
	/**
	 * Return whether this accessor is connected to its document provider.
	 * @return whether this accessor is connected to its document provider
	 */
	boolean isConnected();
	
	/**
	 * Indicate to this accessor that you no longer require access to its document.
	 */
	void disconnect();
	
	/**
	 * Return the document associated with this object. This method will return
	 * <code>null</code> if invoked before {@link #connect()} is invoked or after
	 * {@link #disconnect()} is invoked.
	 * @return the document associated with this object
	 */
	IDocument getDocument();

}
