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

import org.eclipse.core.runtime.IStatus;

/**
 * A CVS status that provides the results of a message or error line parse
 */
public class CVSParseStatus extends CVSStatus {

    CVSHyperlinkDescriptor[] descriptors;
    
    /**
     * Constructor which creates a status for the descriptors
     * @param descriptors a set of descriptors that can be used to create hyperlinks in
     * a message or error line
     */
    public CVSParseStatus(ICVSFolder commandRoot, String outputLine, CVSHyperlinkDescriptor[] descriptors) {
        this(commandRoot, IStatus.INFO, CVSStatus.HYPERLINK_DESCRIPTION_ONLY, outputLine, descriptors);
    }

    public CVSParseStatus(ICVSFolder commandRoot, int severity, int code, String outputLine, CVSHyperlinkDescriptor[] descriptors) {
        super(severity, code, commandRoot, outputLine);
        this.descriptors = descriptors;
    }

    public CVSHyperlinkDescriptor[] getHyperlinkDescriptors() {
        return descriptors;
    }

}
