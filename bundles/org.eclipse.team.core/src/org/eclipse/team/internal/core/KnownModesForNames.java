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

package org.eclipse.team.internal.core;

import org.eclipse.team.core.IFileTypeInfo;

/**
 * 
 */
public class KnownModesForNames {
    
    private static class FileTypeInfo implements IFileTypeInfo {
        
        private final String fName;
        private final int fMode;
        
        public FileTypeInfo(String name, int mode) {
            fName= name;
            fMode= mode;
        }
        
        public FileTypeInfo(String name, Integer mode) {
            this(name, mode.intValue());
        }
        
        public String getExtension() {
            return fName;
        }
        
        public int getType() {
            return fMode;
        }
    }
    static final String PREF_TEAM_TYPES_FOR_FILES= "cvs_mode_for_file_without_extensions"; //$NON-NLS-1$
    
    private static final UserStringMappings fCachedModes= new UserStringMappings(PREF_TEAM_TYPES_FOR_FILES);
    
    private KnownModesForNames() {
    }
}
