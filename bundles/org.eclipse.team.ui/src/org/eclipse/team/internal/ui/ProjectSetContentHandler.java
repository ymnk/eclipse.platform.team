/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 * IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.team.internal.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ProjectSetContentHandler extends DefaultHandler {
	boolean inPsf = false;
	boolean inProvider = false;
	boolean inProject = false;
	Map map;
	String id;
	List references;
	boolean isVersionOne = false;
	
	/**
	 * @see ContentHandler#startElement(String, String, String, Attributes)
	 */
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
		if (localName.equals("psf")) {
			map = new HashMap();
			inPsf = true;
			String version = atts.getValue("version");
			isVersionOne = version.equals("1.0");
			return;
		}
		if (isVersionOne) return;
		if (localName.equals("provider")) {
			if (!inPsf) throw new SAXException(Policy.bind("ProjectSetContentHandler.Element_provider_must_be_contained_in_element_psf_4")); //$NON-NLS-1$
			inProvider = true;
			id = atts.getValue("id");
			references = new ArrayList();
			return;
		}
		if (localName.equals("project")) {
			if (!inProvider) throw new SAXException(Policy.bind("ProjectSetContentHandler.Element_project_must_be_contained_in_element_provider_7")); //$NON-NLS-1$
			inProject = true;
			String reference = atts.getValue("reference");
			references.add(reference);
			return;
		}
	}

	/**
	 * @see ContentHandler#endElement(String, String, String)
	 */
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
		if (localName.equals("psf")) {
			inPsf = false;
			return;
		}
		if (isVersionOne) return;
		if (localName.equals("provider")) {
			map.put(id, references);
			references = null;
			inProvider = false;
			return;
		}
		if (localName.equals("project")) {
			inProject = false;
			return;
		}
	}
	
	public Map getReferences() {
		return map;
	}
	
	public boolean isVersionOne() {
		return isVersionOne;
	}
}
