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

import java.util.*;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.team.core.IFileTypeInfo;
import org.eclipse.team.core.Team;

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
    
    private static class CachedModesForNames implements Preferences.IPropertyChangeListener {
        
        private static final String PREF_TEAM_TYPES_FOR_FILES= "cvs_mode_for_file_without_extensions";
        private static final String PREF_TEAM_SEPARATOR = "\n"; //$NON-NLS-1$
        
        private Map fMap;
        private final Preferences fPreferences;
        
        public CachedModesForNames() {
            fPreferences= TeamPlugin.getPlugin().getPluginPreferences();
            fPreferences.addPropertyChangeListener(this);
        }
        
        public Map referenceMap() {
            if (fMap == null) {
                fMap= new HashMap();
                if (!fPreferences.contains(PREF_TEAM_TYPES_FOR_FILES)) 
                    return Collections.EMPTY_MAP;
                
                final String prefTypes = fPreferences.getString(PREF_TEAM_TYPES_FOR_FILES);
                final StringTokenizer tok = new StringTokenizer(prefTypes, PREF_TEAM_SEPARATOR);
                try {
                    while (true) {
                        final String name = tok.nextToken();
                        if (name.length()==0) 
                            return Collections.EMPTY_MAP;
                        final String mode= tok.nextToken();
                        fMap.put(name, Integer.valueOf(mode));
                    } 
                } catch (NoSuchElementException e) {
                    return Collections.EMPTY_MAP;
                }

            }
            return fMap;
        }

        public void propertyChange(PropertyChangeEvent event) {
            if(event.getProperty().equals(PREF_TEAM_TYPES_FOR_FILES))
                fMap= null;
        }
        
        public void save() {
            // Now set into preferences
            final StringBuffer buffer = new StringBuffer();
            final Iterator e = fMap.keySet().iterator();
            
            while (e.hasNext()) {
                final String filename = (String)e.next();
                buffer.append(filename);
                buffer.append(PREF_TEAM_SEPARATOR);
                final Integer type = (Integer)fMap.get(filename);
                buffer.append(type);
                buffer.append(PREF_TEAM_SEPARATOR);
            }
            TeamPlugin.getPlugin().getPluginPreferences().setValue(PREF_TEAM_TYPES_FOR_FILES, buffer.toString());
        }
    }        

    private static final CachedModesForNames fCachedModes= new CachedModesForNames();
    
    private KnownModesForNames() {
    }
        
    public static IFileTypeInfo [] getKnownModesForNames() {
        return toFileTypeInfoArray(fCachedModes.referenceMap());
    }
    
    public static void addModesForFiles(String [] names, int [] modes) {
        final Map map= fCachedModes.referenceMap();
        map.putAll(toMap(names, modes));
        fCachedModes.save();
    }
    
    public static void setModesforFiles(String [] names, int [] modes) {
        final Map map= fCachedModes.referenceMap();
        map.putAll(toMap(names, modes));
        fCachedModes.save();
    }
    
    private static Map toMap(String [] filenames, int [] modes) {
        final SortedMap map= new TreeMap();
        for (int i = 0; i < filenames.length; i++) {
            map.put(filenames[i], new Integer(modes[i]));
        }
        return map;
    }
    
    private static IFileTypeInfo [] toFileTypeInfoArray(Map map) {
        
        final IFileTypeInfo [] result= new IFileTypeInfo[map.size()];
        int index= 0;
        for (Iterator iter = map.keySet().iterator(); iter.hasNext();) {
            final String name = (String) iter.next();
            result[index++]= new FileTypeInfo(name, (Integer)map.get(name));
        }
        return result;
    }

    /**
     * @param name
     * @return
     */
    public static int getType(String name) {
        final Integer mode= (Integer)fCachedModes.referenceMap().get(name);
        return mode != null ? mode.intValue() : Team.UNKNOWN;
    }
}
