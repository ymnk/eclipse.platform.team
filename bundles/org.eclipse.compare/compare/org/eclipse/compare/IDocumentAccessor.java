package org.eclipse.compare;

import org.eclipse.jface.text.IDocument;

/**
 * An <code>IDocumentAccessor</code> is an object that provides access to a document
 * through which all edits to the bytes of the object are to be performed.
 * 
 * @since 3.3
 */
public interface IDocumentAccessor {
	
	/**
	 * Return the document associated with this object.
	 * @return the document associated with this object
	 */
	IDocument getDocument();

}
