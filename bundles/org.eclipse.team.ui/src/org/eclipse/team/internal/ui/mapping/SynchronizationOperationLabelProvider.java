/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.*;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.mapping.IResourceMappingScope;
import org.eclipse.team.ui.mapping.ISynchronizationContext;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonLabelProvider;
import org.eclipse.ui.navigator.IExtensionStateModel;

public abstract class SynchronizationOperationLabelProvider implements ICommonLabelProvider {

	// Cache for folder images that have been overlayed with conflict icon
	private Map fgImageCache;
	
	// Contains direction images
	CompareConfiguration compareConfig = new CompareConfiguration();

	private IResourceMappingScope scope;
	private ISynchronizationContext context;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		ILabelProvider modelLabelProvider = getModelLabelProvider();
		Image base = modelLabelProvider.getImage(element);
		if (base != null && getContext() != null) {
			int kind = getSyncKind(element);
			Image decoratedImage;
			decoratedImage = getCompareImage(base, kind);				
			// The reason we still overlay the compare image is to
			// ensure that the image width for all images shown in the viewer
			// are consistent.
			return decoratedImage;				
		}
		return base;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		ILabelProvider modelLabelProvider = getModelLabelProvider();
		String base = modelLabelProvider.getText(element);
		if (isSyncInfoInTextEnabled()) {
			int kind = getSyncKind(element);
			if (kind != SyncInfo.IN_SYNC) {
				String syncKindString = SyncInfo.kindToString(kind);
				return NLS.bind(TeamUIMessages.TeamSubscriberSyncPage_labelWithSyncKind, new String[] { base, syncKindString }); // 
			}
		}
		return base;
	}

	/**
	 * Returns whether the synchronization state should be included in the
	 * text of the label. By default, the Team preference is used to determine
	 * what to return.
	 * @return whether the synchronization state should be included in the
	 * text of the label
	 */
	protected boolean isSyncInfoInTextEnabled() {
		return TeamUIPlugin.getPlugin().getPreferenceStore().getBoolean(IPreferenceIds.SYNCVIEW_VIEW_SYNCINFO_IN_LABEL);
	}
	
	private Image getCompareImage(Image base, int kind) {
		switch (kind & SyncInfo.DIRECTION_MASK) {
			case SyncInfo.OUTGOING :
				kind = (kind & ~SyncInfo.OUTGOING) | SyncInfo.INCOMING;
				break;
			case SyncInfo.INCOMING :
				kind = (kind & ~SyncInfo.INCOMING) | SyncInfo.OUTGOING;
				break;
		}
		return compareConfig.getImage(base, kind);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	public void dispose() {
		compareConfig.dispose();
		if (fgImageCache != null) {
			Iterator it = fgImageCache.values().iterator();
			while (it.hasNext()) {
				Image element = (Image) it.next();
				element.dispose();
			}
		}
	}
	
	private void init(IResourceMappingScope input, ISynchronizationContext context) {
		this.scope = input;
		this.context = context;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.ICommonLabelProvider#init(org.eclipse.ui.navigator.IExtensionStateModel, org.eclipse.jface.viewers.ITreeContentProvider)
	 */
	public void init(IExtensionStateModel aStateModel, ITreeContentProvider aContentProvider) {
		init((IResourceMappingScope)aStateModel.getProperty(TeamUI.RESOURCE_MAPPING_SCOPE), (ISynchronizationContext)aStateModel.getProperty(TeamUI.SYNCHRONIZATION_CONTEXT));	
	}
	
	/**
	 * Return the label provider that will return the text and image 
	 * appropriate for the given model element. Subclasses are responsible for
	 * disposing of the label provider.
	 * @return the label provider that will return the text and image 
	 * appropriate for the given model element
	 */
	protected abstract ILabelProvider getModelLabelProvider();
	
	/**
	 * Return the sync kind of the given element. This is used
	 * to determine how to decorate the image and label of the
	 * element.
	 * @param element the element being tested
	 * @return the sync kind of the given element
	 */
	protected abstract int getSyncKind(Object element);

	/**
	 * Return the synchronization context associated with the view to which
	 * this label provider applies.
	 * @return the synchronization context
	 */
	public ISynchronizationContext getContext() {
		return context;
	}

	/**
	 * Return the resource mapping scope associated with the view to which
	 * this label provider applies.
	 * @return the esource mapping scope
	 */
	public IResourceMappingScope getScope() {
		return scope;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	public void addListener(ILabelProviderListener listener) {
		getModelLabelProvider().addListener(listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
	 */
	public boolean isLabelProperty(Object element, String property) {
		return getModelLabelProvider().isLabelProperty(element, property);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	public void removeListener(ILabelProviderListener listener) {
		getModelLabelProvider().removeListener(listener);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.IMementoAware#restoreState(org.eclipse.ui.IMemento)
	 */
	public void restoreState(IMemento aMemento) {
		// Do nothing by default
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.IMementoAware#saveState(org.eclipse.ui.IMemento)
	 */
	public void saveState(IMemento aMemento) {
		// Do nothing by default
	}

}
