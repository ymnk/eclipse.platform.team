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

import org.eclipse.jface.action.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.jobs.*;
import org.eclipse.team.internal.ui.widgets.ControlFactory;
import org.eclipse.team.internal.ui.widgets.FormSection;
import org.eclipse.team.ui.controls.IControlFactory;
import org.eclipse.team.ui.controls.IHyperlinkListener;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class TeamSubscriberParticipantComposite extends Composite implements IPropertyChangeListener, ITeamResourceChangeListener {
	private TeamSubscriberParticipant participant;	
	private Color background;
	private ISynchronizeView view;
	private IControlFactory factory;
	
	private Label lastSyncLabel;
	private Label scheduleLabel;
	private TableViewer rootsList;
	
	private IRefreshSubscriberListener refreshSubscriberListener = new IRefreshSubscriberListener() {
		public void refreshStarted(IRefreshEvent event) {
		}
		public void refreshDone(final IRefreshEvent event) {
			if (event.getParticipant() == participant) {
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						synchronized (this) {
							if (lastSyncLabel != null && !lastSyncLabel.isDisposed()) {
								StringBuffer text = new StringBuffer();
								text.append(RefreshSchedule.refreshEventAsString(event));
								SyncInfo[] changes = event.getChanges();
								if (changes.length != 0) {
									text.append(" (" + Integer.toString(changes.length) + " changes found)");
								} else {
									text.append(" (No changes found)");
								}
								lastSyncLabel.setText(text.toString());
							}
						}
					}
				});
			}
		}
	};
	
	private boolean shortStyle;
	
	public TeamSubscriberParticipantComposite(Composite parent, boolean shortStyle, IControlFactory factory, TeamSubscriberParticipant participant, ISynchronizeView view) {
		super(parent, SWT.NONE);
		this.shortStyle = shortStyle;
		this.factory = factory;
		this.participant = participant;		
		this.view = view;
		createComposite(this);		
		participant.addPropertyChangeListener(this);		
		RefreshSubscriberJob.addRefreshListener(refreshSubscriberListener);
	}
	
	protected Composite createComposite(final Composite area) {
		GridLayout layout = new GridLayout();
		//layout.marginHeight = 0;
		//layout.marginWidth = 0;
		area.setLayout(layout);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 75;
		area.setLayoutData(data);
		setBackground(factory.getBackgroundColor());					
		{
			final Composite composite_1 = factory.createComposite(area, SWT.NONE);
			GridData gridData = new GridData(GridData.VERTICAL_ALIGN_FILL);
			final GridLayout gridLayout_1 = new GridLayout();
			gridLayout_1.numColumns = 3;
			gridLayout_1.marginHeight = 0;
			gridLayout_1.marginWidth = 0;
			composite_1.setLayout(gridLayout_1);
			composite_1.setLayoutData(gridData);
			{
				final Label label = factory.createLabel(composite_1, participant.getInput().getSubscriber().getDescription(), SWT.WRAP);
				gridData = new GridData();
				gridData.horizontalSpan = 3;
				label.setLayoutData(gridData);
			}			
			{
				final Label label = factory.createLabel(composite_1, "Last Refresh:");
				gridData = new GridData();
				label.setLayoutData(gridData);
			}
			{
				lastSyncLabel = factory.createLabel(composite_1, Policy.bind("SyncViewPreferencePage.lastRefreshRunNever")); //$NON-NLS-1$);
				gridData = new GridData(GridData.FILL_HORIZONTAL);
				gridData.grabExcessHorizontalSpace = true;
				gridData.horizontalSpan = 2;
				lastSyncLabel.setLayoutData(gridData);
			}
			{
				factory.createLabel(composite_1, "Refresh Schedule:");
			}
			{
				scheduleLabel = factory.createLabel(composite_1, "");
				gridData = new GridData(GridData.FILL_HORIZONTAL);
				gridData.grabExcessHorizontalSpace = true;
				scheduleLabel.setLayoutData(gridData);
			}			
			{
				Button config = factory.createButton(composite_1, "More...", SWT.FLAT);
				gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
				config.setLayoutData(gridData);
				config.addSelectionListener(new SelectionListener() {
					public void widgetSelected(SelectionEvent e) {
						ConfigureRefreshScheduleDialog d = new ConfigureRefreshScheduleDialog(
								getShell(), participant.getRefreshSchedule());
						d.setBlockOnOpen(false);
						d.open();
					}
					public void widgetDefaultSelected(SelectionEvent e) {
					}
				});
			}			
		}
		
		createSynchronizeResourcesComposite(area);
		updateScheduleLabel();
		return area;
	}
		
	private Composite createSynchronizeResourcesComposite(Composite area) {
		{
			final Composite composite_1 = factory.createComposite(area, SWT.NONE);
			GridData gridData = new GridData(GridData.FILL_BOTH);
			final GridLayout gridLayout_1 = new GridLayout();
			gridLayout_1.numColumns = 1;
			gridLayout_1.marginHeight = 3;
			gridLayout_1.marginWidth = 3;
			composite_1.setLayout(gridLayout_1);
			composite_1.setLayoutData(gridData);
			{
				final Label label = factory.createLabel(composite_1, "Synchronized Folders:");
				rootsList = new TableViewer(composite_1, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
				gridData = new GridData(GridData.FILL_BOTH);
				gridData.heightHint = 80;
				rootsList.getTable().setLayoutData(gridData);
				rootsList.setLabelProvider(new WorkbenchLabelProvider());
				rootsList.setContentProvider(new ArrayContentProvider());
				rootsList.setInput(participant.getInput().subscriberRoots());
				factory.paintBordersFor(composite_1);
				hookContextMenu();
				participant.getInput().getSubscriber().addListener(this);
			}
			return composite_1;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	public void dispose() {
		super.dispose();
		participant.removePropertyChangeListener(this);
		participant.getInput().getSubscriber().removeListener(this);
		RefreshSubscriberJob.removeRefreshListener(refreshSubscriberListener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if(! isDisposed()) {
			String property = event.getProperty();
			if(property.equals(TeamSubscriberParticipant.P_SYNCVIEWPAGE_SCHEDULE)) {
				updateScheduleLabel();
			}
			layout(true);
		}
	}
	
	private void updateScheduleLabel() {
		scheduleLabel.setText(participant.getRefreshSchedule().getScheduleAsString());
	}
	
	protected void hookContextMenu() {
		if(rootsList != null) {
			final MenuManager menuMgr = new MenuManager(); //$NON-NLS-1$
			menuMgr.setRemoveAllWhenShown(true);
			menuMgr.addMenuListener(new IMenuListener() {
				public void menuAboutToShow(IMenuManager manager) {
					setContextMenu(manager);
				}
			});
			Menu menu = menuMgr.createContextMenu(rootsList.getControl());			
			rootsList.getControl().setMenu(menu);			
		}
	}
	
	protected void setContextMenu(IMenuManager manager) {	
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.ITeamResourceChangeListener#teamResourceChanged(org.eclipse.team.core.subscribers.TeamDelta[])
	 */
	public void teamResourceChanged(TeamDelta[] deltas) {
		for (int i = 0; i < deltas.length; i++) {
			final TeamDelta delta = deltas[i];
			if(delta.getFlags() == TeamDelta.PROVIDER_CONFIGURED) {
				TeamUIPlugin.getStandardDisplay().asyncExec(new Runnable() {
					public void run() {
						rootsList.add(delta.getResource());
					}
				});
			} else if(delta.getFlags() == TeamDelta.PROVIDER_DECONFIGURED) {
				TeamUIPlugin.getStandardDisplay().asyncExec(new Runnable() {
					public void run() {						
						rootsList.remove(delta.getResource());					
					}
				});
			}
		}
	}
}