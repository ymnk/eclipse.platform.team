package org.eclipse.team.internal.ui.sync.pages;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.actions.TeamAction;
import org.eclipse.team.internal.ui.jobs.JobBusyCursor;
import org.eclipse.team.internal.ui.jobs.RefreshSubscriberInputJob;
import org.eclipse.team.internal.ui.sync.sets.*;
import org.eclipse.team.internal.ui.sync.views.*;
import org.eclipse.team.ui.sync.INewSynchronizeView;
import org.eclipse.team.ui.sync.SubscriberPage;
import org.eclipse.team.ui.sync.actions.*;
import org.eclipse.team.ui.sync.actions.workingsets.WorkingSetFilterActionGroup;
import org.eclipse.ui.*;
import org.eclipse.ui.part.*;
import org.eclipse.ui.views.navigator.ResourceSorter;

public class SubscriberSynchronizeViewPage implements IPageBookViewPage, ISyncSetChangedListener, IPropertyChangeListener {
	// The viewer that is shown in the view. Currently this can be either a table or tree viewer.
	private StructuredViewer viewer;
	
	// Parent composite of this view. It is remembered so that we can dispose of its children when 
	// the viewer type is switched.
	private Composite composite = null;
	private StatisticsPanel statsPanel;
	
	// Viewer type constants
	private int layout;
	
	// Remembering the current input and the previous.
	private SubscriberInput input = null;
	
	// A set of common actions. They are hooked to the active SubscriberInput and must 
	// be reset when the input changes.
	// private SyncViewerActions actions;
	
	private JobBusyCursor busyCursor;
	private INewSynchronizeView view;
	private SubscriberPage page;
	private IPageSite site;
	
	public final static int[] INCOMING_MODE_FILTER = new int[] {SyncInfo.CONFLICTING, SyncInfo.INCOMING};
	public final static int[] OUTGOING_MODE_FILTER = new int[] {SyncInfo.CONFLICTING, SyncInfo.OUTGOING};
	public final static int[] BOTH_MODE_FILTER = new int[] {SyncInfo.CONFLICTING, SyncInfo.INCOMING, SyncInfo.OUTGOING};
	public final static int[] CONFLICTING_MODE_FILTER = new int[] {SyncInfo.CONFLICTING};
	
	// Actions
	private OpenWithActionGroup openWithActions;
	private NavigateAction gotoNext;
	private NavigateAction gotoPrevious;
	private Action toggleLayoutTree;
	private Action toggleLayoutTable;
	private RefactorActionGroup refactorActions;
	private SyncViewerShowPreferencesAction showPreferences;
	private DirectionFilterActionGroup modesGroup;
	private WorkingSetFilterActionGroup workingSetGroup;
	private RefreshAction refreshAction;
	
	/**
	 * Constructs a new SynchronizeView.
	 */
	public SubscriberSynchronizeViewPage(SubscriberPage page, INewSynchronizeView view) {
		this.page = page;
		this.view = view;
		this.input = new SubscriberInput(page.getSubscriber());
		layout = getStore().getInt(IPreferenceIds.SYNCVIEW_VIEW_TYPE);
		if (layout != SubscriberPage.TREE_LAYOUT) {
			layout = SubscriberPage.TABLE_LAYOUT;
		}		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		composite = new Composite(parent, SWT.NONE); 
		GridLayout gridLayout= new GridLayout();
		gridLayout.makeColumnsEqualWidth= false;
		gridLayout.marginWidth= 0;
		gridLayout.marginHeight = 0;
		composite.setLayout(gridLayout);
		
		// Create the busy cursor with no control to start with (createViewer will set it)
		busyCursor = new JobBusyCursor(null /* control */, SubscriberAction.SUBSCRIBER_JOB_TYPE);
		createViewer(composite);
		updateStatusPanel();
		updateTooltip();
		
		// create actions
		openWithActions = new OpenWithActionGroup(view);
		refactorActions = new RefactorActionGroup(view);
		gotoNext = new NavigateAction(view, this, INavigableControl.NEXT);		
		gotoPrevious = new NavigateAction(view, this, INavigableControl.PREVIOUS);
		modesGroup = new DirectionFilterActionGroup(view, page);
		
		toggleLayoutTable = new ToggleViewLayoutAction(page, SubscriberPage.TABLE_LAYOUT);
		toggleLayoutTree = new ToggleViewLayoutAction(page, SubscriberPage.TREE_LAYOUT);
		
		showPreferences = new SyncViewerShowPreferencesAction(view.getSite().getShell());
		workingSetGroup = new WorkingSetFilterActionGroup(getSite().getShell(), this, view, page);
		
		refreshAction = new RefreshAction(getSite().getPage(), input, true /* refresh all */);		
		
		initializeSubscriberInput(input);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IViewPart#init(org.eclipse.ui.IViewSite, org.eclipse.ui.IMemento)
	 */
	public void init(IPageSite site) throws PartInitException {
		this.site = site;
		page.addPropertyChangeListener(this);
		RefreshSubscriberInputJob refreshJob = TeamUIPlugin.getPlugin().getRefreshJob();
		if(getStore().getBoolean(IPreferenceIds.SYNCVIEW_SCHEDULED_SYNC) && refreshJob.getState() == Job.NONE) {
			refreshJob.setReschedule(true);
			// start once the UI has started and stabilized
			refreshJob.schedule(20000 /* 20 seconds */);
		}
	}
	
	/*
	 * This method is synchronized to ensure that all internal state is not corrupted
	 */
	public void initializeSubscriberInput(final SubscriberInput input) {
		// listen to sync set changes in order to update state relating to the
		// size of the sync sets and update the title
		input.registerListeners(this);
		RefreshSubscriberInputJob refreshJob = TeamUIPlugin.getPlugin().getRefreshJob();
		refreshJob.addSubscriberInput(input);
		updateStatusPanel();
		page.setMode(TeamUIPlugin.getPlugin().getPreferenceStore().getInt(IPreferenceIds.SYNCVIEW_SELECTED_MODE));
	}
	
	private void hookContextMenu() {
		if(getViewer() != null) {
			MenuManager menuMgr = new MenuManager("#PopupMenu2"); //$NON-NLS-1$
			menuMgr.setRemoveAllWhenShown(true);
			menuMgr.addMenuListener(new IMenuListener() {
				public void menuAboutToShow(IMenuManager manager) {
					setContextMenu(manager);
				}
			});
			Menu menu = menuMgr.createContextMenu(viewer.getControl());
			viewer.getControl().setMenu(menu);			
			getSite().registerContextMenu("#PopupMenu3", menuMgr, viewer);
		}
	}	

	protected void setContextMenu(IMenuManager manager) {
		openWithActions.fillContextMenu(manager);
		refactorActions.fillContextMenu(manager);
		manager.add(new Separator("SubscriberActionsGroup1")); //$NON-NLS-1$
		manager.add(new Separator("SubscriberActionsGroup2")); //$NON-NLS-1$
		manager.add(new Separator("SubscriberActionsGroup3")); //$NON-NLS-1$
		manager.add(new Separator("SubscriberActionsGroup4")); //$NON-NLS-1$
		manager.add(new Separator("SubscriberActionsGroup5")); //$NON-NLS-1$
		manager.add(new Separator("SubscriberActionsGroup6")); //$NON-NLS-1$
		manager.add(new Separator("SubscriberActionsGroup7")); //$NON-NLS-1$
		manager.add(new Separator("SubscriberActionsGroup8")); //$NON-NLS-1$
		manager.add(new Separator("SubscriberActionsGroup9")); //$NON-NLS-1$		
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	/**
	 * Toggles between label/tree/table viewers. 
	 */
	public void switchViewerType(int viewerType) {
		if(viewer == null || viewerType != layout) {
			if (composite == null || composite.isDisposed()) return;
			IStructuredSelection oldSelection = null;
			if(viewer != null) {
				oldSelection = (IStructuredSelection)viewer.getSelection();
			}
			layout = viewerType;
			getStore().setValue(IPreferenceIds.SYNCVIEW_VIEW_TYPE, layout);
			disposeChildren(composite);
			createViewer(composite);
			composite.layout();
			if(oldSelection == null || oldSelection.size() == 0) {
				//gotoDifference(INavigableControl.NEXT);
			} else {
				viewer.setSelection(oldSelection, true);
			}
			updateStatusPanel();
		}
	}
	
	/**
	 * Adds the listeners to the viewer.
	 */
	protected void initializeListeners() {
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				;
			}
		});
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				handleDoubleClick(event);
			}
		});
		viewer.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				handleOpen(event);
			}
		});
	}	
	
	protected void createViewer(Composite parent) {				
		statsPanel = new StatisticsPanel(parent);
		switch(layout) {
			case SubscriberPage.TREE_LAYOUT:
				createTreeViewerPartControl(parent); 
				break;
			case SubscriberPage.TABLE_LAYOUT:
				createTableViewerPartControl(parent); 
				break;
		}
		viewer.setInput(input);
		viewer.getControl().setFocus();
		initializeListeners();
		hookContextMenu();
		getSite().setSelectionProvider(getViewer());
		busyCursor.setControl(viewer.getControl());
	}
	
	protected void createTreeViewerPartControl(Composite parent) {
		GridData data = new GridData(GridData.FILL_BOTH);
		viewer = new SyncTreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setLabelProvider(SyncViewerLabelProvider.getDecoratingLabelProvider());
		viewer.setSorter(new SyncViewerSorter(ResourceSorter.NAME));
		((TreeViewer)viewer).getTree().setLayoutData(data);
	}
	
	protected void createTableViewerPartControl(Composite parent) {
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
		TableViewer tableViewer = new SyncTableViewer(table);
		
		// Create the table columns
		createColumns(table, layout, tableViewer);
		
		// Set the table contents
		viewer = tableViewer;
		viewer.setContentProvider(new SyncSetTableContentProvider());
		viewer.setLabelProvider(new SyncViewerLabelProvider());
		
		viewer.setSorter(new SyncViewerTableSorter());
	}
	
	/**
	 * Creates the columns for the sync viewer table.
	 */
	protected void createColumns(Table table, TableLayout layout, TableViewer viewer) {
		SelectionListener headerListener = SyncViewerTableSorter.getColumnListener(viewer);
		// revision
		TableColumn col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText("Resource"); //$NON-NLS-1$
		col.addSelectionListener(headerListener);
		layout.addColumnData(new ColumnWeightData(30, true));
		
		// tags
		col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText("In Folder"); //$NON-NLS-1$
		col.addSelectionListener(headerListener);
		layout.addColumnData(new ColumnWeightData(50, true));
	}
	
	protected void disposeChildren(Composite parent) {
		// Null out the control of the busy cursor while we are switching viewers
		busyCursor.setControl(null);
		Control[] children = parent.getChildren();
		for (int i = 0; i < children.length; i++) {
			Control control = children[i];
			control.dispose();
		}
	}
	
	/**
	 * Handles a selection changed event from the viewer. Updates the status line and the action 
	 * bars, and links to editor (if option enabled).
	 * 
	 * @param event the selection event
	 */
	protected void handleSelectionChanged(SelectionChangedEvent event) {
		final IStructuredSelection sel = (IStructuredSelection) event.getSelection();
		updateStatusLine(sel);
	}
	
	protected void handleOpen(OpenEvent event) {
		// actions.open();
	}
	/**
	 * Handles a double-click event from the viewer.
	 * Expands or collapses a folder when double-clicked.
	 * 
	 * @param event the double-click event
	 * @since 2.0
	 */
	protected void handleDoubleClick(DoubleClickEvent event) {
		IStructuredSelection selection = (IStructuredSelection) event.getSelection();
		Object element = selection.getFirstElement();
		
		// Double-clicking should expand/collapse containers
		if (viewer instanceof TreeViewer) {
			TreeViewer tree = (TreeViewer)viewer;
			if (tree.isExpandable(element)) {
				tree.setExpandedState(element, !tree.getExpandedState(element));
			}
		}
		
	}
	
	protected void updateStatusPanel() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				SubscriberInput input = getInput();
				if(statsPanel != null) {	
					statsPanel.update(new ViewStatusInformation(input));
				}
			}					 	
		});
	}
	
	protected void updateTooltip() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				//				 SubscriberInput input = getInput();
				//				 if(input != null) {	
				//					 if(input.getWorkingSet() != null) {
				//						 String tooltip = Policy.bind("LiveSyncView.titleTooltip", input.getWorkingSet().getName()); //$NON-NLS-1$
				//						 setTitleToolTip(tooltip);					 	
				//					 } else {
				//						 setTitleToolTip(""); //$NON-NLS-1$
				//					 }
				//				 }
			}					 	
		});
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IPage#setFocus()
	 */
	public void setFocus() {
		if (viewer == null) return;
		viewer.getControl().setFocus();
	}
	
	public StructuredViewer getViewer() {
		return viewer;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#dispose()
	 */
	public void dispose() {
		// cancel and wait 
		RefreshSubscriberInputJob job = TeamUIPlugin.getPlugin().getRefreshJob();
		job.removeSubscriberInput(input);
		
		// Cleanup the subscriber inputs
		input.deregisterListeners(this);
		input.dispose();
		busyCursor.dispose();
	}
	
	/*
	 * Return the current input for the view.
	 */
	public SubscriberInput getInput() {
		return input;
	}
	
	public void collapseAll() {
		if (viewer == null || !(viewer instanceof AbstractTreeViewer)) return;
		viewer.getControl().setRedraw(false);		
		((AbstractTreeViewer)viewer).collapseToLevel(viewer.getInput(), TreeViewer.ALL_LEVELS);
		viewer.getControl().setRedraw(true);
	}

	/**
	 * This method enables "Show In" support for this view
	 * 
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class key) {
		if (key == IShowInSource.class) {
			return new IShowInSource() {
				public ShowInContext getShowInContext() {
					StructuredViewer v = getViewer();
					if (v == null) return null;
					return new ShowInContext(null, v.getSelection());
				}
			};
		}
		return null;
	}
	
	/**
	 * Updates the message shown in the status line.
	 *
	 * @param selection the current selection
	 */
	protected void updateStatusLine(IStructuredSelection selection) {
		String msg = getStatusLineMessage(selection);
		//getSite().getActionBars().getStatusLineManager().setMessage(msg);
	}
	
	/**
	 * Returns the message to show in the status line.
	 *
	 * @param selection the current selection
	 * @return the status line message
	 * @since 2.0
	 */
	protected String getStatusLineMessage(IStructuredSelection selection) {
		if (selection.size() == 1) {
			IResource resource = getResource(selection.getFirstElement());
			if (resource == null) {
				return Policy.bind("SynchronizeView.12"); //$NON-NLS-1$
			} else {
				return resource.getFullPath().makeRelative().toString();
			}
		}
		if (selection.size() > 1) {
			return selection.size() + Policy.bind("SynchronizeView.13"); //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
	}
	
	/**
	 * @param object
	 * @return
	 */
	private IResource getResource(Object object) {
		return (IResource)TeamAction.getAdapter(object, IResource.class);
	}
	
	/**
	 * Update the title when either the subscriber or filter sync set changes.
	 */
	public void syncSetChanged(SyncSetChangedEvent event) {
		updateStatusPanel();
	}

	public void selectAll() {
		if (getLayout() == SubscriberPage.TABLE_LAYOUT) {
			TableViewer table = (TableViewer)getViewer();
			table.getTable().selectAll();
		} else {
			// Select All in a tree doesn't really work well
		}
	}
	
	private IPreferenceStore getStore() {
		return TeamUIPlugin.getPlugin().getPreferenceStore();
	}
	
	public void workingSetChanged(IWorkingSet set) {
		input.setWorkingSet(set);
		updateTooltip();
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
	public void setActionBars(final IActionBars actionBars) {		
		IToolBarManager manager = actionBars.getToolBarManager();
		manager.add(refreshAction);
		manager.add(gotoNext);
		manager.add(gotoPrevious);
		modesGroup.fillActionBars(actionBars);
		
		// drop down menu
		IMenuManager menu = actionBars.getMenuManager();
		workingSetGroup.fillActionBars(actionBars);
		MenuManager layoutMenu = new MenuManager(Policy.bind("action.layout.label")); //$NON-NLS-1$		
		layoutMenu.add(toggleLayoutTable);
		layoutMenu.add(toggleLayoutTree);
		menu.add(layoutMenu);
		menu.add(new Separator());
		menu.add(showPreferences);
		
		// allow overrides
		page.setActionsBars(actionBars);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IPageBookViewPage#getSite()
	 */
	public IPageSite getSite() {
		return this.site;
	}

	public int getLayout() {
		return layout;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if(event.getProperty().equals(SubscriberPage.P_SYNCVIEWPAGE_LAYOUT)) {
			switchViewerType(((Integer)event.getNewValue()).intValue());
		} else if(event.getProperty().equals(SubscriberPage.P_SYNCVIEWPAGE_MODE)) {
			updateMode(((Integer)event.getNewValue()).intValue());
		} else if(event.getProperty().equals(SubscriberPage.P_SYNCVIEWPAGE_WORKINGSET)) {
			updateWorkingSet((IWorkingSet)event.getNewValue());
		} else if(event.getProperty().equals(WorkingSetFilterActionGroup.CHANGE_WORKING_SET)) {
			Object newValue = event.getNewValue();
			if (newValue instanceof IWorkingSet) {	
				updateWorkingSet((IWorkingSet)newValue);
			} else if (newValue == null) {
				updateWorkingSet(null);
			}
		}
	}

	private void updateWorkingSet(IWorkingSet set) {
		input.setWorkingSet(set);
		updateTooltip();
	}

	private void updateMode(int mode) {
		int[] modeFilter = BOTH_MODE_FILTER;
		switch(mode) {
			case SubscriberPage.INCOMING_MODE:
				modeFilter = INCOMING_MODE_FILTER; break;
			case SubscriberPage.OUTGOING_MODE:
				modeFilter = OUTGOING_MODE_FILTER; break;
			case SubscriberPage.BOTH_MODE:
				modeFilter = BOTH_MODE_FILTER; break;
			case SubscriberPage.CONFLICTING_MODE:
				modeFilter = CONFLICTING_MODE_FILTER; break;
		}
		try {
			input.setFilter(
					new AndSyncInfoFilter(
						new SyncInfoFilter[] {
						   new SyncInfoDirectionFilter(modeFilter), 
						   new SyncInfoChangeTypeFilter(new int[] {SyncInfo.ADDITION, SyncInfo.DELETION, SyncInfo.CHANGE}),
						   new PseudoConflictFilter()
			}), new NullProgressMonitor());
		} catch (TeamException e) {
			Utils.handleError(getSite().getShell(), e, Policy.bind("SynchronizeView.16"), e.getMessage()); //$NON-NLS-1$
		}
	}
}
