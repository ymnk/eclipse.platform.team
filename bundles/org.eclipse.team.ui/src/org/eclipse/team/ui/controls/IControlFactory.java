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
package org.eclipse.team.ui.controls;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;

public interface IControlFactory {
	public abstract Button createButton(Composite parent, String text, int style);
	public abstract Composite createComposite(Composite parent);
	public abstract Composite createComposite(Composite parent, int style);
	public abstract Composite createCompositeSeparator(Composite parent);
	public abstract Label createHeadingLabel(Composite parent, String text);
	public abstract Label createHeadingLabel(Composite parent, String text, int style);
	public abstract Label createHeadingLabel(Composite parent, String text, Color bg);
	public abstract Label createHeadingLabel(Composite parent, String text, Color bg, int style);
	public abstract Label createLabel(Composite parent, String text);
	public abstract Label createLabel(Composite parent, String text, int style);
	public abstract Label createSeparator(Composite parent, int style);
	public abstract Table createTable(Composite parent, int style);
	public abstract Text createText(Composite parent, String value);
	public abstract Text createText(Composite parent, String value, int style);
	public abstract Tree createTree(Composite parent, int style);
	public abstract Color getBackgroundColor();
	public abstract Color getBorderColor();
	public abstract Cursor getBusyCursor();
	public abstract Color getColor(String key);
	public abstract Color getForegroundColor();
	public abstract void hookDeleteListener(Control control);
	public abstract Color registerColor(String key, int r, int g, int b);
	public abstract void setBackgroundColor(Color color);
	public abstract void setHyperlinkColor(Color color);
	public abstract void setHyperlinkHoverColor(org.eclipse.swt.graphics.Color hoverColor);
	public abstract void setHyperlinkUnderlineMode(int newHyperlinkUnderlineMode);
	public abstract void turnIntoHyperlink(Control control, IHyperlinkListener listener);
}