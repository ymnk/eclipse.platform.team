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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.*;
import org.eclipse.compare.structuremergeviewer.*;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.internal.ui.mapping.AbstractCompareInput;
import org.eclipse.team.internal.ui.mapping.CompareInputChangeNotifier;
import org.eclipse.team.internal.ui.synchronize.LocalResourceSaveableComparison;
import org.eclipse.team.internal.ui.synchronize.LocalResourceTypedElement;
import org.eclipse.team.ui.mapping.SaveableComparison;
import org.eclipse.ui.*;
import org.eclipse.ui.services.IDisposable;

public class TwoSidesSaveableCompareEditorInput extends
		AbstractSaveableCompareEditorInput {
	
	private ICompareInputChangeListener fCompareInputChangeListener;
	private IPropertyListener fLeftPropertyListener;
	private IPropertyListener fRightPropertyListener;
	
	private Saveable fLeftSaveable;
	private Saveable fRightSaveable;
	
	private ITypedElement fLeftElement;
	private ITypedElement fRightElement;
	
	public TwoSidesSaveableCompareEditorInput(ITypedElement left,
			ITypedElement right, IWorkbenchPage page) {
		super(new CompareConfiguration(), page);
		this.fLeftElement = left;
		this.fRightElement = right;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#contentsCreated()
	 */
	protected void contentsCreated() {
		super.contentsCreated();
		fCompareInputChangeListener = new ICompareInputChangeListener() {
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
		getCompareInput().addCompareInputChangeListener(fCompareInputChangeListener);
		
		if (getLeftSaveable() instanceof SaveableComparison) {
			SaveableComparison lscm = (SaveableComparison) fLeftSaveable;
			fLeftPropertyListener = new IPropertyListener() {
				public void propertyChanged(Object source, int propId) {
					if (propId == SaveableComparison.PROP_DIRTY) {
						setDirty(fLeftSaveable.isDirty());
					}
				}
			};
			lscm.addPropertyListener(fLeftPropertyListener);
		}
		
		if (getRightSaveable() instanceof SaveableComparison) {
			SaveableComparison rscm = (SaveableComparison) fRightSaveable;
			fRightPropertyListener = new IPropertyListener() {
				public void propertyChanged(Object source, int propId) {
					if (propId == SaveableComparison.PROP_DIRTY) {
						setDirty(fRightSaveable.isDirty());
					}
				}
			};
			rscm.addPropertyListener(fRightPropertyListener);
		}
		
		setDirty(fLeftSaveable.isDirty() || fRightSaveable.isDirty());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#handleDispose()
	 */
	protected void handleDispose() {
		super.handleDispose();
		ICompareInput compareInput = getCompareInput();
		if (compareInput != null)
			compareInput.removeCompareInputChangeListener(fCompareInputChangeListener);
		if (fLeftSaveable instanceof SaveableComparison) {
			SaveableComparison scm = (SaveableComparison) fLeftSaveable;
			scm.removePropertyListener(fLeftPropertyListener);
		}
		if (fLeftSaveable instanceof LocalResourceSaveableComparison) {
			LocalResourceSaveableComparison rsc = (LocalResourceSaveableComparison) fLeftSaveable;
			rsc.dispose();
		}
		if (fRightSaveable instanceof SaveableComparison) {
			SaveableComparison scm = (SaveableComparison) fRightSaveable;
			scm.removePropertyListener(fRightPropertyListener);
		}
		if (fRightSaveable instanceof LocalResourceSaveableComparison) {
			LocalResourceSaveableComparison rsc = (LocalResourceSaveableComparison) fRightSaveable;
			rsc.dispose();
		}

		if (getCompareResult() instanceof IDisposable) {
			((IDisposable) getCompareResult()).dispose();
		}
	}
	
	public String getToolTipText() {
		IResource leftResource = getResource(fLeftElement);
		IResource rightResource = getResource(fRightElement);

		if (leftResource != null && rightResource != null) {
			String leftLabel = leftResource.getFullPath().makeRelative().toString();
			String rightLabel = rightResource.getFullPath().makeRelative().toString();
			// TODO (TomaszZarna): Externalize
			return NLS.bind("Two-way compare of {0} with {1}", new String[] { leftLabel, rightLabel });  //$NON-NLS-1$
		}
		// TODO (TomaszZarna): Externalize
		return NLS.bind("Two-way compare of {0} with {1}", new String[] { fLeftElement.getName(), fRightElement.getName() }); //$NON-NLS-1$
	}
	
	protected void fireInputChange() {
			((MyDiffNode)getCompareResult()).fireChange();
	}
	
	protected Saveable getLeftSaveable() {
		if (fLeftSaveable == null) {
			fLeftSaveable = createLeftSaveable();
		}
		return fLeftSaveable;
	}
	
	protected Saveable getRightSaveable() {
		if (fRightSaveable == null) {
			fRightSaveable = createRightSaveable();
		}
		return fRightSaveable;
	}
	
	protected Saveable createLeftSaveable() {
		Object compareResult = getCompareResult();
		Assert.isNotNull(compareResult, "This method cannot be called until after prepareInput is called"); //$NON-NLS-1$
		ITypedElement leftFileElement = getFileElement(getCompareInput().getLeft(), this);
		return new InternalResourceSaveableComparison((ICompareInput)compareResult, this, leftFileElement);
	}
	
	protected Saveable createRightSaveable() {
		Object compareResult = getCompareResult();
		Assert.isNotNull(compareResult, "This method cannot be called until after prepareInput is called"); //$NON-NLS-1$
		ITypedElement rightFileElement = getFileElement(getCompareInput().getRight(), this);
		return new InternalResourceSaveableComparison((ICompareInput)compareResult, this, rightFileElement);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablesSource#getActiveSaveables()
	 */
	public Saveable[] getActiveSaveables() {
		if (getCompareResult() == null)
			return new Saveable[0];
		return new Saveable[] { getLeftSaveable(), getRightSaveable() };
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#findContentViewer(org.eclipse.jface.viewers.Viewer, org.eclipse.compare.structuremergeviewer.ICompareInput, org.eclipse.swt.widgets.Composite)
	 */
	public Viewer findContentViewer(Viewer pOldViewer, ICompareInput pInput, Composite pParent) {
		Viewer newViewer = super.findContentViewer(pOldViewer, pInput, pParent);
		boolean isNewViewer= newViewer != pOldViewer;
		if (isNewViewer && newViewer instanceof IPropertyChangeNotifier
				&& fLeftSaveable instanceof IPropertyChangeListener
				&& fRightSaveable instanceof IPropertyChangeListener) {
			// Register the model for change events if appropriate
			final IPropertyChangeNotifier dsp= (IPropertyChangeNotifier) newViewer;
			final IPropertyChangeListener lpcl = (IPropertyChangeListener) fLeftSaveable;
			final IPropertyChangeListener rpcl = (IPropertyChangeListener) fRightSaveable;
			dsp.addPropertyChangeListener(lpcl);
			dsp.addPropertyChangeListener(rpcl);
			Control c= newViewer.getControl();
			c.addDisposeListener(
				new DisposeListener() {
					public void widgetDisposed(DisposeEvent e) {
						dsp.removePropertyChangeListener(lpcl);
						dsp.removePropertyChangeListener(rpcl);
					}
				}
			);
		}
		return newViewer;
	}
	
	
	public boolean isDirty() {
		if (fLeftSaveable != null && fLeftSaveable.isDirty())
			return true;
		if (fRightSaveable != null && fRightSaveable.isDirty())
			return true;
		return super.isDirty();
	}
	
	protected ICompareInput prepareCompareInput(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		ICompareInput input = createCompareInput();
		getCompareConfiguration().setLeftEditable(isLeftEditable(input)); 
		getCompareConfiguration().setRightEditable(isRightEditable(input));
		// TODO (TomaszZarna): both are local
		// ensureContentsCached(getLeftRevision(), getRightRevision(), monitor);
		initLabels();
		return input;
	}
	
	private boolean isLeftEditable(ICompareInput pInput) {
		Object left = pInput.getLeft();
		if (left instanceof IEditableContent) {
			return ((IEditableContent) left).isEditable();
		}
		return false;
	}
	
	private boolean isRightEditable(ICompareInput pInput) {
		Object right = pInput.getRight();
		if (right instanceof IEditableContent) {
			return ((IEditableContent) right).isEditable();
		}
		return false;
	}
	
	private void initLabels() {
		CompareConfiguration cc = getCompareConfiguration();
		
		IResource leftResource = getResource(fLeftElement);
		IResource rightResource = getResource(fRightElement);
		
		if (leftResource != null && rightResource != null) {
			String leftLabel = leftResource.getFullPath().makeRelative()
					.toString();
			String rightLabel = rightResource.getFullPath().makeRelative()
					.toString();
			
			cc.setLeftLabel(leftLabel);
			cc.setRightLabel(rightLabel);
		}
	}
	
	private ICompareInput createCompareInput() {
		return new MyDiffNode(fLeftElement, fRightElement);
	}
	
	private CompareInputChangeNotifier notifier = new CompareInputChangeNotifier() {
		protected IResource[] getResources(ICompareInput input) {
			IResource leftResource = getResource(fLeftElement);
			IResource rightResource = getResource(fRightElement);
			if (leftResource == null && rightResource == null)
				return new IResource[0];
			if (leftResource == null && rightResource != null)
				return new IResource[] { rightResource };
			if (leftResource != null && rightResource == null)
				return new IResource[] { leftResource };
			return new IResource[] { leftResource, rightResource };
		}
	};
	
	private class MyDiffNode extends AbstractCompareInput {
		public MyDiffNode(ITypedElement left, ITypedElement right) {
			super(Differencer.CHANGE, null, left, right);
		}
		public void fireChange() {
			super.fireChange();
		}
		protected CompareInputChangeNotifier getChangeNotifier() {
			return notifier;
		}
		public boolean needsUpdate() {
			return true;
		}
		public void update() {
			fireChange();
		}
	}
	
	private IResource getResource(ITypedElement pElement) {
		if (pElement instanceof LocalResourceTypedElement
				&& pElement instanceof IResourceProvider) {
			return ((IResourceProvider) pElement).getResource();
		}
		return null;
	}

	public void registerContextMenu(final MenuManager pMenuManager,
			final ISelectionProvider pSelectionProvider) {
		super.registerContextMenu(pMenuManager, pSelectionProvider);
		final Saveable lLeftSaveable = getLeftSaveable();
		final ITypedElement lLeftElement = getFileElement(getCompareInput().getLeft(), this);
		if (lLeftSaveable instanceof LocalResourceSaveableComparison) {
			pMenuManager.addMenuListener(new IMenuListener() {
				public void menuAboutToShow(IMenuManager manager) {
					handleMenuAboutToShow(manager, lLeftSaveable, lLeftElement, pSelectionProvider);
				}
			});
		}
		final Saveable lRightSaveable = getRightSaveable();
		final ITypedElement lRightElement = getFileElement(getCompareInput().getRight(), this);
		if (lRightSaveable instanceof LocalResourceSaveableComparison) {
			pMenuManager.addMenuListener(new IMenuListener() {
				public void menuAboutToShow(IMenuManager manager) {
					handleMenuAboutToShow(manager, lRightSaveable, lRightElement, pSelectionProvider);
				}
			});
		}
	}
	
}
