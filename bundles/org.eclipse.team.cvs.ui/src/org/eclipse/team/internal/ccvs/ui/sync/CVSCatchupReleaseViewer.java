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
package org.eclipse.team.internal.ccvs.ui.sync;

 
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.structuremergeviewer.DiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.sync.IRemoteResource;
import org.eclipse.team.core.sync.IRemoteSyncElement;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ui.OverlayIcon;
import org.eclipse.team.internal.ui.sync.*;
import org.eclipse.ui.help.WorkbenchHelp;

public class CVSCatchupReleaseViewer extends CatchupReleaseViewer {
	// Actions
	private HistoryAction showInHistory;

	private Image conflictImage;
	
	private static class DiffOverlayIcon extends OverlayIcon {
		private static final int HEIGHT = 16;
		private static final int WIDTH = 22;
		public DiffOverlayIcon(Image baseImage, ImageDescriptor[] overlays, int[] locations) {
			super(baseImage, overlays, locations, new Point(WIDTH, HEIGHT));
		}
		protected void drawOverlays(ImageDescriptor[] overlays, int[] locations) {
			Point size = getSize();
			for (int i = 0; i < overlays.length; i++) {
				ImageDescriptor overlay = overlays[i];
				ImageData overlayData = overlay.getImageData();
				switch (locations[i]) {
					case TOP_LEFT:
						drawImage(overlayData, 0, 0);			
						break;
					case TOP_RIGHT:
						drawImage(overlayData, size.x - overlayData.width, 0);			
						break;
					case BOTTOM_LEFT:
						drawImage(overlayData, 0, size.y - overlayData.height);			
						break;
					case BOTTOM_RIGHT:
						drawImage(overlayData, size.x - overlayData.width, size.y - overlayData.height);			
						break;
				}
			}
		}
	}
	
	private static class HistoryAction extends Action implements ISelectionChangedListener {
		IStructuredSelection selection;
		public HistoryAction(String label) {
			super(label);
		}
		public void run() {
			if (selection.isEmpty()) {
				return;
			}
			HistoryView view = HistoryView.openInActivePerspective();
			if (view == null) {
				return;
			}
			ITeamNode node = (ITeamNode)selection.getFirstElement();
			IRemoteSyncElement remoteSyncElement = ((TeamFile)node).getMergeResource().getSyncElement();
			ICVSRemoteFile remoteFile = (ICVSRemoteFile)remoteSyncElement.getRemote();
			IResource local = remoteSyncElement.getLocal();
			
			ICVSRemoteFile baseFile = (ICVSRemoteFile)remoteSyncElement.getBase();
			if(baseFile == null) {
				try {
					baseFile = (ICVSRemoteFile)CVSWorkspaceRoot.getRemoteResourceFor(local);
				} catch (CVSException e) {
					baseFile = null;
				}
			}
			
			if(local.exists()) {
				view.showHistory(local);
			}else if (baseFile != null) {
				view.showHistory(baseFile);
			} else if (remoteFile != null) {
				view.showHistory(remoteFile);
			} 
		}
		public void selectionChanged(SelectionChangedEvent event) {
			ISelection selection = event.getSelection();
			if (!(selection instanceof IStructuredSelection)) {
				setEnabled(false);
				return;
			}
			IStructuredSelection ss = (IStructuredSelection)selection;
			if (ss.size() != 1) {
				setEnabled(false);
				return;
			}
			ITeamNode first = (ITeamNode)ss.getFirstElement();
			if (first instanceof TeamFile) {
				// can only show history on elements that have a remote file
				this.selection = ss;
				IRemoteSyncElement remoteSyncElement = ((TeamFile)first).getMergeResource().getSyncElement();
				if(remoteSyncElement.getRemote() != null || remoteSyncElement.getBase() != null) {
					setEnabled(true);
				} else {
					setEnabled(false);
				}
			} else {
				this.selection = null;
				setEnabled(false);
			}
		}
	}
	
	public CVSCatchupReleaseViewer(Composite parent, CVSSyncCompareInput model) {
		super(parent, model);
		initializeActions(model);
		initializeLabelProvider();
		// set F1 help
		WorkbenchHelp.setHelp(this.getControl(), IHelpContextIds.CATCHUP_RELEASE_VIEWER);
	}
	
	private static class Decoration implements IDecoration {
		public String prefix = ""; //$NON-NLS-1$
		public String suffix = ""; //$NON-NLS-1$
		public ImageDescriptor overlay;

		/**
		 * @see org.eclipse.jface.viewers.IDecoration#addPrefix(java.lang.String)
		 */
		public void addPrefix(String prefix) {
			this.prefix = prefix;
		}
		/**
		 * @see org.eclipse.jface.viewers.IDecoration#addSuffix(java.lang.String)
		 */
		public void addSuffix(String suffix) {
			this.suffix = suffix;
		}
		/**
		 * @see org.eclipse.jface.viewers.IDecoration#addOverlay(org.eclipse.jface.resource.ImageDescriptor)
		 */
		public void addOverlay(ImageDescriptor overlay) {
			this.overlay = overlay;
		}
	}
		
	private void initializeLabelProvider() {
		final LabelProvider oldProvider = (LabelProvider)getLabelProvider();
		
		
		setLabelProvider(new LabelProvider() {
			private OverlayIconCache iconCache = new OverlayIconCache();
			
			public void dispose() {
				iconCache.disposeAll();
				oldProvider.dispose();
				if(conflictImage != null)	
					conflictImage.dispose();
			}
			
			public Image getImage(Object element) {
				Image image = oldProvider.getImage(element);

				if (! (element instanceof ITeamNode))
					return image;
				
				ITeamNode node = (ITeamNode)element;
				IResource resource = node.getResource();

				if (! resource.exists())
					return image;
					
				CVSTeamProvider provider = (CVSTeamProvider)RepositoryProvider.getProvider(resource.getProject(), CVSProviderPlugin.getTypeId());
				List overlays = new ArrayList();
				List locations = new ArrayList();
				
				// use the default cvs image decorations
				ImageDescriptor resourceOverlay = CVSLightweightDecorator.getOverlay(node.getResource(),false, provider);
				
				int kind = node.getKind();
				boolean conflict = (kind & IRemoteSyncElement.AUTOMERGE_CONFLICT) != 0;

				if(resourceOverlay != null) {
					overlays.add(resourceOverlay);
					locations.add(new Integer(OverlayIcon.BOTTOM_RIGHT));
				}
				
				if(conflict) {
					overlays.add(CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_MERGEABLE_CONFLICT));
					locations.add(new Integer(OverlayIcon.TOP_LEFT));
				}

				if (overlays.isEmpty()) {
					return image;
				}

				//combine the descriptors and return the resulting image
				Integer[] integers = (Integer[])locations.toArray(new Integer[locations.size()]);
				int[] locs = new int[integers.length];
				for (int i = 0; i < integers.length; i++) {
					locs[i] = integers[i].intValue();
				}
				
				return iconCache.getImageFor(new DiffOverlayIcon(image,
					(ImageDescriptor[]) overlays.toArray(new ImageDescriptor[overlays.size()]),
					locs));
			}

			public String getText(Object element) {
				String label = oldProvider.getText(element);
				if (! (element instanceof ITeamNode))
					return label;
					
				ITeamNode node = (ITeamNode)element;					
				IResource resource = node.getResource();

				if (resource.exists()) {
					// use the default text decoration preferences
					Decoration decoration = new Decoration();
					CVSLightweightDecorator.decorateTextLabel(resource, decoration, false /*don't show dirty*/, false /*don't show revisions*/);
					label = decoration.prefix + label + decoration.suffix;
				}
				return label;
			}								
		});
	}
	
	protected void fillContextMenu(IMenuManager manager) {
		super.fillContextMenu(manager);
		if (showInHistory != null) {
			manager.add(showInHistory);
		}
	}
	
	/**
	 * Creates the actions for this viewer.
	 */
	private void initializeActions(final CVSSyncCompareInput diffModel) {		// Show in history view
		showInHistory = new HistoryAction(Policy.bind("CVSCatchupReleaseViewer.showInHistory")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(showInHistory, IHelpContextIds.SHOW_IN_RESOURCE_HISTORY);
		addSelectionChangedListener(showInHistory);
	}
	
	protected void mergeRecursive(IDiffElement element, List needsMerge) {
		if (element instanceof DiffContainer) {
			DiffContainer container = (DiffContainer)element;
			IDiffElement[] children = container.getChildren();
			for (int i = 0; i < children.length; i++) {
				mergeRecursive(children[i], needsMerge);
			}
		} else if (element instanceof TeamFile) {
			TeamFile file = (TeamFile)element;
			needsMerge.add(file);			
		}
	}
	
	/**
	 * Provide CVS-specific labels for the editors.
	 */
	protected void updateLabels(MergeResource resource) {
		CompareConfiguration config = getCompareConfiguration();
		String name = resource.getName();
		config.setLeftLabel(Policy.bind("CVSCatchupReleaseViewer.workspaceFile", name)); //$NON-NLS-1$
	
		IRemoteSyncElement syncTree = resource.getSyncElement();
		IRemoteResource remote = syncTree.getRemote();
		if (remote != null) {
			try {
				final ICVSRemoteFile remoteFile = (ICVSRemoteFile)remote;
				String revision = remoteFile.getRevision();
				final String[] author = new String[] { "" }; //$NON-NLS-1$
				try {
					CVSUIPlugin.runWithProgress(getTree().getShell(), true /*cancelable*/,
						new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							try {
								ILogEntry logEntry = remoteFile.getLogEntry(monitor);
								if (logEntry != null)
									author[0] = logEntry.getAuthor();
							} catch (TeamException e) {
								throw new InvocationTargetException(e);
							}
						}
					});
				} catch (InterruptedException e) { // ignore cancellation
				} catch (InvocationTargetException e) {
					Throwable t = e.getTargetException();
					if (t instanceof TeamException) {
						throw (TeamException) t;
					}
					// should not get here
				}
				config.setRightLabel(Policy.bind("CVSCatchupReleaseViewer.repositoryFileRevision", new Object[] {name, revision, author[0]})); //$NON-NLS-1$
			} catch (TeamException e) {
				CVSUIPlugin.openError(getControl().getShell(), null, null, e);
				config.setRightLabel(Policy.bind("CVSCatchupReleaseViewer.repositoryFile", name)); //$NON-NLS-1$
			}
		} else {
			config.setRightLabel(Policy.bind("CVSCatchupReleaseViewer.noRepositoryFile")); //$NON-NLS-1$
		}
	
		IRemoteResource base = syncTree.getBase();
		if (base != null) {
			try {
				String revision = ((ICVSRemoteFile)base).getRevision();
				config.setAncestorLabel(Policy.bind("CVSCatchupReleaseViewer.commonFileRevision", new Object[] {name, revision} )); //$NON-NLS-1$
			} catch (TeamException e) {
				CVSUIPlugin.openError(getControl().getShell(), null, null, e);
				config.setRightLabel(Policy.bind("CVSCatchupReleaseViewer.commonFile", name)); //$NON-NLS-1$
			}
		} else {
			config.setAncestorLabel(Policy.bind("CVSCatchupReleaseViewer.noCommonFile")); //$NON-NLS-1$
		}
		
		IResource local = syncTree.getLocal();
		if (local != null) {
			if (!local.exists()) {
				config.setLeftLabel(Policy.bind("CVSCatchupReleaseViewer.No_workspace_file_1")); //$NON-NLS-1$
			} else {
				ICVSFile cvsFile = CVSWorkspaceRoot.getCVSFileFor((IFile)local);
				ResourceSyncInfo info = null;
				try {
					info = cvsFile.getSyncInfo();
					name = local.getName();
					String revision = null;
					if (info != null) {
						revision = info.getRevision();
						if (info.isAdded() || info.isDeleted()) {
							revision = null;
						}
					}
					if (revision != null) {
						config.setLeftLabel(Policy.bind("CVSCatchupReleaseViewer.commonFileRevision", name, revision)); //$NON-NLS-1$
					} else {
						config.setLeftLabel(Policy.bind("CVSCatchupReleaseViewer.commonFile", name)); //$NON-NLS-1$
					}
				} catch (CVSException e) {
					CVSUIPlugin.openError(getControl().getShell(), null, null, e);
					config.setLeftLabel(Policy.bind("CVSCatchupReleaseViewer.commonFile", name)); //$NON-NLS-1$				
				}
			}
		}
	}
}
