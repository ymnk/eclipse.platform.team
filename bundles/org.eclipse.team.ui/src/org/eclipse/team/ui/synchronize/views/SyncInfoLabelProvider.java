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
package org.eclipse.team.ui.synchronize.views;

import java.util.*;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.ui.ISharedImages;
import org.eclipse.team.ui.synchronize.SyncInfoDiffNode;
import org.eclipse.ui.internal.WorkbenchColors;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * @since 3.0
 */
public class SyncInfoLabelProvider extends LabelProvider implements IColorProvider {
	
	private boolean working = false;
	// cache for folder images that have been overlayed with conflict icon
	private Map fgImageCache;	
	CompareConfiguration compareConfig = new CompareConfiguration();
	private WorkbenchLabelProvider workbenchLabelProvider = new WorkbenchLabelProvider();
	
	/**
	 * Decorating label provider that also support color providers
	 */
	public static class DecoratingColorLabelProvider extends DecoratingLabelProvider implements IColorProvider {

		public DecoratingColorLabelProvider(ILabelProvider provider, ILabelDecorator decorator) {
			super(provider, decorator);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
		 */
		public Color getForeground(Object element) {
			ILabelProvider p = getLabelProvider();
			if(p instanceof IColorProvider) {
				return ((IColorProvider)p).getForeground(element);
			}
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.Object)
		 */
		public Color getBackground(Object element) {
			ILabelProvider p = getLabelProvider();
			if(p instanceof IColorProvider) {
				return ((IColorProvider)p).getBackground(element);
			}
			return null;
		}
	}

	
	public SyncInfoLabelProvider() {
		JobStatusHandler.addJobListener(new IJobListener() {
			public void started(QualifiedName jobType) {
				working = true;
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						// TODO: What this is this supposed to be?
						synchronized (this) {
							fireLabelProviderChanged(new LabelProviderChangedEvent(SyncInfoLabelProvider.this));
						}
					}
				});
			}
			public void finished(QualifiedName jobType) {
				working = false;
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						synchronized (this) {
							fireLabelProviderChanged(new LabelProviderChangedEvent(SyncInfoLabelProvider.this));
						}
					}
				});
			}
		}, TeamSubscriber.SUBSCRIBER_JOB_TYPE);
		
		// The label provider may of been created after the subscriber job has been
		// started.
		this.working = JobStatusHandler.hasRunningJobs(TeamSubscriber.SUBSCRIBER_JOB_TYPE);
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		Image base = workbenchLabelProvider.getImage(element);
		if(base != null) {
			SyncInfo info = getSyncInfo(element);
			if(info == null) {
				return getCompareImage(base, SyncInfo.IN_SYNC);
			}
			Image decoratedImage = getCompareImage(base, info.getKind());
			return propagateConflicts(decoratedImage, element, info.getLocal()); 
		}
		return base;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		String base = workbenchLabelProvider.getText(element);
		if (TeamUIPlugin.getPlugin().getPreferenceStore().getBoolean(IPreferenceIds.SYNCVIEW_VIEW_SYNCINFO_IN_LABEL)) {
			SyncInfo info = getSyncInfo(element);
			if (info != null && info.getKind() != SyncInfo.IN_SYNC) {
				String syncKindString = SyncInfo.kindToString(info.getKind());
				return Policy.bind("TeamSubscriberSyncPage.labelWithSyncKind", base, syncKindString); //$NON-NLS-1$ 
			}
		}
		return base;
	}
	
	protected Image getCompareImage(Image base, int kind) {
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
			int kind = getSyncKind(element);
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
	
	private int getSyncKind(Object element) {
		SyncInfo info = getSyncInfo(element);
		if (info != null) {
			return info.getKind();
		}
		return SyncInfo.IN_SYNC;
	}
	   
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	public void dispose() {
		compareConfig.dispose();
		if(fgImageCache != null) {
			Iterator it = fgImageCache.values().iterator();
			while (it.hasNext()) {
				Image element = (Image) it.next();
				element.dispose();				
			}
		}
	}
	
	/**
	 * Returns the implementation of SyncInfo for the given
	 * object.  Returns <code>null</code> if the adapter is not defined or the
	 * object is not adaptable.
	 */
	protected final SyncInfo getSyncInfo(Object o) {
		if (!(o instanceof IAdaptable)) {
			return null;
		}
		return (SyncInfo) ((IAdaptable) o).getAdapter(SyncInfo.class);
	}
}