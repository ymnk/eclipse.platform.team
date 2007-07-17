/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.synchronize;

import org.eclipse.compare.*;
import org.eclipse.compare.structuremergeviewer.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.LocalResourceSaveableComparison;
import org.eclipse.team.internal.ui.synchronize.LocalResourceTypedElement;
import org.eclipse.team.ui.mapping.SaveableComparison;
import org.eclipse.ui.*;
import org.eclipse.ui.services.IDisposable;

/**
 * A compare editor input that makes use of a {@link Saveable} to manage the save
 * lifecycle of the editor input. If the element returned from 
 * {@link #createFileElement(IFile)} is used as the left side of the compare input
 * and the default saveable returned from {@link #createSaveable()} is used, then
 * this compare input will provide the complete save lifecycle for the local file.
 * <p>
 * Clients may subclass this class.
 * </p>
 * @since 3.3
 */
public abstract class SaveableCompareEditorInput extends AbstractSaveableCompareEditorInput {

	private ICompareInputChangeListener compareInputChangeListener;
	private Saveable saveable;
	private IPropertyListener propertyListener;
	
	/**
	 * Return a typed element that represents a local file. If the element
	 * returned from this method is used as the left contributor of the compare
	 * input for a {@link SaveableCompareEditorInput}, then the file will
	 * be properly saved when the compare editor input or viewers are saved.
	 * @param file the file
	 * @return a typed element that represents a local file.
	 */
	public static ITypedElement createFileElement(IFile file) {
		return new LocalResourceTypedElement(file);
	}
	
	/**
	 * Creates a <code>LocalResourceCompareEditorInput</code> which is initialized with the given
	 * compare configuration.
	 * The compare configuration is passed to subsequently created viewers.
	 *
	 * @param configuration the compare configuration 
	 * @param page the workbench page that will contain the editor
	 */
	public SaveableCompareEditorInput(CompareConfiguration configuration, IWorkbenchPage page) {
		super(configuration, page);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#contentsCreated()
	 */
	protected void contentsCreated() {
		super.contentsCreated();
		compareInputChangeListener = new ICompareInputChangeListener() {
			public void compareInputChanged(ICompareInput source) {
				if (source == getCompareResult()) {
					boolean closed = false;
					if (source.getKind() == Differencer.NO_CHANGE) {
						closed = closeEditor(true);
					}
					if (!closed) {
						// The editor was closed either because the compare input still has changes
						// or because the editor input is dirty. In either case, fire the changes
						// to the registered listeners
						propogateInputChange();
					}
				}
			}
		};
		getCompareInput().addCompareInputChangeListener(compareInputChangeListener);
		
		if (getSaveable() instanceof SaveableComparison) {
			SaveableComparison scm = (SaveableComparison) saveable;
			propertyListener = new IPropertyListener() {
				public void propertyChanged(Object source, int propId) {
					if (propId == SaveableComparison.PROP_DIRTY) {
						setDirty(saveable.isDirty());
					}
				}
			};
			scm.addPropertyListener(propertyListener);
		}
		setDirty(saveable.isDirty());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#handleDispose()
	 */
	protected void handleDispose() {
		super.handleDispose();
		ICompareInput compareInput = getCompareInput();
		if (compareInput != null)
			compareInput.removeCompareInputChangeListener(compareInputChangeListener);
		if (saveable instanceof SaveableComparison) {
			SaveableComparison scm = (SaveableComparison) saveable;
			scm.removePropertyListener(propertyListener);
		}
		if (saveable instanceof LocalResourceSaveableComparison) {
			LocalResourceSaveableComparison rsc = (LocalResourceSaveableComparison) saveable;
			rsc.dispose();
		}
		if (getCompareResult() instanceof IDisposable) {
			((IDisposable) getCompareResult()).dispose();
		}
	}
	
	/**
	 * Get the saveable that provides the save behavior for this compare editor input.
	 * The {@link #createSaveable()} is called to create the saveable if it does not yet exist.
	 * This method cannot be called until after the input is prepared (i.e. until after
	 * the {@link #run(IProgressMonitor)} method is called which will in turn will invoke 
	 * {@link #prepareCompareInput(IProgressMonitor)}.
	 * @return saveable that provides the save behavior for this compare editor input.
	 */
	protected Saveable getSaveable() {
		if (saveable == null) {
			saveable = createSaveable();
		}
		return saveable;
	}
	
	/**
	 * Create the saveable that provides the save behavior for this compare editor input.
	 * By default, a saveable that handles local files is returned
	 * @return the saveable that provides the save behavior for this compare editor input
	 */
	protected Saveable createSaveable() {
		Object compareResult = getCompareResult();
		Assert.isNotNull(compareResult, "This method cannot be called until after prepareInput is called"); //$NON-NLS-1$
		return new InternalResourceSaveableComparison((ICompareInput)compareResult, this, getFileElement(getCompareInput().getLeft(), this));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablesSource#getActiveSaveables()
	 */
	public Saveable[] getActiveSaveables() {
		if (getCompareResult() == null)
			return new Saveable[0];
		return new Saveable[] { getSaveable() };
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (IFile.class.equals(adapter)) {
			IResource resource = Utils.getResource(getCompareResult());
			if (resource instanceof IFile) {
				return resource;
			}
		}
		return super.getAdapter(adapter);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#findContentViewer(org.eclipse.jface.viewers.Viewer, org.eclipse.compare.structuremergeviewer.ICompareInput, org.eclipse.swt.widgets.Composite)
	 */
	public Viewer findContentViewer(Viewer oldViewer, ICompareInput input, Composite parent) {
		Viewer newViewer = super.findContentViewer(oldViewer, input, parent);
		boolean isNewViewer= newViewer != oldViewer;
		if (isNewViewer && newViewer instanceof IPropertyChangeNotifier && saveable instanceof IPropertyChangeListener) {
			// Register the model for change events if appropriate
			final IPropertyChangeNotifier dsp= (IPropertyChangeNotifier) newViewer;
			final IPropertyChangeListener pcl = (IPropertyChangeListener) saveable;
			dsp.addPropertyChangeListener(pcl);
			Control c= newViewer.getControl();
			c.addDisposeListener(
				new DisposeListener() {
					public void widgetDisposed(DisposeEvent e) {
						dsp.removePropertyChangeListener(pcl);
					}
				}
			);
		}
		return newViewer;
	}

	public boolean isDirty() {
		if (saveable != null)
			return saveable.isDirty();
		return super.isDirty();
	}
	
	public void registerContextMenu(final MenuManager menu,
			final ISelectionProvider selectionProvider) {
		super.registerContextMenu(menu, selectionProvider);
		final Saveable saveable = getSaveable();
		final ITypedElement element = getFileElement(getCompareInput().getLeft(), this);
		if (saveable instanceof LocalResourceSaveableComparison) {
			menu.addMenuListener(new IMenuListener() {
				public void menuAboutToShow(IMenuManager manager) {
					handleMenuAboutToShow(manager, saveable, element, selectionProvider);
				}
			});
		}
	}

}
