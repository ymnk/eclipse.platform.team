/*************.******************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.sync.views;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.sync.ComparisonCriteria;
import org.eclipse.team.core.sync.SyncTreeSubscriber;
import org.eclipse.team.core.sync.TeamProvider;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.sync.actions.SyncViewerActions;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.part.ViewPart;

public class SyncViewer extends ViewPart {
	
	/*
	 * The viewer thst is shown in the view. Currently this can be
	 * either a table or tree viewer.
	 */
	private StructuredViewer viewer;
	
	/*
	 * Parent composite of this view. It is remembered so that we can
	 * dispose of its children when the viewer type is switched.
	 */
	private Composite composite = null;
	private IMemento memento;
	
	/*
	 * viewer type constants
	 */ 
	public static final int TREE_VIEW = 0;
	public static final int TABLE_VIEW = 1;
	
	/*
	 * Array of SubscriberInput objects. There is one of these for each subscriber
	 * registered with the sync view. 
	 */
	private List subscriberInputs = new ArrayList(1);
	private SubscriberInput input = null;
	
	/*
	 * A set of common actions. They are hooked to the active SubscriberInput and
	 * must be reset when the input changes.
	 */
	private SyncViewerActions actions;
			
	public SyncViewer() {
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		initializeActions();
		initializeSyncSet();
		createViewer(parent, TABLE_VIEW);
		contributeToActionBars();
		this.composite = parent;
	}

	public void switchViewerType(int viewerType) {
		if (composite == null || composite.isDisposed()) return;
		disposeChildren(composite);
		createViewer(composite, viewerType);
		composite.layout();
	}
	
	private void createViewer(Composite parent, int viewerType) {
		switch(viewerType) {
			case TREE_VIEW:
				createTreeViewerPartControl(parent); 
				break;
			case TABLE_VIEW:
				createTableViewerPartControl(parent); 
				break;
		}
		hookContextMenu();
		hookOpen();
	}

	protected ILabelProvider getLabelProvider() {
		return new DecoratingLabelProvider(
					new SyncViewerLabelProvider(),
					WorkbenchPlugin
						.getDefault()
						.getWorkbench()
						.getDecoratorManager()
						.getLabelDecorator());
	}

	private void createTreeViewerPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new SyncSetTreeContentProvider());
		viewer.setLabelProvider(getLabelProvider());
		viewer.setSorter(new SyncViewerSorter());
		//viewer.setInput(filteredInput.getSyncSet());
	}
	
	private void createTableViewerPartControl(Composite parent) {
		// Create the table
		Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		GridData data = new GridData(GridData.FILL_BOTH);
		table.setLayoutData(data);
		
		// Set the table layout
		TableLayout layout = new TableLayout();
		table.setLayout(layout);
		
		// Create the viewer
		TableViewer tableViewer = new TableViewer(table);
		
		// Create the table columns
		createColumns(table, layout, tableViewer);
		
		// Set the table contents
		viewer = tableViewer;
		viewer.setContentProvider(new SyncSetTableContentProvider());
		viewer.setLabelProvider(new SyncViewerLabelProvider());
		viewer.setSorter(new SyncViewerSorter());
		//viewer.setInput(filteredInput.getSyncSet());
	}
	
	/**
	 * Creates the columns for the sync viewer table.
	 */
	private void createColumns(Table table, TableLayout layout, TableViewer viewer) {
		//SelectionListener headerListener = getColumnListener(viewer);
		// revision
		TableColumn col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText("Resource"); //$NON-NLS-1$
		//col.addSelectionListener(headerListener);
		layout.addColumnData(new ColumnWeightData(30, true));
	
		// tags
		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText("Parent"); //$NON-NLS-1$
		//col.addSelectionListener(headerListener);
		layout.addColumnData(new ColumnWeightData(50, true));
	}
	
	private void disposeChildren(Composite parent) {
		Control[] children = parent.getChildren();
		for (int i = 0; i < children.length; i++) {
			Control control = children[i];
			control.dispose();
		}
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				actions.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		actions.fillActionBars(bars);
	}

	private void initializeActions() {
		actions = new SyncViewerActions(this);
		actions.restore(memento);
	}

	private void initializeSubscriberInput(SubscriberInput input) {
		run(new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					actions.
					input.prepareInput(monitor);
				} catch (TeamException e) {
					throw new InvocationTargetException(e);
				}
			}
		});
	}
	
	private void resetFilters() {
		run(new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					resetFilters(monitor);
				} catch (TeamException e) {
					throw new InvocationTargetException(e);
				}
			}
		});
	}

	private void resetFilters(IProgressMonitor monitor) throws TeamException {
		filteredInput.setFilter(new AndSyncSetFilter(
			new SyncSetFilter[] {
				new SyncSetDirectionFilter(getDirectionFilters()), 
				new SyncSetChangeFilter(getChangeFilters())
			}), 
			monitor);
	}
	
	private int[] getDirectionFilters() {
		return actions.getDirectionFilters();
	}

	private int[] getChangeFilters() {
		return actions.getChangeFilters();
	}
	
		
	private void hookOpen() {
		viewer.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				actions.open();
			}
		});
	}
	
	private void showMessage(String message) {
		MessageDialog.openInformation(
			viewer.getControl().getShell(),
			"Sample View",
			message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		// TODO: Broken on startup. Probably due to use of workbench progress
		if (viewer == null) return;
		viewer.getControl().setFocus();
	}

	public StructuredViewer getViewer() {
		return viewer;
	}
	
	private static void handle(Shell shell, Exception exception, String title, String message) {
		IStatus status = null;
		boolean log = false;
		boolean dialog = false;
		if (exception instanceof TeamException) {
			status = ((TeamException)exception).getStatus();
			log = false;
			dialog = true;
		} else if (exception instanceof InvocationTargetException) {
			Throwable t = ((InvocationTargetException)exception).getTargetException();
			if (t instanceof TeamException) {
				status = ((TeamException)t).getStatus();
				log = false;
				dialog = true;
			} else if (t instanceof CoreException) {
				status = ((CoreException)t).getStatus();
				log = true;
				dialog = true;
			} else if (t instanceof InterruptedException) {
				return;
			} else {
				status = new Status(IStatus.ERROR, TeamUIPlugin.ID, 1, "internal error", t); //$NON-NLS-1$
				log = true;
				dialog = true;
			}
		}
		if (status == null) return;
		if (!status.isOK()) {
			IStatus toShow = status;
			if (status.isMultiStatus()) {
				IStatus[] children = status.getChildren();
				if (children.length == 1) {
					toShow = children[0];
				}
			}
			if (title == null) {
				title = status.getMessage();
			}
			if (message == null) {
				message = status.getMessage();
			}
			if (dialog) {
				ErrorDialog.openError(shell, title, message, toShow);
			}
			if (log) {
				TeamUIPlugin.log(toShow);
			}
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#dispose()
	 */
	public void dispose() {
		super.dispose();
		subscriberInput.disconnect();
		filteredInput.disconnect();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#setTitle(java.lang.String)
	 */
	public void setTitle(String title) {
		super.setTitle(getSubscriber().getName() + ": " + title);
	}

	/**
	 * The sync mode has changed. Update as appropriate.
	 * @param mode
	 */
	public void updateFilters() {
		resetFilters();
	}

	public SyncTreeSubscriber getSubscriber() {
		if (subscriber == null) {
			// TODO: hack for taking the first subscriber only. This will have to change
			// to allow more than one subscriber.
			this.subscriber = TeamProvider.getSubscriber();
		}
		return subscriber;
	}
	/**
	 * 
	 */
	public ComparisonCriteria[] getAllComparisonCritera() {
		return getSubscriber().getComparisonCriterias();
	}

	/**
	 * @param criteria
	 */
	public void activateComparisonCriteria(ComparisonCriteria criteria) {
		try {
			subscriber.setCurrentComparisonCriteria(criteria.getId());
			resetSubscriberInput();
		} catch (TeamException e) {
			handle(getSite().getShell(), e, null, null);
		}
	}
	
	public void resetSubscriberInput() {
		run(new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					resetSubscriberInput(monitor);
				} catch (TeamException e) {
					throw new InvocationTargetException(e);
				}
			}
		});
	}
	
	public void refreshViewerInput(IProgressMonitor monitor) {
		try {
			resetSubscriberInput(monitor);
		} catch (TeamException e) {
		}
		// TODO: Should not be needed once proper eventing exists
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				viewer.getControl().setRedraw(false);
				viewer.refresh();
				viewer.getControl().setRedraw(true);
			}
		});				
	}

	public void run(IRunnableWithProgress runnable) {
		try {
			getRunnableContext().run(true, true, runnable);
		} catch (InvocationTargetException e) {
			handle(getSite().getShell(), e, null, null);
		} catch (InterruptedException e) {
			// Nothing to be done
		}
	}
	
	private void resetSubscriberInput(IProgressMonitor monitor) throws TeamException {
		subscriberInput.reset(monitor);
	}
	
	/**
	 * Returns the runnableContext.
	 * @return IRunnableContext
	 */
	public IRunnableContext getRunnableContext() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IViewPart#init(org.eclipse.ui.IViewSite, org.eclipse.ui.IMemento)
	 */
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		this.memento = memento;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IViewPart#saveState(org.eclipse.ui.IMemento)
	 */
	public void saveState(IMemento memento) {
		super.saveState(memento);
		actions.save(memento);
	}
}