/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Common Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/cpl-v10.html Contributors:
 * IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.team.ui.synchronize;

import java.util.ArrayList;
import org.eclipse.compare.*;
import org.eclipse.compare.internal.CompareEditor;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.structuremergeviewer.*;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.core.Assert;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.*;
import org.eclipse.team.internal.ui.synchronize.LocalResourceTypedElement;
import org.eclipse.team.internal.ui.synchronize.SyncInfoModelElement;
import org.eclipse.ui.*;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.progress.IProgressService;

public class SynchronizeInput implements IContentChangeListener, ICompareContainer {

	private CompareConfiguration cc;
	private ISynchronizeParticipant participant;
	private IPageBookViewPage page;
	private Shell shell;
	
	private boolean fDirty= false;
	private ArrayList fDirtyViewers= new ArrayList();
	private IPropertyChangeListener fDirtyStateListener;
	
	//	 SWT controls
	private CompareViewerSwitchingPane fContentPane;
	private CompareViewerPane fMemberPane;
	private CompareViewerPane fEditionPane;
	private CompareViewerSwitchingPane fStructuredComparePane;
	private Viewer viewer;

	class CompareViewerPaneSite implements ISynchronizePageSite {
		public IWorkbenchPage getPage() {
			return null;
		}
		public ISelectionProvider getSelectionProvider() {
			return viewer;
		}
		public Shell getShell() {
			return shell;
		}
		public IWorkbenchWindow getWorkbenchWindow() {
			return null;
		}
		public void setSelectionProvider(ISelectionProvider provider) {
			//viewer.sets
		}
		public Object getAdapter(Class adapter) {
			return null;
		}
		public IWorkbenchSite getWorkbenchSite() {
			return null;
		}
		public IWorkbenchPart getPart() {
			return null;
		}
		public IKeyBindingService getKeyBindingService() {
			return null;
		}
		public void setFocus() {
		}	
	}
	
	public SynchronizeInput(Shell shell, CompareConfiguration cc, ISynchronizeParticipant participant) {
		this.cc = cc;
		this.shell = shell;
		this.participant = participant;
		
		fDirtyStateListener= new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				String propertyName= e.getProperty();
				if (CompareEditorInput.DIRTY_STATE.equals(propertyName)) {
					boolean changed= false;
					Object newValue= e.getNewValue();
					if (newValue instanceof Boolean)
						changed= ((Boolean)newValue).booleanValue();
					setDirty(e.getSource(), changed);
				}			
			}
		};
	}
	
	public Control createContents(Composite parent2) {
		Composite parent = new Composite(parent2, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessHorizontalSpace = true;
		parent.setLayout(layout);
		parent.setLayoutData(data);
		
		Splitter vsplitter = new Splitter(parent, SWT.VERTICAL);
		vsplitter.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_VERTICAL));
		// we need two panes: the left for the elements, the right one
		// for the structured diff
		Splitter hsplitter = new Splitter(vsplitter, SWT.HORIZONTAL);
		fEditionPane = new CompareViewerPane(hsplitter, SWT.BORDER | SWT.FLAT);
		fStructuredComparePane = new CompareViewerSwitchingPane(hsplitter, SWT.BORDER | SWT.FLAT, true) {

			protected Viewer getViewer(Viewer oldViewer, Object input) {
				if (input instanceof ICompareInput)
					return CompareUIPlugin.findStructureViewer(oldViewer, (ICompareInput) input, this, cc);
				return null;
			}
		};
		fStructuredComparePane.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				feedInput2(e.getSelection());
			}
		});
		fEditionPane.setText("Changes");
		
		ISynchronizePageConfiguration c = participant.createPageConfiguration();
		IPageBookViewPage page = participant.createPage(c);
		try {
			((ISynchronizePage)page).init(new CompareViewerPaneSite());
		} catch (PartInitException e1) {
		}

		page.createControl(fEditionPane);
		
		if(page instanceof ISynchronizePage) {
			((ISynchronizePage)page).getViewer().addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					ISelection sel = event.getSelection();
					if (sel instanceof IStructuredSelection) {
						IStructuredSelection ss= (IStructuredSelection) sel;
						if (ss.size() == 1)
							setInput(ss.getFirstElement());
					}
				}
			});
			initializeDiffViewer(((ISynchronizePage)page).getViewer());
		}
		
		ToolBarManager tbm = CompareViewerPane.getToolBarManager(fEditionPane);
		page.setActionBars(getActionBars(tbm));
		fEditionPane.setContent(page.getControl());
		tbm.update(true);
		if(page instanceof ISynchronizePage) {
			this.viewer = ((ISynchronizePage)page).getViewer();
		}
		
		fContentPane = new CompareViewerSwitchingPane(vsplitter, SWT.BORDER | SWT.FLAT) {
			protected Viewer getViewer(Viewer oldViewer, Object input) {
				Viewer newViewer= CompareUIPlugin.findContentViewer(oldViewer, input, this, cc);
				boolean isNewViewer= newViewer != oldViewer;
				if (isNewViewer && newViewer instanceof IPropertyChangeNotifier) {
					final IPropertyChangeNotifier dsp= (IPropertyChangeNotifier) newViewer;
					dsp.addPropertyChangeListener(fDirtyStateListener);
					Control c= newViewer.getControl();
					c.addDisposeListener(
						new DisposeListener() {
							public void widgetDisposed(DisposeEvent e) {
								dsp.removePropertyChangeListener(fDirtyStateListener);
							}
						}
					);
				}	
				return newViewer;
			}
		};
		vsplitter.setWeights(new int[]{30, 70});
		return parent;
	}
	
	private void setInput(Object input) {
		fContentPane.setInput(input);
		if (fStructuredComparePane != null)
			fStructuredComparePane.setInput(input);
	}
	
	/*
	 * Feeds selection from structure viewer to content viewer.
	 */
	private void feedInput2(ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			IStructuredSelection ss= (IStructuredSelection) sel;
			if (ss.size() == 1)
				fContentPane.setInput(ss.getFirstElement());
		}
	}
	
	/**
	 * Initialize the diff viewer created for this compare input. If a subclass
	 * overrides the <code>createDiffViewer(Composite)</code> method, it should
	 * invoke this method on the created viewer in order to get the proper
	 * labelling in the compare input's contents viewers.
	 * @param viewer the diff viewer created by the compare input
	 */
	protected void initializeDiffViewer(Viewer viewer) {
		if (viewer instanceof StructuredViewer) {
			((StructuredViewer) viewer).addOpenListener(new IOpenListener() {
				public void open(OpenEvent event) {
					ISelection s = event.getSelection();
					final SyncInfoModelElement node = getElement(s);
					if (node != null) {
						IResource resource = node.getResource();
						int kind = node.getKind();
						if (resource != null && resource.getType() == IResource.FILE) {
							// Cache the contents because compare doesn't show progress
							// when calling getContents on a diff node.
							IProgressService manager = PlatformUI.getWorkbench().getProgressService();
							try {
								node.cacheContents(new NullProgressMonitor());
								hookContentChangeListener(node);
							} catch (TeamException e) {
								Utils.handle(e);
							} finally {
								// Update the labels even if the content wasn't fetched correctly. This is
								// required because the selection would still of changed.
								Utils.updateLabels(node.getSyncInfo(), cc);
							}
						}
					}
				}
			});
		}
	}
	
	private void hookContentChangeListener(DiffNode node) {
		ITypedElement left = node.getLeft();
		if(left instanceof IContentChangeNotifier) {
			((IContentChangeNotifier)left).addContentChangeListener(this);
		}
		ITypedElement right = node.getRight();
		if(right instanceof IContentChangeNotifier) {
			((IContentChangeNotifier)right).addContentChangeListener(this);
		}
	}
	
	/* private */ SyncInfoModelElement getElement(ISelection selection) {
		if (selection != null && selection instanceof IStructuredSelection) {
			IStructuredSelection ss= (IStructuredSelection) selection;
			if (ss.size() == 1) {
				Object o = ss.getFirstElement();
				if(o instanceof SyncInfoModelElement) {
					return (SyncInfoModelElement)o;
				}
			}
		}
		return null;
	}
	
	/*
	 * (non-Javadoc)
	 * @see CompareEditorInput#saveChanges(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void saveChanges(IProgressMonitor pm) throws CoreException {
		//super.saveChanges(pm);
		ISynchronizeModelElement root = (ISynchronizeModelElement)viewer.getInput();
		if (root != null && root instanceof DiffNode) {
			try {
				commit(pm, (DiffNode)root);
			} finally {
				setDirty(false);
			}
		}
	}

	public Viewer getViewer() {
		return viewer;
	}
	
	private static void commit(IProgressMonitor pm, DiffNode node) throws CoreException {
		ITypedElement left = node.getLeft();
		if (left instanceof LocalResourceTypedElement)
			 ((LocalResourceTypedElement) left).commit(pm);

		ITypedElement right = node.getRight();
		if (right instanceof LocalResourceTypedElement)
			 ((LocalResourceTypedElement) right).commit(pm);
		
		//node.getC
		IDiffElement[] children = (IDiffElement[])node.getChildren();
		for (int i = 0; i < children.length; i++) {
			commit(pm, (DiffNode)children[i]);			
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.IContentChangeListener#contentChanged(org.eclipse.compare.IContentChangeNotifier)
	 */
	public void contentChanged(IContentChangeNotifier source) {
		try {
			if (source instanceof DiffNode) {
				commit(new NullProgressMonitor(), (DiffNode) source);
			} else if (source instanceof LocalResourceTypedElement) {
				 ((LocalResourceTypedElement) source).commit(new NullProgressMonitor());
			}
		} catch (CoreException e) {
			Utils.handle(e);
		}
	}
	
	/**
	 * Returns <code>true</code> if there are unsaved changes.
	 * The value returned is the value of the <code>DIRTY_STATE</code> property of this input object. 
	 * Returns <code>true</code> if this input has unsaved changes,
	 * that is if <code>setDirty(true)</code> has been called.
	 * Subclasses don't have to override if the functionality provided by <doce>setDirty</code>
	 * is sufficient.
	 *
	 * @return <code>true</code> if there are changes that need to be saved
	 */
	public boolean isSaveNeeded() {
		return fDirty || fDirtyViewers.size() > 0;
	}
		
	/**
	 * Sets the dirty state of this input to the given
	 * value and sends out a <code>PropertyChangeEvent</code> if the new value differs from the old value.
	 *
	 * @param dirty the dirty state for this compare input
	 */
	public void setDirty(boolean dirty) {

		boolean confirmSave= true;
		Object o= cc.getProperty(CompareEditor.CONFIRM_SAVE_PROPERTY);
		if (o instanceof Boolean)
			confirmSave= ((Boolean)o).booleanValue();

		if (!confirmSave) {
			fDirty= dirty;
			if (!fDirty)
				fDirtyViewers.clear();
		}
	}
	
	private void setDirty(Object source, boolean dirty) {
		Assert.isNotNull(source);
		boolean oldDirty= fDirtyViewers.size() > 0;
		if (dirty)
			fDirtyViewers.add(source);
		else
			fDirtyViewers.remove(source);
		boolean newDirty= fDirtyViewers.size() > 0;
	}	
	
	private IActionBars getActionBars(final IToolBarManager toolbar) {
		return new IActionBars() {
			public void clearGlobalActionHandlers() {
			}
			public IAction getGlobalActionHandler(String actionId) {
				return null;
			}
			public IMenuManager getMenuManager() {
				return null;
			}
			public IStatusLineManager getStatusLineManager() {
				return null;
			}
			public IToolBarManager getToolBarManager() {
				return toolbar;
			}
			public void setGlobalActionHandler(String actionId, IAction handler) {
			}
			public void updateActionBars() {
			}
		};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.ICompareContainer#getTitleImage()
	 */
	public ImageDescriptor getImageDescriptor() {
		return participant.getImageDescriptor();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.ICompareContainer#getTitle()
	 */
	public String getTitle() {
		return participant.getName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.ICompareContainer#getCompareConfiguration()
	 */
	public CompareConfiguration getCompareConfiguration() {
		return cc;
	}
}