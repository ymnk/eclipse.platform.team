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
package org.eclipse.team.internal.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Something (as a mark of visible sign) left by a material thing
 * formely present but now lost or unknown.
 */
public class VestigeConfigurationItem {
	
	/*
	 * The name of this configuration element
	 */
	private String name;
	
	/*
	 * The value of this configuration element
	 */
	private String value;
	
	/*
	 * The attributes of this element: {String:attribute name -> String:attr value}
	 */
	private Map attributes;
	
	/*
	 * The child configuration items
	 */
	private VestigeConfigurationItem[] children;
	
	public VestigeConfigurationItem() {}
	
	/**
	 * Returns the named attribute of this configuration element, or
	 * <code>null</code> if none. 
	 * <p>
	 * The names of configuration element attributes
	 * are the same as the attribute names of the corresponding XML element.
	 * For example, the configuration markup 
	 * <pre>
	 * &lt;bg pattern="stripes"/&gt;
	 * </pre>
	 * corresponds to a configuration element named <code>"bg"</code>
	 * with an attribute named <code>"pattern"</code>
	 * with attribute value <code>"stripes"</code>.
	 * </p>
	 * <p> Note that any translation specified in the plug-in manifest
	 * file is automatically applied.
	 * </p>
	 *
	 * @see IPluginDescriptor#getResourceString 
	 *
	 * @param name the name of the attribute
	 * @return attribute value, or <code>null</code> if none
	 */
	public String getAttribute(String name) {
		if(attributes == null) {
			return null;
		}
		return (String)attributes.get(name);
	}
	
	/**
	 * Returns the names of the attributes of this configuration element.
	 * Returns an empty array if this configuration element has no attributes.
	 * <p>
	 * The names of configuration element attributes
	 * are the same as the attribute names of the corresponding XML element.
	 * For example, the configuration markup 
	 * <pre>
	 * &lt;bg color="blue" pattern="stripes"/&gt;
	 * </pre>
	 * corresponds to a configuration element named <code>"bg"</code>
	 * with attributes named <code>"color"</code>
	 * and <code>"pattern"</code>.
	 * </p>
	 *
	 * @return the names of the attributes 
	 */
	public String[] getAttributeNames() {
		if(attributes == null) {
			return new String[0];
		}
		return (String[])attributes.keySet().toArray(new String[attributes.keySet().size()]);
	}
	
	/**
	 * Returns all configuration elements that are children of this
	 * configuration element. 
	 * Returns an empty array if this configuration element has no children.
	 * <p>
	 * Each child corresponds to a nested
	 * XML element in the configuration markup.
	 * For example, the configuration markup 
	 * <pre>
	 * &lt;view&gt;
	 * &nbsp&nbsp&nbsp&nbsp&lt;verticalHint&gt;top&lt;/verticalHint&gt;
	 * &nbsp&nbsp&nbsp&nbsp&lt;horizontalHint&gt;left&lt;/horizontalHint&gt;
	 * &lt;/view&gt;
	 * </pre>
	 * corresponds to a configuration element, named <code>"view"</code>,
	 * with two children.
	 * </p>
	 *
	 * @return the child configuration elements
	 */
	public VestigeConfigurationItem[] getChildren() {
		return children;
	}
	
	/**
	 * Returns the name of this configuration element. 
	 * The name of a configuration element is the same as
	 * the XML tag of the corresponding XML element. 
	 * For example, the configuration markup 
	 * <pre>
	 * &lt;wizard name="Create Project"/&gt; 
	 * </pre>
	 * corresponds to a configuration element named <code>"wizard"</code>.
	 *
	 * @return the name of this configuration element
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns the text value of this configuration element.
	 * For example, the configuration markup 
	 * <pre>
	 * &lt;script lang="javascript"&gt;.\scripts\cp.js&lt;/script&gt;
	 * </pre>
	 * corresponds to a configuration element <code>"script"</code>
	 * with value <code>".\scripts\cp.js"</code>.
	 * <p> Values may span multiple lines (i.e., contain carriage returns
	 * and/or line feeds).
	 * <p> Note that any translation specified in the plug-in manifest
	 * file is automatically applied.
	 * </p>
	 *
	 * @see IPluginDescriptor#getResourceString 
	 *
	 * @return the text value of this configuration element or <code>null</code>
	 */
	public String getValue() {
		return value;
	}
	
	public void setAttributes(Map map) {
		attributes = map;
	}

	public void setChildren(VestigeConfigurationItem[] items) {
		children = items;
	}

	public void setName(String string) {
		name = string;
	}

	public void setValue(String string) {
		value = string;
	}

	public void addAttribute(String key, String value) {
		if(attributes == null) {
			attributes = new HashMap();
		}
		attributes.put(key, value);
	}
}