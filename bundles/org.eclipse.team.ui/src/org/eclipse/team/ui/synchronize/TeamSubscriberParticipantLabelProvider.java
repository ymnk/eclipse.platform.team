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

import java.util.*;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.internal.core.Assert;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.ui.ISharedImages;
import org.eclipse.team.ui.synchronize.content.SyncInfoLabelProvider;
import org.eclipse.team.ui.synchronize.content.SyncInfoSetContentProvider;
import org.eclipse.ui.internal.WorkbenchColors;

/**
 * Provides basic labels for the subscriber participant synchronize view 
 * page. This class provides a facility for subclasses to define annotations
 * on the labels and icons of adaptable objects by overriding
 * <code>decorateText()</code> and <code>decorateImage</code>.
 * 
 * @see TeamSubscriberParticipantPage#getLabelProvider()
 * @since 3.0
 */
public class TeamSubscriberParticipantLabelProvider extends LabelProvider implements ITableLabelProvider, IColorProvider {
	
	//column constants
	private static final int COL_RESOURCE = 0;
	private static final int COL_PARENT = 1;
	private boolean working = false;
	
	// cache for folder images that have been overlayed with conflict icon
	private Map fgImageCache;
	
	// Keep track of the compare provider and sync info label provider
	// so they can be properly disposed
	CompareConfiguration compareConfig = new CompareConfiguration();
	SyncInfoLabelProvider syncInfoLabelProvider;

	public TeamSubscriberParticipantLabelProvider() {
		this(new SyncInfoLabelProvider());
	}
	
	public TeamSubscriberParticipantLabelProvider(SyncInfoLabelProvider syncInfoLabelProvider) {
		Assert.isNotNull(syncInfoLabelProvider);
		JobStatusHandler.addJobListener(new IJobListener() {
			public void started(QualifiedName jobType) {
				working = true;
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						// TODO: What this is this supposed to be?
						synchronized (this) {
							fireLabelProviderChanged(new LabelProviderChangedEvent(TeamSubscriberParticipantLabelProvider.this));
						}
					}
				});
			}
			public void finished(QualifiedName jobType) {
				working = false;
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						synchronized (this) {
							fireLabelProviderChanged(new LabelProviderChangedEvent(TeamSubscriberParticipantLabelProvider.this));
						}
					}
				});
			}
		}, TeamSubscriber.SUBSCRIBER_JOB_TYPE);
		
		// The label provider may of been created after the subscriber job has been
		// started.
		this.working = JobStatusHandler.hasRunningJobs(TeamSubscriber.SUBSCRIBER_JOB_TYPE);
		this.syncInfoLabelProvider = syncInfoLabelProvider;
	}

	protected String decorateText(String input, Object element) {
		return input;
	}
	
	protected Image decorateImage(Image base, Object element) {
		return base;
	}
	
	public String getText(Object element) {
		String name = syncInfoLabelProvider.getText(element);
		if (TeamUIPlugin.getPlugin().getPreferenceStore().getBoolean(IPreferenceIds.SYNCVIEW_VIEW_SYNCINFO_IN_LABEL)) {
			SyncInfo info = SyncInfoSetContentProvider.getSyncInfo(element);
			if (info != null && info.getKind() != SyncInfo.IN_SYNC) {
				String syncKindString = SyncInfo.kindToString(info.getKind());
				name = Policy.bind("TeamSubscriberSyncPage.labelWithSyncKind", name, syncKindString); //$NON-NLS-1$
			}
		}
		return decorateText(name, element);
	}
	
	/**
	 * An image is decorated by at most 3 different plugins. 
	 * 1. ask the sync info label decorator for the default icon for the resource
	 * 2. ask the compare plugin for the sync kind overlay
	 * 3. overlay the conflicting image on folders/projects containing conflicts 
	 */
	public Image getImage(Object element) {
		Image decoratedImage = null;
		IResource resource = SyncInfoSetContentProvider.getResource(element);
		Image image = syncInfoLabelProvider.getImage(element);
		decoratedImage = getCompareImage(image, element);	
		decoratedImage = propagateConflicts(decoratedImage, element, resource);
		return decorateImage(decoratedImage, element);
	}
	
	private Image getCompareImage(Image base, Object element) {
		int kind = SyncInfoSetContentProvider.getSyncKind(element);
		switch (kind & SyncInfo.DIRECTION_MASK) {
			case SyncInfo.OUTGOING:
				kind = (kind &~ SyncInfo.OUTGOING) | SyncInfo.INCOMING;
				break;
			case SyncInfo.INCOMING:
				kind = (kind &~ SyncInfo.INCOMING) | SyncInfo.OUTGOING;
				break;
		}	
		return compareConfig.getImage(base, kind);
	}
	
	private Image propagateConflicts(Image base, Object element, IResource resource) {
		if(element instanceof SyncInfoDiffNode && resource.getType() != IResource.FILE) {
			// if the folder is already conflicting then don't bother propagating the conflict
			int kind = SyncInfoSetContentProvider.getSyncKind(element);
			if((kind & SyncInfo.DIRECTION_MASK) != SyncInfo.CONFLICTING) {
				if(((SyncInfoDiffNode)element).hasDecendantConflicts()) {
					ImageDescriptor overlay = new OverlayIcon(
	   					base, 
	   					new ImageDescriptor[] { TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_CONFLICT_OVR)}, 
	   					new int[] {OverlayIcon.BOTTOM_LEFT}, 
	   					new Point(base.getBounds().width, base.getBounds().height));
	  
					if(fgImageCache == null) {
	   					fgImageCache = new HashMap(10);
	 				}
	 				Image conflictDecoratedImage = (Image) fgImageCache.get(overlay);
	 				if (conflictDecoratedImage == null) {
	   					conflictDecoratedImage = overlay.createImage();
	   					fgImageCache.put(overlay, conflictDecoratedImage);
				 	}
					return conflictDecoratedImage;
				}
			}
		}
		return base;
	}
	   
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	public void dispose() {
		super.dispose();
		syncInfoLabelProvider.dispose();
		compareConfig.dispose();
		if(fgImageCache != null) {
			Iterator it = fgImageCache.values().iterator();
			while (it.hasNext()) {
				Image element = (Image) it.next();
				element.dispose();				
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
	 */
	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex == COL_RESOURCE) {
			return getImage(element);
		} else if (columnIndex == COL_PARENT) {
			IResource resource = SyncInfoSetContentProvider.getResource(element);
			return null;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
	 */
	public String getColumnText(Object element, int columnIndex) {
		if (columnIndex == COL_RESOURCE) {
			return getText(element);
		} else if (columnIndex == COL_PARENT) {
			IResource resource = SyncInfoSetContentProvider.getResource(element);
			return resource.getParent().getFullPath().toString();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
	 */
	public Color getForeground(Object element) {	
		if (working)  {
			return WorkbenchColors.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
		} else  {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.Object)
	 */
	public Color getBackground(Object element) {
		return null;
	}
}