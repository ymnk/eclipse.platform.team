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
package org.eclipse.team.internal.ui.synchronize.actions;

import java.util.*;

import org.eclipse.jface.action.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.SyncInfoSetCompareConfiguration;
import org.eclipse.team.ui.synchronize.content.ILogicalView;
import org.eclipse.ui.actions.ActionGroup;

/**
 * Action group that populates a context menu with the available logical views.
 */
public class LogicalViewActionGroup extends ActionGroup {

	public static final String SELECTED_VIEW = "selected-view";
	
	Action noLogicalView;
	List actions = new ArrayList();
	ILogicalView selectedView;
	IPropertyChangeListener listener;
	
	public LogicalViewActionGroup() {
		makeActions();
	}

	private void makeActions() {
		noLogicalView = new Action() {
			public void run() {
				selectView(null);
			}
		};
		Utils.initAction(noLogicalView, "action.noLogicalView.", Policy.getBundle());
		ILogicalView[] views = SyncInfoSetCompareConfiguration.getLogicalViews();
		for (int i = 0; i < views.length; i++) {
			ILogicalView view = views[i];
			actions.add(new LogicalViewAction(view, this));
		}		
	}

	public void fillContextMenu(IMenuManager parentMenu) {
		MenuManager menu =
			new MenuManager("Logical View"); //$NON-NLS-1$
		menu.add(noLogicalView);
		menu.add(new Separator());
		boolean hasSelection = false;
		for (Iterator iter = actions.iterator(); iter.hasNext();) {
			LogicalViewAction element = (LogicalViewAction) iter.next();
			menu.add(element);
			boolean selected = isSelected(element.getView().getId());
			hasSelection |= selected;
			element.setChecked(selected);
		}
		noLogicalView.setChecked(!hasSelection);
		parentMenu.add(menu);
	}

	private boolean isSelected(String id) {
		if (selectedView == null || id == null) return false;
		return selectedView.getId().equals(id);
	}

	
	/* package */ void selectView(ILogicalView view) {
		ILogicalView oldView = selectedView;
		selectedView = view;
		firePropertyChangeEvent(SELECTED_VIEW, oldView, view);
	}

	public void setPropertyChangeListener(IPropertyChangeListener listener) {
		this.listener = listener;
	}

	private void firePropertyChangeEvent(String property, Object oldValue, Object newValue) {
		if (listener == null)
			return;
		listener.propertyChange(new PropertyChangeEvent(this, property, oldValue, newValue));
	}

	public void setSelectedView(ILogicalView view) {
		selectedView = view;
	}
}
