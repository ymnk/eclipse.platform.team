/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.core;

import java.util.Map;

public interface ISaveContext {
	public String getAttribute(String name);
	
	public void putInteger(String key, int n);
	
	public void putFloat(String key, float n);
	
	public void putString(String key, String n);
	
	public void putBoolean(String key, boolean n);
	
	public int getInteger(String key);
	
	public float getFloat(String key);
	
	public String getString(String key);
	
	public boolean getBoolean(String key);
	
	public String[] getAttributeNames();
	
	public ISaveContext[] getChildren();
	
	public String getName();
	
	public String getValue();
	
	public void setAttributes(Map map);
	
	public void setChildren(ISaveContext[] items);
	
	public void putChild(ISaveContext child);
	
	public void setName(String string);
	
	public void setValue(String string);
	
	public void addAttribute(String key, String value);
	
	public ISaveContext createChild(String name, String value);
}
