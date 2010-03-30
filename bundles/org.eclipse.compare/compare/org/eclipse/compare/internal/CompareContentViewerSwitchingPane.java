/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareViewerSwitchingPane;
import org.eclipse.compare.Splitter;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class CompareContentViewerSwitchingPane extends
		CompareViewerSwitchingPane {

	private static final String OPTIMIZED_WARNING_IMAGE_NAME = "obj16/warning_st_obj.gif"; //$NON-NLS-1$
	public static final String OPTIMIZED_ALGORITHM_USED = "OPTIMIZED_ALGORITHM_USED"; //$NON-NLS-1$

	private CompareEditorInput fCompareEditorInput;

	private CLabel clOptimized;

	public CompareContentViewerSwitchingPane(Splitter parent, int style,
			CompareEditorInput cei) {
		super(parent, style);
		fCompareEditorInput = cei;
	}

	private CompareConfiguration getCompareConfiguration() {
		return fCompareEditorInput.getCompareConfiguration();
	}

	protected Viewer getViewer(Viewer oldViewer, Object input) {
		if (input instanceof ICompareInput)
			return fCompareEditorInput.findContentViewer(oldViewer,
					(ICompareInput) input, this);
		return null;
	}

	protected Control createTopLeft(Composite p) {
		final Composite composite = new Composite(p, SWT.NONE) {
			public Point computeSize(int wHint, int hHint, boolean changed) {
				return super.computeSize(wHint, Math.max(24, hHint), changed);
			}
		};

		RowLayout layout = new RowLayout();
		layout.marginTop = 0;
		composite.setLayout(layout);

		CLabel cl = new CLabel(composite, SWT.NONE);
		cl.setText(null);

		clOptimized = new CLabel(composite, SWT.NONE);
		clOptimized
				.setText(CompareMessages.CompareContentViewerSwitchingPane_optimized);
		clOptimized
				.setToolTipText(CompareMessages.CompareContentViewerSwitchingPane_optimizedTooltip);
		clOptimized.setImage(CompareUIPlugin.getImageDescriptor(
				OPTIMIZED_WARNING_IMAGE_NAME).createImage());
		clOptimized.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				Image img = clOptimized.getImage();
				if ((img != null) && (!img.isDisposed())) {
					img.dispose();
				}
			}
		});

		return composite;
	}

	public void setInput(Object input) {
		super.setInput(input);
		CompareConfiguration cc = getCompareConfiguration();
		Boolean isOptimized = (Boolean) cc
				.getProperty(OPTIMIZED_ALGORITHM_USED);
		clOptimized.setVisible(isOptimized != null
				&& isOptimized.booleanValue());
	}

	public void setText(String label) {
		Composite c = (Composite) getTopLeft();
		if (c == null)
			return;
		Control[] children = c.getChildren();
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof CLabel) {
				CLabel cl = (CLabel) children[i];
				if (cl != null && !cl.isDisposed()) {
					cl.setText(label);
					c.layout();
				}
				return;
			}
		}
	}

	public void setImage(Image image) {
		Composite c = (Composite) getTopLeft();
		if (c == null)
			return;
		Control[] children = c.getChildren();
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof CLabel) {
				CLabel cl = (CLabel) children[i];
				if (cl != null && !cl.isDisposed())
					cl.setImage(image);
				return;
			}
		}
	}

	public void addMouseListener(MouseListener listener) {
		Composite c = (Composite) getTopLeft();
		if (c == null)
			return;
		Control[] children = c.getChildren();
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof CLabel) {
				CLabel cl = (CLabel) children[i];
				cl.addMouseListener(listener);
			}
		}
	}

}
