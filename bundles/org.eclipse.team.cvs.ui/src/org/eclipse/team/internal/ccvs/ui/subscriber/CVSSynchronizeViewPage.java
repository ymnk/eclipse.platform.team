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
package org.eclipse.team.internal.ccvs.ui.subscriber;

import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.internal.ccvs.ui.CVSLightweightDecorator;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.IActionDelegate;

public class CVSSynchronizeViewPage extends TeamSubscriberParticipantPage implements ISyncSetChangedListener {

	private static class CVSLabelProvider extends TeamSubscriberParticipantLabelProvider {
		private ILabelProvider oldLabelProvider;
		public CVSLabelProvider(ILabelProvider oldLabelProvider) {
			this.oldLabelProvider = oldLabelProvider;
		}
		protected String decorateText(String input, Object element) {
			String text = input;
			if (element instanceof SyncInfoDiffNode) {
				IResource resource =  ((SyncInfoDiffNode)element).getResource();
				if(resource != null) {
					CVSLightweightDecorator.Decoration decoration = new CVSLightweightDecorator.Decoration();
					CVSLightweightDecorator.decorateTextLabel((IResource) resource, decoration, false, true);
					StringBuffer output = new StringBuffer(25);
					if(decoration.prefix != null) {
						output.append(decoration.prefix);
					}
					output.append(text);
					if(decoration.suffix != null) {
						output.append(decoration.suffix);
					}
					return output.toString();
				}
			}
			return text;
		}
	}
	
	private List delegates = new ArrayList(2);

	protected class CVSActionDelegate extends Action {
		private IActionDelegate delegate;

		public CVSActionDelegate(IActionDelegate delegate) {
			this.delegate = delegate;
			addDelegate(this);
		}

		public void run() {
			IStructuredContentProvider cp = (IStructuredContentProvider) ((StructuredViewer)getChangesViewer()).getContentProvider();
			StructuredSelection selection = new StructuredSelection(cp.getElements(getSyncInfoSet()));
			if (!selection.isEmpty()) {
				delegate.selectionChanged(this, selection);
				delegate.run(this);
			}
		}

		public IActionDelegate getDelegate() {
			return delegate;
		}
	}

	public CVSSynchronizeViewPage(TeamSubscriberParticipant participant, ISynchronizeView view) {
		super(participant, view);
	}	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.ui.sync.AbstractSynchronizeParticipant#dispose()
	 */
	public void dispose() {
		super.dispose();
		getSyncInfoSet().removeSyncSetChangedListener(this);
		CVSUIPlugin.removePropertyChangeListener(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.internal.ui.sync.sets.ISyncSetChangedListener#syncSetChanged(org.eclipse.team.internal.ui.sync.sets.SyncSetChangedEvent)
	 */
	public void syncSetChanged(ISyncInfoSetChangeEvent event) {
		StructuredViewer viewer = (StructuredViewer)getChangesViewer();
		if (viewer != null && getSyncInfoSet() != null) {
			IStructuredContentProvider cp = (IStructuredContentProvider) viewer.getContentProvider();
			StructuredSelection selection = new StructuredSelection(cp.getElements(getSyncInfoSet()));
			for (Iterator it = delegates.iterator(); it.hasNext(); ) {
				CVSActionDelegate delegate = (CVSActionDelegate) it.next();
				delegate.getDelegate().selectionChanged(delegate, selection);
			}
		}
	}

	private void addDelegate(CVSActionDelegate delagate) {
		delegates.add(delagate);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {		
		super.propertyChange(event);
		String prop = event.getProperty();
		if(prop.equals(CVSUIPlugin.P_DECORATORS_CHANGED) && getChangesViewer() != null && getSyncInfoSet() != null) {
			((StructuredViewer)getChangesViewer()).refresh(true /* update labels */);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		
		// Sync changes are used to update the action state for the update/commit buttons.
		getSyncInfoSet().addSyncSetChangedListener(this);
		
		// Listen for decorator changed to refresh the viewer's labels.
		CVSUIPlugin.addPropertyChangeListener(this);
		
		// Add a CVS specific label decorator to show CVS information in the sync
		// view. We aren't using the global adaptable decorators because we don't
		// want all outgoing/repository icons in this view. Instead, we add 
		// CVS specific information that is useful in the synchronizing context.
		StructuredViewer viewer = (StructuredViewer)getChangesViewer();
		ILabelProvider oldLabelProvider = (ILabelProvider)viewer.getLabelProvider();
		viewer.setLabelProvider(new CVSLabelProvider(oldLabelProvider));		
	}
	
	private ISyncInfoSet getSyncInfoSet() {
		return getParticipant().getSyncInfoSet();
	}
}