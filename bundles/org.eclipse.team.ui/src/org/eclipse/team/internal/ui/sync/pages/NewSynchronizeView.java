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
package org.eclipse.team.internal.ui.sync.pages;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IBasicPropertyConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.sync.INewSynchronizeView;
import org.eclipse.team.ui.sync.ISynchronizeManager;
import org.eclipse.team.ui.sync.ISynchronizePageListener;
import org.eclipse.team.ui.sync.ISynchronizeViewPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.MessagePage;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.PageBookView;

public class NewSynchronizeView extends PageBookView implements INewSynchronizeView, ISynchronizePageListener, IPropertyChangeListener {
	
	/**
	 * The console being displayed, or <code>null</code> if none
	 */
	private ISynchronizeViewPage fActivePage = null;
	
	/**
	 * Map of consoles to dummy console parts (used to close pages)
	 */
	private Map fPageToPart;
	
	/**
	 * Map of parts to consoles
	 */
	private Map fPartToPage;
	
	// actions
	private SynchronizePageDropDownAction fPageDropDown;
	
	private boolean isAvailable() {
		return getPageBook() != null && !getPageBook().isDisposed();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		Object source = event.getSource();
		if (source instanceof ISynchronizeViewPage && event.getProperty().equals(IBasicPropertyConstants.P_TEXT)) {
			if (source.equals(getActivePage())) {
				updateTitle();
			}
		}

	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener#partClosed(org.eclipse.ui.IWorkbenchPart)
	 */
	public void partClosed(IWorkbenchPart part) {
		super.partClosed(part);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.ui.console.IConsoleView#getConsole()
	 */
	public ISynchronizeViewPage getActivePage() {
		return fActivePage;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.PageBookView#showPageRec(org.eclipse.ui.part.PageBookView.PageRec)
	 */
	protected void showPageRec(PageRec pageRec) {
		super.showPageRec(pageRec);
		fActivePage = (ISynchronizeViewPage)fPartToPage.get(pageRec.part);
		updateTitle();		
	}

	/**
	 * Updates the view title based on the active console
	 */
	protected void updateTitle() {
		ISynchronizeViewPage page = getActivePage();
		if (page == null) {
			setTitle("Synchronize View");
		} else {
			setTitle("Synchronize View - " + page.getName());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.PageBookView#doDestroyPage(org.eclipse.ui.IWorkbenchPart, org.eclipse.ui.part.PageBookView.PageRec)
	 */
	protected void doDestroyPage(IWorkbenchPart part, PageRec pageRecord) {
		IPage page = pageRecord.page;
		page.dispose();
		pageRecord.dispose();
		
		ISynchronizeViewPage console = (ISynchronizeViewPage)fPartToPage.get(part);
		console.removePropertyChangeListener(this);
				
		// empty cross-reference cache
		fPartToPage.remove(part);
		fPageToPart.remove(console);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.PageBookView#doCreatePage(org.eclipse.ui.IWorkbenchPart)
	 */
	protected PageRec doCreatePage(IWorkbenchPart dummyPart) {
		SynchronizeViewWorkbenchPart part = (SynchronizeViewWorkbenchPart)dummyPart;
		ISynchronizeViewPage console = part.getConsole();
		IPageBookViewPage page = console.createPage(this);
		initPage(page);
		page.createControl(getPageBook());
		console.addPropertyChangeListener(this);
		PageRec rec = new PageRec(dummyPart, page);
		return rec;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.PageBookView#isImportant(org.eclipse.ui.IWorkbenchPart)
	 */
	protected boolean isImportant(IWorkbenchPart part) {
		return part instanceof SynchronizeViewWorkbenchPart;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#dispose()
	 */
	public void dispose() {
		super.dispose();
		TeamUI.getSynchronizeManager().removeSynchronizePageListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.PageBookView#createDefaultPage(org.eclipse.ui.part.PageBook)
	 */
	protected IPage createDefaultPage(PageBook book) {
		MessagePage page = new MessagePage();
		page.createControl(getPageBook());
		initPage(page);
		return page;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.IConsoleListener#consolesAdded(org.eclipse.ui.console.IConsole[])
	 */
	public void consolesAdded(final ISynchronizeViewPage[] consoles) {
		if (isAvailable()) {
			Runnable r = new Runnable() {
				public void run() {
					for (int i = 0; i < consoles.length; i++) {
						if (isAvailable()) {
							ISynchronizeViewPage console = consoles[i];
							SynchronizeViewWorkbenchPart part = new SynchronizeViewWorkbenchPart(console, getSite());
							fPageToPart.put(console, part);
							fPartToPage.put(part, console);
							partActivated(part);
						}
					}
				}
			};
			asyncExec(r);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.IConsoleListener#consolesRemoved(org.eclipse.ui.console.IConsole[])
	 */
	public void consolesRemoved(final ISynchronizeViewPage[] consoles) {
		if (isAvailable()) {
			Runnable r = new Runnable() {
				public void run() {
					for (int i = 0; i < consoles.length; i++) {
						if (isAvailable()) {
							ISynchronizeViewPage console = consoles[i];
							SynchronizeViewWorkbenchPart part = (SynchronizeViewWorkbenchPart)fPageToPart.get(console);
							if (part != null) {
								partClosed(part);
							}
							if (getActivePage() == null) {
								ISynchronizeViewPage[] available = TeamUI.getSynchronizeManager().getSynchronizePages();
								if (available.length > 0) {
									display(available[available.length - 1]);
								}
							}
						}
					}
				}
			};
			asyncExec(r);
		}
	}

	/**
	 * Constructs a console view
	 */
	public NewSynchronizeView() {
		super();
		fPageToPart = new HashMap();
		fPartToPage = new HashMap();
	}
	
	/**
	 * Creates a pop-up menu on the given control. The menu
	 * is registered with this view's site, such that other
	 * plug-ins may contribute to the menu.
	 * 
	 * @param menuControl the control with which the pop-up
	 *  menu will be associated with.
	 */
	protected void createContextMenu(Control menuControl) {
		MenuManager menuMgr= new MenuManager("#PopUp"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		Menu menu= menuMgr.createContextMenu(menuControl);
		menuControl.setMenu(menu);

		// register the context menu such that other plugins may contribute to it
		if (getSite() != null) {
			getSite().registerContextMenu(menuMgr, null);
		}
	}

	protected void createActions() {
		//fPinAction = new PinConsoleAction(this);
		fPageDropDown = new SynchronizePageDropDownAction(this);
	}

	protected void configureToolBar(IToolBarManager mgr) {
//		mgr.add(new Separator(IConsoleConstants.LAUNCH_GROUP));
//		mgr.add(new Separator(IConsoleConstants.OUTPUT_GROUP));
//		mgr.add(fPinAction);
		mgr.add(fPageDropDown);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.IConsoleView#display(org.eclipse.ui.console.IConsole)
	 */
	public void display(ISynchronizeViewPage console) {
		SynchronizeViewWorkbenchPart part = (SynchronizeViewWorkbenchPart)fPageToPart.get(console);
		if (part != null) {
			partActivated(part);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.PageBookView#getBootstrapPart()
	 */
	protected IWorkbenchPart getBootstrapPart() {
		return null;
	}
	
	/**
	 * Registers the given runnable with the display
	 * associated with this view's control, if any.
	 * 
	 * @see org.eclipse.swt.widgets.Display#asyncExec(java.lang.Runnable)
	 */
	public void asyncExec(Runnable r) {
		if (isAvailable()) {
			getPageBook().getDisplay().asyncExec(r);
		}
	}
	
	/**
	 * Creates this view's underlying viewer and actions.
	 * Hooks a pop-up menu to the underlying viewer's control,
	 * as well as a key listener. When the delete key is pressed,
	 * the <code>REMOVE_ACTION</code> is invoked. Hooks help to
	 * this view. Subclasses must implement the following methods
	 * which are called in the following order when a view is
	 * created:<ul>
	 * <li><code>createViewer(Composite)</code> - the context
	 *   menu is hooked to the viewer's control.</li>
	 * <li><code>createActions()</code></li>
	 * <li><code>configureToolBar(IToolBarManager)</code></li>
	 * <li><code>getHelpContextId()</code></li>
	 * </ul>
	 * @see IWorkbenchPart#createPartControl(Composite)
	 */
	public void createPartControl(Composite parent) {
		//registerPartListener();
		super.createPartControl(parent);
		createActions();
		IToolBarManager tbm= getViewSite().getActionBars().getToolBarManager();
		configureToolBar(tbm);
		updateForExistingConsoles();
		getViewSite().getActionBars().updateActionBars();
	}
	
	/**
	 * Initialize for existing consoles
	 */
	private void updateForExistingConsoles() {
		ISynchronizeManager manager = TeamUI.getSynchronizeManager();
		// create pages for consoles
		ISynchronizeViewPage[] consoles = manager.getSynchronizePages();
		consolesAdded(consoles);
		// add as a listener
		manager.addSynchronizePageListener(this);		
	}	
}