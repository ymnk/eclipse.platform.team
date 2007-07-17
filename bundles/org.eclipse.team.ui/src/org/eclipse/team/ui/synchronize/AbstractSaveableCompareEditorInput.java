/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.synchronize;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.*;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.history.CompareFileRevisionEditorInput;
import org.eclipse.team.internal.ui.synchronize.LocalResourceSaveableComparison;
import org.eclipse.team.internal.ui.synchronize.LocalResourceTypedElement;
import org.eclipse.team.internal.ui.synchronize.EditableSharedDocumentAdapter.ISharedDocumentAdapterListener;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.keys.IBindingService;

/**
 * A compare editor input that makes use of a {@link Saveable} to manage the save
 * lifecycle of the left and/or right sides of the comparison.
 * <p>
 * This class is not intended to be subclassed outside the framework.
 * </p>
 * @since 3.4
 */
public abstract class AbstractSaveableCompareEditorInput extends
		CompareEditorInput implements ISaveablesSource {
	
	private final IWorkbenchPage page;
	private final ListenerList inputChangeListeners = new ListenerList(ListenerList.IDENTITY);

	/* package */ static ITypedElement getFileElement(ITypedElement element, CompareEditorInput editorInput) {
		if (element instanceof LocalResourceTypedElement) {
			return (LocalResourceTypedElement) element;
		}
		if (editorInput instanceof CompareFileRevisionEditorInput) {
			return ((CompareFileRevisionEditorInput) editorInput).getLocalElement();
		}
		return null;
	}
	
	/* package */ class InternalResourceSaveableComparison extends LocalResourceSaveableComparison implements ISharedDocumentAdapterListener {
		private LocalResourceTypedElement lrte;
		private boolean connected = false;
		public InternalResourceSaveableComparison(
				ICompareInput input, CompareEditorInput editorInput, ITypedElement element) {
			super(input, editorInput, element);
			if (element instanceof LocalResourceTypedElement) {
				lrte = (LocalResourceTypedElement) element;
				if (lrte.isConnected()) {
					registerSaveable(true);
				} else {
					lrte.setSharedDocumentListener(this);
				}
			}
		}
		protected void fireInputChange() {
			AbstractSaveableCompareEditorInput.this.fireInputChange();
		}
		public void dispose() {
			super.dispose();
			if (lrte != null)
				lrte.setSharedDocumentListener(null);
		}
		public void handleDocumentConnected() {
			if (connected)
				return;
			connected = true;
			registerSaveable(false);
			if (lrte != null)
				lrte.setSharedDocumentListener(null);
		}
		private void registerSaveable(boolean init) {
			ICompareContainer container = getContainer();
			IWorkbenchPart part = container.getWorkbenchPart();
			if (part != null) {
				ISaveablesLifecycleListener lifecycleListener= getSaveablesLifecycleListener(part);
				// Remove this saveable from the lifecycle listener
				if (!init)
					lifecycleListener.handleLifecycleEvent(
							new SaveablesLifecycleEvent(part, SaveablesLifecycleEvent.POST_CLOSE, new Saveable[] { this }, false));
				// Now fix the hashing so it uses the connected document
				initializeHashing();
				// Finally, add this saveable back to the listener
				lifecycleListener.handleLifecycleEvent(
						new SaveablesLifecycleEvent(part, SaveablesLifecycleEvent.POST_OPEN, new Saveable[] { this }, false));
			}
		}
		public void handleDocumentDeleted() {
			// Ignore
		}
		public void handleDocumentDisconnected() {
			// Ignore
		}
		public void handleDocumentFlushed() {
			// Ignore
		}
		public void handleDocumentSaved() {
			// Ignore
		}
	}
	
	public AbstractSaveableCompareEditorInput(CompareConfiguration configuration, IWorkbenchPage page) {
		super(configuration);
		this.page = page;
	}

	/* package */ ISaveablesLifecycleListener getSaveablesLifecycleListener(IWorkbenchPart part) {
		ISaveablesLifecycleListener listener = (ISaveablesLifecycleListener)Utils.getAdapter(part, ISaveablesLifecycleListener.class);
		if (listener == null)
			listener = (ISaveablesLifecycleListener) part.getSite().getService(ISaveablesLifecycleListener.class);
		return listener;
	}

	/**
	 * Prepare the compare input of this editor input. This method is not intended to be overridden of 
	 * extended by subclasses (but is not final for backwards compatibility reasons). 
	 * The implementation of this method in this class
	 * delegates the creation of the compare input to the {@link #prepareCompareInput(IProgressMonitor)}
	 * method which subclasses must implement.
	 * @see org.eclipse.compare.CompareEditorInput#prepareInput(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected Object prepareInput(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
				final ICompareInput input = prepareCompareInput(monitor);
				if (input != null)
					setTitle(NLS.bind(TeamUIMessages.SyncInfoCompareInput_title, new String[] { input.getName()}));
				return input;
			}
	
	/**
	 * Close the editor if it is not dirty. If it is still dirty, let the 
	 * content merge viewer handle the compare input change.
	 * @param checkForUnsavedChanges whether to check for unsaved changes
	 * @return <code>true</code> if the editor was closed (note that the 
	 * close may be asynchronous)
	 */
	protected boolean closeEditor(boolean checkForUnsavedChanges) {
		if (isSaveNeeded() && checkForUnsavedChanges) {
			return false;
		} else {
			Runnable runnable = new Runnable() {
				public void run() {
					IEditorPart part = getPage().findEditor(AbstractSaveableCompareEditorInput.this);
					getPage().closeEditor(part, false);
				}
			};
			if (Display.getCurrent() != null) {
				runnable.run();
			} else {
				Display display = getPage().getWorkbenchWindow().getShell().getDisplay();
				display.asyncExec(runnable);
			}
			return true;
		}
	}
	
	/**
	 * Method called from {@link #prepareInput(IProgressMonitor)} to obtain the input.
	 * It's purpose is to ensure that the input is an instance of {@link ICompareInput}.
	 * @param monitor a progress monitor
	 * @return the compare input
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 */
	protected abstract ICompareInput prepareCompareInput(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException;

	/* package */ IWorkbenchPage getPage() {
		if (page == null)
			return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		return page;
	}

	/**
	 * Return the compare input of this editor input.
	 * @return the compare input of this editor input
	 */
	protected final ICompareInput getCompareInput() {
		return (ICompareInput)getCompareResult();
	}

	/**
	 * Callback from the resource saveable that is invoked when the resource is
	 * saved so that this input can fire a change event for its input. Subclasses
	 * only need this method if the left side of their compare input is
	 * an element returned from {@link SaveableCompareEditorInput#createFileElement(IFile)}.
	 */
	protected abstract void fireInputChange();
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#addCompareInputChangeListener(org.eclipse.compare.structuremergeviewer.ICompareInput, org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener)
	 */
	public void addCompareInputChangeListener(ICompareInput input,
			ICompareInputChangeListener listener) {
		if (input == getCompareResult()) {
			inputChangeListeners.add(listener);
		} else {
			super.addCompareInputChangeListener(input, listener);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#removeCompareInputChangeListener(org.eclipse.compare.structuremergeviewer.ICompareInput, org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener)
	 */
	public void removeCompareInputChangeListener(ICompareInput input,
			ICompareInputChangeListener listener) {
		if (input == getCompareResult()) {
			inputChangeListeners.remove(listener);
		} else {
			super.removeCompareInputChangeListener(input, listener);
		}
	}
	
	/* package */ void propogateInputChange() {
		if (!inputChangeListeners.isEmpty()) {
			Object[] allListeners = inputChangeListeners.getListeners();
			final ICompareInput compareResult = (ICompareInput) getCompareResult();
			for (int i = 0; i < allListeners.length; i++) {
				final ICompareInputChangeListener listener = (ICompareInputChangeListener)allListeners[i];
				SafeRunner.run(new ISafeRunnable() {
					public void run() throws Exception {
						listener.compareInputChanged(compareResult);
					}
					public void handleException(Throwable exception) {
						// Logged by the safe runner
					}
				});
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablesSource#getActiveSaveables()
	 */
	public abstract Saveable[] getActiveSaveables();
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablesSource#getSaveables()
	 */
	public Saveable[] getSaveables() {
		return getActiveSaveables();
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#getTitleImage()
	 */
	public Image getTitleImage() {
		ImageRegistry reg = TeamUIPlugin.getPlugin().getImageRegistry();
		Image image = reg.get(ITeamUIImages.IMG_SYNC_VIEW);
		if (image == null) {
			image = getImageDescriptor().createImage();
			reg.put(ITeamUIImages.IMG_SYNC_VIEW, image);
		}
		return image;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
	 */
	public ImageDescriptor getImageDescriptor() {
		return TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_SYNC_VIEW);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#canRunAsJob()
	 */
	public boolean canRunAsJob() {
		return true;
	}
	
	/* package */ void handleMenuAboutToShow(IMenuManager manager, Saveable saveable, ITypedElement element,
			ISelectionProvider provider) {
		if (provider instanceof ITextViewer) {
			ITextViewer v = (ITextViewer) provider;
			IDocument d = v.getDocument();
			IDocument other = (IDocument)Utils.getAdapter(saveable, IDocument.class);
			if (d == other) {
				if (element instanceof IResourceProvider) {
					IResourceProvider rp = (IResourceProvider) element;
					IResource resource = rp.getResource();
					StructuredSelection selection = new StructuredSelection(resource);
					IWorkbenchPart workbenchPart = getContainer().getWorkbenchPart();
					if (workbenchPart != null) {
						IWorkbenchSite ws = workbenchPart.getSite();
						MenuManager submenu1 =
							new MenuManager(getShowInMenuLabel());
						IContributionItem showInMenu = ContributionItemFactory.VIEWS_SHOW_IN.create(ws.getWorkbenchWindow());
						submenu1.add(showInMenu);
						manager.insertAfter("file", submenu1); //$NON-NLS-1$
						MenuManager submenu2 =
							new MenuManager(TeamUIMessages.OpenWithActionGroup_0); 
						submenu2.add(new OpenWithMenu(ws.getPage(), resource));
						manager.insertAfter("file", submenu2); //$NON-NLS-1$

						OpenFileAction openFileAction = new OpenFileAction(ws.getPage());
						openFileAction.selectionChanged(selection);
						manager.insertAfter("file", openFileAction); //$NON-NLS-1$
					}
				}
			}
		}
	}
	
	/* package */ String getShowInMenuLabel() {
		String keyBinding= null;
		
		IBindingService bindingService= (IBindingService)PlatformUI.getWorkbench().getAdapter(IBindingService.class);
		if (bindingService != null)
			keyBinding= bindingService.getBestActiveBindingFormattedFor("org.eclipse.ui.navigate.showInQuickMenu"); //$NON-NLS-1$
		
		if (keyBinding == null)
			keyBinding= ""; //$NON-NLS-1$
		
		return NLS.bind(TeamUIMessages.SaveableCompareEditorInput_0, keyBinding);
	}
}
