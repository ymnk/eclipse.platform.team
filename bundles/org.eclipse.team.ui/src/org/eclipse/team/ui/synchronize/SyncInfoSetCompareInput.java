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
package org.eclipse.team.ui.synchronize;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.*;
import org.eclipse.compare.internal.INavigatable;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.ui.Utils;

/**
 * A <code>CompareEditorInput</code> whose diff viewer shows the resources contained
 * in a <code>SyncInfoSet</code>. The configuration of the diff viewer is determined by the 
 * <code>SyncInfoSetCompareConfiguration</code> that is used to create the 
 * <code>SyncInfoSetCompareInput</code>.
 * 
 * @since 3.0
 */
public class SyncInfoSetCompareInput extends CompareEditorInput {

	private DiffTreeViewerConfiguration diffViewerConfiguration;
	private NavigationAction nextAction;
	private NavigationAction previousAction;

	/**
	 * Create a <code>SyncInfoSetCompareInput</code> whose diff viewer is configured
	 * using the provided <code>SyncInfoSetCompareConfiguration</code>.
	 * @param configuration the compare configuration 
	 * @param diffViewerConfiguration the diff viewer configuration 
	 */
	public SyncInfoSetCompareInput(CompareConfiguration configuration, DiffTreeViewerConfiguration diffViewerConfiguration) {
		super(configuration);
		this.diffViewerConfiguration = diffViewerConfiguration;
	}

	public final Viewer createDiffViewer(Composite parent) {
		final StructuredViewer viewer = internalCreateDiffViewer(parent, diffViewerConfiguration);
		viewer.getControl().setData(CompareUI.COMPARE_VIEWER_TITLE, diffViewerConfiguration.getTitle());
		
		if(viewer instanceof INavigatable) { 
			INavigatable nav= new INavigatable() {
				public boolean gotoDifference(boolean next) {
					// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=20106
					return ((INavigatable)viewer).gotoDifference(next);
				}
			};
			viewer.getControl().setData(INavigatable.NAVIGATOR_PROPERTY, nav);
			
			nextAction = new NavigationAction(true);
			previousAction = new NavigationAction(false);
			nextAction.setCompareEditorInput(this);
			previousAction.setCompareEditorInput(this);
		}
		
		initializeToolBar(viewer.getControl().getParent());
		initializeDiffViewer(viewer);
		return viewer;
	}

	/**
	 * Create the diff viewer for this compare input. This method simply creates the widget.
	 * Any initialization is performed in the <code>initializeDiffViewer(StructuredViewer)</code>
	 * method. The default diff viewer is a <code>SyncInfoDiffTreeViewer</code>. Subclass may override.
	 * @param parent the parent <code>Composite</code> of the diff viewer to be created
	 * @param diffViewerConfiguration the configuration for the diff viewer
	 * @return the created diff viewer
	 */
	protected StructuredViewer internalCreateDiffViewer(Composite parent, DiffTreeViewerConfiguration diffViewerConfiguration) {
		return new SyncInfoDiffTreeViewer(parent, diffViewerConfiguration);
	}

	/**
	 * Initialize the diff viewer created for this compare input. If a subclass
	 * overrides the <code>createDiffViewer(Composite)</code> method, it should
	 * invoke this method on the created viewer in order to get the proper
	 * labelling in the compare input's contents viewers.
	 * @param viewer the diff viewer created by the compare input
	 */
	protected void initializeDiffViewer(StructuredViewer viewer) {
		viewer.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				ISelection s = event.getSelection();
				SyncInfoDiffNode node = getElement(s);
				if(node != null) {
					SyncInfo info = node.getSyncInfo();
					if(info != null && info.getLocal().getType() == IResource.FILE) { 
						Utils.updateLabels(node.getSyncInfo(), getCompareConfiguration());
					}
				}
			}
		});
	}

	public void contributeToToolBar(ToolBarManager tbm) {	
		if(nextAction != null && previousAction != null) { 
			tbm.appendToGroup("navigation", nextAction); //$NON-NLS-1$
			tbm.appendToGroup("navigation", previousAction); //$NON-NLS-1$
		}
	}
	
	private void initializeToolBar(Composite parent) {
		ToolBarManager tbm= CompareViewerPane.getToolBarManager(parent);
		if (tbm != null) {
			tbm.removeAll();
			tbm.add(new Separator("navigation")); //$NON-NLS-1$
			contributeToToolBar(tbm);
			tbm.update(true);
		}
	}
	
	/* private */ SyncInfoDiffNode getElement(ISelection selection) {
		if (selection != null && selection instanceof IStructuredSelection) {
			IStructuredSelection ss= (IStructuredSelection) selection;
			if (ss.size() == 1) {
				Object o = ss.getFirstElement();
				if(o instanceof SyncInfoDiffNode) {
					return (SyncInfoDiffNode)o;
				}
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#prepareInput(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected Object prepareInput(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		return diffViewerConfiguration.getInput();
	}	
}
