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
package org.eclipse.team.ui.synchronize;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.team.internal.ui.synchronize.SyncInfoDiffTreeNavigator;
import org.eclipse.team.ui.synchronize.actions.INavigableControl;

public class SyncInfoDiffTreeViewer extends TreeViewer implements INavigableControl, SyncInfoDiffTreeNavigator.INavigationTarget {

	private SyncInfoSetCompareConfiguration configuration;
	
	public SyncInfoDiffTreeViewer(Composite parent, SyncInfoSetCompareConfiguration configuration) {
		super(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		this.configuration = configuration;
		configuration.initializeViewer(parent, this);
	}

	protected void handleLabelProviderChanged(LabelProviderChangedEvent event) {
		Object[] changed = configuration.asModelObjects(this, event.getElements());
		if (changed != null) {
			if (changed.length == 0) {
				return;
			}
			event= new LabelProviderChangedEvent((IBaseLabelProvider) event.getSource(), changed);
		}
		super.handleLabelProviderChanged(event);
	}

	/**
	 * Cleanup listeners and call super for content provider and label provider disposal.
	 */	
	protected void handleDispose(DisposeEvent event) {
		super.handleDispose(event);
		configuration.dispose();
	}

	protected void inputChanged(Object in, Object oldInput) {
		super.inputChanged(in, oldInput);		
		if (in != oldInput) {
			configuration.getNavigator().navigate(false, true);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.actions.INavigableControl#gotoDifference(int)
	 */
	public boolean gotoDifference(int direction) {
		boolean next = direction == INavigableControl.NEXT ? true : false;
		return configuration.getNavigator().navigate(next, false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncInfoDiffTreeNavigator.INavigationTarget#openSelection()
	 */
	public void openSelection() {
		fireOpen(new OpenEvent(this, getSelection()));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncInfoDiffTreeNavigator.INavigationTarget#createChildren(org.eclipse.swt.widgets.TreeItem)
	 */
	public void createChildren(TreeItem item) {
		super.createChildren(item);
	}

}
