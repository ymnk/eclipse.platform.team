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
package org.eclipse.team.ui.synchronize;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;

/**
 * A synchronize scope whose roots are defined by a working set.
 * <p>
 * Clients are not expected to subclass this class.
 * @since 3.0
 */
public class WorkingSetScope extends AbstractSynchronizeScope implements IPropertyChangeListener {
	
	/*
	 * Constants used to save and restore this scope
	 */
	private final static String CTX_SET_NAME = "workingset_scope_name"; //$NON-NLS-1$
	
	private IWorkingSet set;
	
	/**
	 * Create the scope for the subscriber and working set
	 * @param subscriber the subscriber that defines this scope
	 * @param set the working set that defines this scope
	 */
	public WorkingSetScope(IWorkingSet set) {
		this.set = set;
		PlatformUI.getWorkbench().getWorkingSetManager().addPropertyChangeListener(this);
	}
	
	/** 
	 * Create this scope from it's previously saved state
	 * @param memento
	 */
	protected WorkingSetScope(IMemento memento) {
		super(memento);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.ScopableSubscriberParticipant.SubscriberScope#getName()
	 */
	public String getName() {
		if (set == null) {
			return "Workspace";
		}
		return set.getName();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.ScopableSubscriberParticipant.SubscriberScope#getRoots()
	 */
	public IResource[] getRoots() {
		if (set == null) {
			return null;
		}
		return Utils.getResources(set.getElements());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty() == IWorkingSetManager.CHANGE_WORKING_SET_CONTENT_CHANGE) {
			IWorkingSet newSet = (IWorkingSet)event.getNewValue();
			if (newSet == set) {
				fireRootsChanges();
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.ScopableSubscriberParticipant.SubscriberScope#dispose()
	 */
	public void dispose() {
		super.dispose();
		PlatformUI.getWorkbench().getWorkingSetManager().removePropertyChangeListener(this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.AbstractSynchronizeScope#saveState(org.eclipse.ui.IMemento)
	 */
	public void saveState(IMemento memento) {
		super.saveState(memento);
		if (set != null) {
			memento.putString(CTX_SET_NAME, set.getName());
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.AbstractSynchronizeScope#init(org.eclipse.ui.IMemento)
	 */
	protected void init(IMemento memento) {
		super.init(memento);
		String name = memento.getString(CTX_SET_NAME);
		if (name != null) {
			set = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(name);
		}
	}
}