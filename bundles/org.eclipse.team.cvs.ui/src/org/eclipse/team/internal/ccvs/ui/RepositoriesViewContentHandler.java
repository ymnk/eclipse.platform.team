/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 * IBM - Initial implementation
 ******************************************************************************/
package org.eclipse.team.internal.ccvs.ui;

import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class RepositoriesViewContentHandler extends DefaultHandler {

	public static final String REPOSITORIES_VIEW_TAG = "repositories-view"; //$NON-NLS-1$

	public static final String REPOSITORY_TAG = "repository"; //$NON-NLS-1$
	public static final String MODULE_TAG = "module"; //$NON-NLS-1$
	public static final String TAG_TAG = "tag"; //$NON-NLS-1$
	public static final String AUTO_REFRESH_FILE_TAG = "auto-refresh-file"; //$NON-NLS-1$
	
	public static final String ID_ATTRIBUTE = "id"; //$NON-NLS-1$
	public static final String NAME_ATTRIBUTE = "name"; //$NON-NLS-1$
	public static final String PATH_ATTRIBUTE = "path"; //$NON-NLS-1$
	public static final String TYPE_ATTRIBUTE = "type"; //$NON-NLS-1$
	public static final String REPOSITORY_PROGRAM_NAME_ATTRIBUTE = "program-name"; //$NON-NLS-1$
	
	public static final String[] TAG_TYPES = {"head", "branch", "version", "date"};
	
	StringBuffer buffer = new StringBuffer();
	Stack tagStack = new Stack();

	/**
	 * @see ContentHandler#characters(char[], int, int)
	 */
	public void characters(char[] chars, int startIndex, int length) throws SAXException {
		buffer.append(chars, startIndex, length);
	}

	/**
	 * @see ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void endElement(String namespaceURI, String localName, String qName)
		throws SAXException {
		if (localName.equals(REPOSITORIES_VIEW_TAG)) {
			// all done
		} else if (localName.equals(REPOSITORY_TAG)) {
			// finished with this repository
		} else if (localName.equals(TAG_TAG)) {
			// finished with this tag
		}
		if (!localName.equals(tagStack.peek())) {
			throw new SAXException(Policy.bind("RepositoriesViewContentHandler.unmatchedTag", localName)); //$NON-NLS-1$
		}
		tagStack.pop();
	}
		
	/**
	 * @see ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(
			String namespaceURI,
			String localName,
			String qName,
			Attributes atts)
			throws SAXException {
			
		if (localName.equals(REPOSITORIES_VIEW_TAG)) {
			// just started
		} else if (localName.equals(REPOSITORY_TAG)) {
			String id = atts.getValue(ID_ATTRIBUTE);
			if (id == null) {
				throw new SAXException(Policy.bind("RepositoriesViewContentHandler.missingAttribute", REPOSITORY_TAG, ID_ATTRIBUTE));
			}
			String name = atts.getValue(NAME_ATTRIBUTE);
			if (name != null) {
			}
		} else if (localName.equals(TAG_TAG)) {
			String type = atts.getValue(TYPE_ATTRIBUTE);
			if (type != null) {
			}
			String name = atts.getValue(NAME_ATTRIBUTE);
			if (name != null) {
			}
		}
		// empty buffer
		buffer = new StringBuffer();
		tagStack.push(localName);
	}
	
}
