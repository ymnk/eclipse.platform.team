/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core;

import org.eclipse.core.resources.IResource;

/**
 * A hyperlink descriptor provides information that can be used to
 * create hyperlinks in the message and error lines that have been 
 * returned from the server during a CVS command.
 */
public class CVSHyperlinkDescriptor {

    public static final String LOCAL_RESOURCE_PATH = "local_path"; //$NON-NLS-1$
    
    private final ICVSFolder commandRoot;
    private final String line;

    private final String type;
    private final String value;

    private final int offset;
    
    public CVSHyperlinkDescriptor(ICVSFolder commandRoot, String line, String type, String value) {
        this.commandRoot = commandRoot;
        this.line = line;
        this.type = type;
        this.value = value;
        this.offset = line.indexOf(value);
    }
    
    public IResource getLocalResource() {
        if (type == LOCAL_RESOURCE_PATH) {
	        String localPath = value;
	        if (localPath != null) {
		        try {
		            ICVSResource cvsResource = commandRoot.getChild(localPath);
		            return cvsResource.getIResource();
		        } catch (CVSException e) {
		            // The child could not be obtained so just return null
		        }
	        }
        }
        return null;
    }

    /**
     * Return the offset of the link text in the line
     * @return the offset of the link text in the line
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Return the length of the link text
     * @return the length of the link text
     */
    public int getLength() {
        return value.length();
    }

}
