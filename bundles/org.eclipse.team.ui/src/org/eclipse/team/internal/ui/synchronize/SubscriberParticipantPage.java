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
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.team.ui.synchronize.subscribers.SubscriberParticipant;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.part.*;

/**
 * A synchronize view page that works with participants that are subclasses of 
 * {@link SubscriberParticipant}. It shows changes in the tree or table view
 * and supports navigation, opening, and filtering changes.
 * <p>
 * Clients can subclass to extend the label decoration or add action bar 
 * contributions. For more extensive modifications, clients should create
 * their own custom page.
 * </p> 
 * @since 3.0
 */
public final class SubscriberParticipantPage extends Page implements ISynchronizePage, IAdaptable {
	
	/** 
	 * Settings constant for section name (value <code>SubscriberParticipantPage</code>).
	 */
	private static final String STORE_SECTION_POSTFIX = "SubscriberParticipantPage"; //$NON-NLS-1$
	/** 
	 * Settings constant for working set (value <code>SubscriberParticipantPage</code>).
	 */
	private static final String STORE_WORKING_SET = "SubscriberParticipantPage.STORE_WORKING_SET"; //$NON-NLS-1$
	/** 
	 * Settings constant for working set (value <code>SubscriberParticipantPage</code>).
	 */
	private static final String STORE_MODE = "SubscriberParticipantPage.STORE_MODE"; //$NON-NLS-1$
	
	private IDialogSettings settings;
	private SubscriberPageConfiguration configuration;
	
	// Parent composite of this view. It is remembered so that we can dispose of its children when 
	// the viewer type is switched.
	private Composite composite;
	private ChangesSection changesSection;
	private Viewer changesViewer;
	private SubscriberParticipant participant;
	
	// Toolbar and status line actions for this page, note that context menu actions shown in 
	// the changes viewer are contributed via the viewer and not the page.
	private StructuredViewerAdvisor viewerAdvisor;
	private ISynchronizePageSite site;
		
	/**
	 * Constructs a new SynchronizeView.
	 */
	public SubscriberParticipantPage(SubscriberPageConfiguration configuration) {
		this.configuration = configuration;
		this.participant = (SubscriberParticipant)configuration.getParticipant();
		IDialogSettings viewsSettings = TeamUIPlugin.getPlugin().getDialogSettings();
		
		String key = Utils.getKey(participant.getId(), participant.getSecondaryId());
		settings = viewsSettings.getSection(key + STORE_SECTION_POSTFIX);
		if (settings == null) {
			settings = viewsSettings.addNewSection(key + STORE_SECTION_POSTFIX);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		composite = new Composite(parent, SWT.NONE); 
		//sc.setContent(composite);
		GridLayout gridLayout= new GridLayout();
		gridLayout.makeColumnsEqualWidth= false;
		gridLayout.marginWidth= 0;
		gridLayout.marginHeight = 0;
		gridLayout.verticalSpacing = 0;
		composite.setLayout(gridLayout);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessVerticalSpace = true;
		composite.setLayoutData(data);
		
		// Create the changes section which, in turn, creates the changes viewer and its configuration
		this.changesSection = new ChangesSection(composite, this, configuration);
		this.changesViewer = createChangesViewer(changesSection.getComposite());
		changesSection.setViewer(changesViewer);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeViewPage#init(org.eclipse.ui.IWorkbenchPart)
	 */
	public void init(ISynchronizePageSite site) {
		this.site = site;
		configuration.setSite(site);
	}
	
	public ISynchronizePageSite getSynchronizePageSite() {
		return site;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IPage#setFocus()
	 */
	public void setFocus() {
		changesSection.setFocus();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#dispose()
	 */
	public void dispose() {
		changesSection.dispose();
		composite.dispose();
		((SynchronizePageActionGroup)configuration).dispose();
	}

	/*
	 * This method enables "Show In" support for this view
	 * 
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class key) {
		if (key.equals(ISelectionProvider.class))
			return changesViewer;
		if (key == IShowInSource.class) {
			return new IShowInSource() {
				public ShowInContext getShowInContext() {					
					StructuredViewer v = (StructuredViewer)changesViewer;
					if (v == null) return null;
					ISelection s = v.getSelection();
					if (s instanceof IStructuredSelection) {
						Object[] resources = Utils.getResources(((IStructuredSelection)s).toArray());
						return new ShowInContext(null, new StructuredSelection(resources));
					}
					return null;
				}
			};
		}
		if (key == IShowInTargetList.class) {
			return new IShowInTargetList() {
				public String[] getShowInTargetIds() {
					return new String[] { IPageLayout.ID_RES_NAV };
				}

			};
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IPage#getControl()
	 */
	public Control getControl() {
		return composite;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IPage#setActionBars(org.eclipse.ui.IActionBars)
	 */
	public void setActionBars(IActionBars actionBars) {
		// Delegate menu creation to the advisor
		viewerAdvisor.setActionBars(actionBars);		
	}
	
	/**
	 * @return Returns the participant.
	 */
	public SubscriberParticipant getParticipant() {
		return participant;
	}
	
	private Viewer createChangesViewer(Composite parent) {
		TreeViewer viewer = new TreeViewerAdvisor.NavigableTreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		GridData data = new GridData(GridData.FILL_BOTH);
		viewer.getControl().setLayoutData(data);
		viewerAdvisor = new TreeViewerAdvisor(configuration);
		viewerAdvisor.initializeViewer(viewer);
		getSynchronizePageSite().setSelectionProvider(viewer);
		return viewer;
	}
	
	public StructuredViewerAdvisor getViewerAdvisor() {
		return viewerAdvisor;
	}
	
	public Viewer getViewer() {
		return changesViewer;
	}
}